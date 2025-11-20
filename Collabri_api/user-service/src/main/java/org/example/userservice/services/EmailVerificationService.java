package org.example.userservice.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.ResendVerificationRequest;
import org.example.userservice.entities.EmailVerificationToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.repositories.EmailVerificationTokenRepository;
import org.example.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.verify-token-ttl-hours}")
    private long tokenTtlHours;


    @Value("${spring.mail.username}")
    private String mailFrom;

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Unable to hash token", e);
        }
    }

    @Transactional
    public void createAndSendVerificationToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // If user is already enabled, nothing to do
        if (user.isVerified()) return;

        // remove previous tokens for this user
        tokenRepository.deleteAllByUser(user);

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(Duration.ofHours(tokenTtlHours));

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        tokenRepository.save(token);

        sendVerificationEmailHtml(user.getEmail(), rawToken, user.getFirstname());
    }

    private void sendVerificationEmailHtml(String toEmail, String rawToken, String firstName) {
        // link now points to backend endpoint that will verify + redirect
        // String verifyLink = backendVerifyUrl + "?token=" + rawToken;
        String verifyLink = "http://localhost:5173/verify-email" + "?token=" + rawToken;
        try {
            var resource = new ClassPathResource("templates/verify_email.html");
            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{name}}", firstName != null ? firstName : "")
                    .replace("{{verifyLink}}", verifyLink)
                    .replace("{{ttl}}", String.valueOf(tokenTtlHours));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Verify your email");
            if (mailFrom != null && !mailFrom.isBlank()) helper.setFrom(mailFrom);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }


    @Transactional
    public void confirmToken(String rawToken) {
        log.info("Confirming verification token");

        String tokenHash = hashToken(rawToken);
        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException("Invalid token", HttpStatus.BAD_REQUEST));

        if (token.isUsed()) {
            throw new CustomException("Token already used", HttpStatus.BAD_REQUEST);
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new CustomException("Token expired", HttpStatus.BAD_REQUEST);
        }

        if (token.getUser().isVerified()) {
            throw new CustomException("User already verified", HttpStatus.OK);
        }

        User user = token.getUser();
        user.setVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);


    }


    /**
     * Optional: used by controller to resend a verification email (silently swallow user-not-found)
     */
    public void resendVerification(ResendVerificationRequest request) {
        try {
            createAndSendVerificationToken(request.email());
        } catch (IllegalArgumentException ignored) {
            log.info("Resend verification requested for non-existing email: " + request.email());
        }
    }

    /**
     * Optional helper to check if token is valid (can be used by frontend to validate before showing final step)
     */
    public boolean validateToken(String rawToken) {
        try {
            String tokenHash = hashToken(rawToken);
            Optional<EmailVerificationToken> opt = tokenRepository.findByTokenHash(tokenHash);
            if (opt.isEmpty()) throw new CustomException("Invalid token", HttpStatus.BAD_REQUEST);
            var t = opt.get();
            if (t.isUsed()) throw new CustomException("Token already used", HttpStatus.BAD_REQUEST);
            if (t.getExpiresAt().isBefore(Instant.now()))
                throw new CustomException("Token expired", HttpStatus.BAD_REQUEST);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
