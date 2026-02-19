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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
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
    @Qualifier("emailTemplateEngine")
    private final SpringTemplateEngine templateEngine;

    @Value("${app.verify-token-ttl-hours}")
    private long tokenTtlHours;

    @Value("${app.name:Collabri}")
    private String appName;

    @Value("${app.support-email:support@collabri.com}")
    private String supportEmail;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${app.frontend.verify-url:http://localhost:5173/verify-email}")
    private String frontendVerifyUrl;

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
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        // If user is already enabled, nothing to do
        if (user.isVerified()) throw new CustomException("User already verified", HttpStatus.BAD_REQUEST);

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

        sendVerificationEmailWithThymeleaf(user.getEmail(), rawToken, user.getFirstname());
    }

    /**
     * Sends verification email using Thymeleaf template engine.
     * Properly escapes all variables and handles missing name gracefully.
     */
    private void sendVerificationEmailWithThymeleaf(String toEmail, String rawToken, String firstName) {
        try {
            String verifyLink = frontendVerifyUrl + "?token=" + rawToken;

            // Prepare template context with all variables
            Context context = new Context();
            context.setVariable("name", firstName != null && !firstName.isBlank() ? firstName : "there");
            context.setVariable("actionLink", verifyLink);
            context.setVariable("ttl", tokenTtlHours);
            context.setVariable("ttlUnit", tokenTtlHours == 1 ? "hour" : "hours");
            context.setVariable("appName", appName);
            context.setVariable("supportEmail", supportEmail);

            // Process template
            String htmlContent = templateEngine.process("verify-email", context);

            // Create and send MIME message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Verify Your " + appName + " Email Address");
            helper.setFrom(mailFrom);
            helper.setText(htmlContent, true); // true = HTML content

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new CustomException("Failed to send verification email. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Transactional
    public void confirmToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new CustomException("Invalid token", HttpStatus.BAD_REQUEST);

        String tokenHash = hashToken(rawToken);
        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException("Invalid token", HttpStatus.BAD_REQUEST));

        if (token.isUsed()) {
            throw new CustomException("Token already used", HttpStatus.BAD_REQUEST);
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new CustomException("Token expired", HttpStatus.BAD_REQUEST);
        }

        User user = token.getUser();
        if (user.isVerified()) {
            throw new CustomException("User already verified", HttpStatus.BAD_REQUEST);
        }

        user.setVerified(true);
        userRepository.save(user);

        // mark used (optional) and then remove tokens for hygiene
        token.setUsed(true);
        tokenRepository.save(token);
        tokenRepository.deleteAllByUser(user); // delete used and old tokens
    }


    /**
     * Optional: used by controller to resend a verification email (silently swallow user-not-found)
     */
    /**
     * Resends verification email. Handles already-verified users gracefully.
     */
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        try {
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

            if (user.isVerified()) {
                throw new CustomException("User already verified", HttpStatus.BAD_REQUEST);
            }

            // Remove previous tokens for this user
            tokenRepository.deleteAllByUser(user);

            String rawToken = UUID.randomUUID().toString();
            String tokenHash = hashToken(rawToken);
            Instant expiresAt = Instant.now().plus(Duration.ofHours(tokenTtlHours));

            EmailVerificationToken token = new EmailVerificationToken();
            token.setUser(user);
            token.setTokenHash(tokenHash);
            token.setExpiresAt(expiresAt);
            tokenRepository.save(token);

            sendVerificationEmailWithThymeleaf(user.getEmail(), rawToken, user.getFirstname());

        } catch (CustomException e) {
            log.info("Resend verification requested for non-existing email: {}", request.email());
            // Silently swallow - don't reveal if email exists
        }
    }


    /**
     * Optional helper to check if token is valid (can be used by frontend to validate before showing final step)
     */
    public boolean validateToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        Optional<EmailVerificationToken> opt = tokenRepository.findByTokenHash(tokenHash);
        if (opt.isEmpty()) throw new CustomException("Invalid token", HttpStatus.BAD_REQUEST);
        var t = opt.get();
        if (t.isUsed()) throw new CustomException("Token already used", HttpStatus.BAD_REQUEST);
        if (t.getExpiresAt().isBefore(Instant.now()))
            throw new CustomException("Token expired", HttpStatus.BAD_REQUEST);
        return true;
    }
}
