package org.example.userservice.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.ResetPasswordRequest;
import org.example.userservice.entities.PasswordResetToken;
import org.example.userservice.entities.User;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.repositories.PasswordResetTokenRepository;
import org.example.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    @Qualifier("emailTemplateEngine")
    private final SpringTemplateEngine templateEngine;

    @Value("${app.reset-token-ttl-minutes}")
    private long tokenTtlMinutes;

    @Value("${app.name:Collabri}")
    private String appName;

    @Value("${app.support-email:support@collabri.com}")
    private String supportEmail;

    @Value("${app.frontend.reset-url}")
    private String frontendResetUrl;

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
    public void createAndSendResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(tokenTtlMinutes));

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setTokenHash(hashToken(token));
        prt.setExpiresAt(expiresAt);
        tokenRepository.save(prt);

        sendResetEmailWithThymeleaf(user.getEmail(), token, user.getFirstname());
    }

    /**
     * Sends password reset email using Thymeleaf template engine.
     * Properly escapes all variables and handles missing name gracefully.
     */
    private void sendResetEmailWithThymeleaf(String toEmail, String rawToken, String firstName) {
        try {
            String resetLink = frontendResetUrl + "?token=" + rawToken;

            // Prepare template context with all variables
            Context context = new Context();
            context.setVariable("name", firstName != null && !firstName.isBlank() ? firstName : "there");
            context.setVariable("actionLink", resetLink);
            context.setVariable("ttl", tokenTtlMinutes);
            context.setVariable("ttlUnit", tokenTtlMinutes == 1 ? "minute" : "minutes");
            context.setVariable("appName", appName);
            context.setVariable("supportEmail", supportEmail);

            // Process template
            String htmlContent = templateEngine.process("reset-password", context);

            // Create and send MIME message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Reset Your " + appName + " Password");
            helper.setFrom(mailFrom);
            helper.setText(htmlContent, true); // true = HTML content

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new CustomException("Failed to send password reset email. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.token());
        String newPassword = request.newPassword();

        PasswordResetToken prt = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException("Invalid token", HttpStatus.BAD_REQUEST));

        if (prt.isUsed()) {
            throw new CustomException("Token already used", HttpStatus.BAD_REQUEST);
        }

        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new CustomException("Token expired", HttpStatus.BAD_REQUEST);
        }

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // mark token used and optionally delete all tokens for user
        prt.setUsed(true);
        tokenRepository.save(prt);
        tokenRepository.deleteAllByUser(user); // cleanup
    }

    /**
     * Optional utility method for frontend validation.
     */
    public boolean validateToken(String token) {
        String hashToken = hashToken(token);
        Optional<PasswordResetToken> opt = tokenRepository.findByTokenHash(hashToken);
        if (opt.isEmpty()) throw new CustomException("Invalid token", HttpStatus.BAD_REQUEST);
        PasswordResetToken prt = opt.get();
        if (prt.isUsed()) throw new CustomException("Token already used", HttpStatus.BAD_REQUEST);
        if (prt.getExpiresAt().isBefore(Instant.now()))
            throw new CustomException("Token expired", HttpStatus.BAD_REQUEST);
        return true;
    }
}
