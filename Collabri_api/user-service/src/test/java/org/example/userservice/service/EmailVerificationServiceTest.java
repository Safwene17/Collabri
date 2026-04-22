package org.example.userservice.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.example.userservice.dto.ResendVerificationRequest;
import org.example.userservice.entities.EmailVerificationToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.repositories.EmailVerificationTokenRepository;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.EmailVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService Unit Tests")
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User buildUser(String email, boolean verified) {
        return User.builder()
                .id(UUID.randomUUID())
                .firstname("John")
                .lastname("Doe")
                .email(email)
                .password("encoded")
                .verified(verified)
                .build();
    }

    private void setFields() {
        ReflectionTestUtils.setField(emailVerificationService, "tokenTtlHours", 24L);
        ReflectionTestUtils.setField(emailVerificationService, "appName", "Collabri");
        ReflectionTestUtils.setField(emailVerificationService, "supportEmail", "support@collabri.com");
        ReflectionTestUtils.setField(emailVerificationService, "mailFrom", "noreply@collabri.com");
        ReflectionTestUtils.setField(emailVerificationService, "frontendVerifyUrl", "https://frontend/verify-email");
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Protects token issuance and resend hygiene for unverified accounts.
    @Nested
    @DisplayName("createAndSendVerificationToken()")
    class CreateAndSendVerificationToken {

        @Test
        @DisplayName("should cleanup old tokens, persist new token, and send verification email")
        void shouldCreateAndSendToken() {
            setFields();
            User user = buildUser("verify@example.com", false);
            MimeMessage message = new MimeMessage((Session) null);

            when(userRepository.findByEmail("verify@example.com")).thenReturn(Optional.of(user));
            when(templateEngine.process(org.mockito.ArgumentMatchers.eq("verify-email"), org.mockito.ArgumentMatchers.any()))
                    .thenReturn("<html>ok</html>");
            when(mailSender.createMimeMessage()).thenReturn(message);

            emailVerificationService.createAndSendVerificationToken("verify@example.com");

            verify(tokenRepository).deleteAllByUser(user);
            ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).hasSize(64);
            assertThat(captor.getValue().isUsed()).isFalse();
            verify(mailSender).send(message);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when user is already verified")
        void shouldThrowWhenAlreadyVerified() {
            User user = buildUser("already@example.com", true);
            when(userRepository.findByEmail("already@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> emailVerificationService.createAndSendVerificationToken("already@example.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("User already verified")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verifyNoInteractions(tokenRepository, mailSender);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when user does not exist")
        void shouldThrowNotFoundWhenUserMissing() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailVerificationService.createAndSendVerificationToken("missing@example.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("User not found")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verifyNoInteractions(tokenRepository, mailSender);
        }

        @Test
        @DisplayName("should throw INTERNAL_SERVER_ERROR when email dispatch fails")
        void shouldThrowWhenEmailDispatchFails() {
            setFields();
            User user = buildUser("verify@example.com", false);

            when(userRepository.findByEmail("verify@example.com")).thenReturn(Optional.of(user));
            when(templateEngine.process(org.mockito.ArgumentMatchers.eq("verify-email"), org.mockito.ArgumentMatchers.any()))
                    .thenReturn("<html>ok</html>");
            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("smtp down"));

            assertThatThrownBy(() -> emailVerificationService.createAndSendVerificationToken("verify@example.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Failed to send verification email. Please try again later.")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Protects verification confirmation branches and state transitions.
    @Nested
    @DisplayName("confirmToken()")
    class ConfirmToken {

        @Test
        @DisplayName("should verify user, mark token used, and cleanup all user tokens")
        void shouldConfirmSuccessfully() {
            String raw = "confirm-token";
            String hash = sha256(raw);
            User user = buildUser("confirm@example.com", false);
            EmailVerificationToken token = new EmailVerificationToken();
            token.setUser(user);
            token.setTokenHash(hash);
            token.setUsed(false);
            token.setExpiresAt(Instant.now().plusSeconds(60));

            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            emailVerificationService.confirmToken(raw);

            assertThat(user.isVerified()).isTrue();
            assertThat(token.isUsed()).isTrue();
            var order = inOrder(userRepository, tokenRepository);
            order.verify(userRepository).save(user);
            order.verify(tokenRepository).save(token);
            order.verify(tokenRepository).deleteAllByUser(user);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when token is blank")
        void shouldThrowWhenTokenBlank() {
            assertThatThrownBy(() -> emailVerificationService.confirmToken("  "))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Invalid token")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verifyNoInteractions(tokenRepository);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when token is already used")
        void shouldThrowWhenTokenUsed() {
            String raw = "used-token";
            String hash = sha256(raw);
            EmailVerificationToken token = new EmailVerificationToken();
            token.setTokenHash(hash);
            token.setUsed(true);
            token.setExpiresAt(Instant.now().plusSeconds(60));

            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> emailVerificationService.confirmToken(raw))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Token already used")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when token is expired")
        void shouldThrowWhenTokenExpired() {
            String raw = "expired-token";
            String hash = sha256(raw);
            EmailVerificationToken token = new EmailVerificationToken();
            token.setTokenHash(hash);
            token.setUsed(false);
            token.setExpiresAt(Instant.now().minusSeconds(1));

            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> emailVerificationService.confirmToken(raw))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Token expired")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // Protects resend endpoint from account enumeration leaks.
    @Nested
    @DisplayName("resendVerification()")
    class ResendVerification {

        @Test
        @DisplayName("should silently swallow user-not-found and not throw")
        void shouldSwallowUserNotFound() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            emailVerificationService.resendVerification(new ResendVerificationRequest("missing@example.com"));

            verifyNoInteractions(tokenRepository, mailSender);
        }
    }

    // Protects frontend helper validation behavior.
    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("should return true when token is valid")
        void shouldReturnTrueWhenValid() {
            String raw = "valid-token";
            String hash = sha256(raw);
            EmailVerificationToken token = new EmailVerificationToken();
            token.setUsed(false);
            token.setExpiresAt(Instant.now().plusSeconds(60));
            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            assertThat(emailVerificationService.validateToken(raw)).isTrue();
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when token is already used")
        void shouldThrowWhenUsed() {
            String raw = "used-token";
            String hash = sha256(raw);
            EmailVerificationToken token = new EmailVerificationToken();
            token.setUsed(true);
            token.setExpiresAt(Instant.now().plusSeconds(60));
            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> emailVerificationService.validateToken(raw))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Token already used")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when token does not exist")
        void shouldThrowWhenTokenMissing() {
            String raw = "missing-token";
            String hash = sha256(raw);
            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailVerificationService.validateToken(raw))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Invalid token")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when token is expired")
        void shouldThrowWhenExpired() {
            String raw = "expired-token";
            String hash = sha256(raw);
            EmailVerificationToken token = new EmailVerificationToken();
            token.setUsed(false);
            token.setExpiresAt(Instant.now().minusSeconds(1));
            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> emailVerificationService.validateToken(raw))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Token expired")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}

