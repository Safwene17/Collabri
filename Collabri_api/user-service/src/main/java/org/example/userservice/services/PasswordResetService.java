package org.example.userservice.services;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.ResetPasswordRequest;
import org.example.userservice.entities.PasswordResetToken;
import org.example.userservice.entities.User;
//import org.example.userservice.exceptions.InvalidEmailException;
import org.example.userservice.exceptions.CustomException;
import org.example.userservice.repositories.PasswordResetTokenRepository;
import org.example.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.reset-token-ttl-minutes}")
    private long tokenTtlMinutes;

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

        // delete any previous tokens for this user (optional)
        // tokenRepository.deleteAllByUser(user);
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(tokenTtlMinutes));

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setTokenHash(hashToken(token));
        prt.setExpiresAt(expiresAt);
        tokenRepository.save(prt);
        sendResetEmail(user.getEmail(), token, user.getFirstname());
    }

    private void sendResetEmail(String toEmail, String token, String firstName) {
        userRepository.findByEmail(toEmail)
                .orElseThrow(() -> new CustomException("Email not found", HttpStatus.NOT_FOUND));

        String resetLink = frontendResetUrl + "?token=" + token;
        String subject = "Password Reset Instructions";

        String text = """
                Hi %s,
                
                We received a request to reset the password for your account.
                
                Click the link below to create a new password:
                %s
                
                ⚠️ For security reasons, this link will expire in %d minutes.
                
                If you did not request a password reset, please ignore this message or contact support if you have concerns.
                
                Best regards,
                %s
                """.formatted(
                firstName != null && !firstName.isBlank() ? firstName : "there",
                resetLink,
                tokenTtlMinutes,
                "Collabri Team"
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);

        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }

        mailSender.send(message);
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
