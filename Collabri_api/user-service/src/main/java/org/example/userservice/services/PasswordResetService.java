package org.example.userservice.services;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.ForgotPasswordRequest;
import org.example.userservice.dto.ResetPasswordRequest;
import org.example.userservice.entities.PasswordResetToken;
import org.example.userservice.entities.User;
import org.example.userservice.repositories.PasswordResetTokenRepository;
import org.example.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
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

    @Transactional
    public void createAndSendResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // delete any previous tokens for this user (optional)
        tokenRepository.deleteAllByUser(user);

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(tokenTtlMinutes));

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken(token);
        prt.setExpiresAt(expiresAt);
        tokenRepository.save(prt);

        sendResetEmail(user.getEmail(), token, user.getFirstname());
    }

    private void sendResetEmail(String toEmail, String token, String firstName) {
        String resetLink = frontendResetUrl + "?token=" + token;

        String subject = "Reset your password";
        String text = "Hi " + (firstName != null ? firstName : "") + ",\n\n"
                + "We received a request to reset your password. Click the link below to set a new password. "
                + "This link will expire in " + tokenTtlMinutes + " minutes.\n\n"
                + resetLink + "\n\n"
                + "If you did not request a password reset, please ignore this email.\n\n"
                + "Thanks,\nYour App Team";

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
        String token = request.token();
        String newPassword = request.newPassword();

        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (prt.isUsed()) {
            throw new IllegalArgumentException("Token already used");
        }

        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");
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
        Optional<PasswordResetToken> opt = tokenRepository.findByToken(token);
        if (opt.isEmpty()) return false;
        PasswordResetToken prt = opt.get();
        if (prt.isUsed()) return false;
        if (prt.getExpiresAt().isBefore(Instant.now())) return false;
        return true;
    }
}
