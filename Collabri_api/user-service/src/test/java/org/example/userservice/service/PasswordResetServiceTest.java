package org.example.userservice.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.example.userservice.dto.ResetPasswordRequest;
import org.example.userservice.entities.PasswordResetToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.repositories.PasswordResetTokenRepository;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.services.PasswordResetService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Unit Tests")
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User buildUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .firstname("John")
                .lastname("Doe")
                .email(email)
                .password("old-encoded")
                .build();
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setFields() {
        ReflectionTestUtils.setField(passwordResetService, "tokenTtlMinutes", 15L);
        ReflectionTestUtils.setField(passwordResetService, "appName", "Collabri");
        ReflectionTestUtils.setField(passwordResetService, "supportEmail", "support@collabri.com");
        ReflectionTestUtils.setField(passwordResetService, "frontendResetUrl", "https://frontend/reset");
        ReflectionTestUtils.setField(passwordResetService, "mailFrom", "noreply@collabri.com");
    }

    // Protects reset-token issuance and mail dispatch behavior.
    @Nested
    @DisplayName("createAndSendResetToken()")
    class CreateAndSendResetToken {

        @Test
        @DisplayName("should create token hash, persist it, and send mail")
        void shouldCreateTokenAndSendMail() {
            setFields();
            User user = buildUser("reset@example.com");
            MimeMessage mimeMessage = new MimeMessage((Session) null);

            when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
            when(templateEngine.process(org.mockito.ArgumentMatchers.eq("reset-password"), org.mockito.ArgumentMatchers.any()))
                    .thenReturn("<html>ok</html>");
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            passwordResetService.createAndSendResetToken("reset@example.com");

            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            PasswordResetToken saved = tokenCaptor.getValue();

            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getTokenHash()).hasSize(64);
            assertThat(saved.isUsed()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(Instant.now().minusSeconds(1));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when user does not exist")
        void shouldThrowWhenUserMissing() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.createAndSendResetToken("missing@example.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("User not found")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verifyNoInteractions(tokenRepository, mailSender);
        }
    }

    // Protects reset-password branch rules (invalid, used, expired, valid).
    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        @Test
        @DisplayName("should update password, mark token used, then cleanup user tokens")
        void shouldResetPasswordSuccessfully() {
            User user = buildUser("ok@example.com");
            String rawToken = "raw-token";
            String hash = sha256(rawToken);
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hash);
            token.setUsed(false);
            token.setExpiresAt(Instant.now().plusSeconds(60));

            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("StrongPass1!")).thenReturn("new-encoded");

            passwordResetService.resetPassword(new ResetPasswordRequest(rawToken, "StrongPass1!"));

            assertThat(user.getPassword()).isEqualTo("new-encoded");
            assertThat(token.isUsed()).isTrue();

            var order = inOrder(userRepository, tokenRepository);
            order.verify(userRepository).save(user);
            order.verify(tokenRepository).save(token);
            order.verify(tokenRepository).deleteAllByUser(user);
        }

        @Test
        @DisplayName("should throw when token already used")
        void shouldThrowWhenUsed() {
            String rawToken = "used-token";
            String hash = sha256(rawToken);
            PasswordResetToken token = new PasswordResetToken();
            token.setUsed(true);
            token.setExpiresAt(Instant.now().plusSeconds(60));

            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetService.resetPassword(new ResetPasswordRequest(rawToken, "StrongPass1!")))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Token already used");
        }
    }

    // Protects frontend token pre-validation helper.
    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("should return true when token is valid")
        void shouldReturnTrueWhenValid() {
            String raw = "valid-token";
            String hash = sha256(raw);
            PasswordResetToken token = new PasswordResetToken();
            token.setUsed(false);
            token.setExpiresAt(Instant.now().plusSeconds(60));

            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            assertThat(passwordResetService.validateToken(raw)).isTrue();
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when token is expired")
        void shouldThrowWhenExpired() {
            String raw = "expired-token";
            String hash = sha256(raw);
            PasswordResetToken token = new PasswordResetToken();
            token.setUsed(false);
            token.setExpiresAt(Instant.now().minusSeconds(1));
            when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> passwordResetService.validateToken(raw))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("Token expired")
                    .extracting(ex -> ((CustomException) ex).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}

