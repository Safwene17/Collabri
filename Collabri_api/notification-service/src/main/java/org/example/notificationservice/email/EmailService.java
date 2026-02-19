package org.example.notificationservice.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_RELATED;

@RequiredArgsConstructor
@Slf4j
@Service
public class EmailService {

    @Value("${app.frontend.invite-base}")
    private String frontendInviteBase;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${app.email.send-retry-count}")
    private int maxSendAttempts;

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    public void sendCalendarInvitationEmail(
            UUID calendarId,
            String calendarName,
            String destinationEmail,
            String inviterEmail,
            String token,          // plaintext token — only on internal topic
            Instant expiresAt
    ) throws MessagingException {

        if (destinationEmail == null || destinationEmail.isBlank()) {
            log.warn("Skipping email send: destinationEmail is null/blank for invite to calendar {}", calendarId);
            return;
        }

        String acceptLink = frontendInviteBase + "/accept?token=" + token;
        String declineLink = frontendInviteBase + "/decline?token=" + token;

        Map<String, Object> variables = new HashMap<>();
        variables.put("calendarName", calendarName);
        variables.put("calendarId", calendarId);
        variables.put("inviterEmail", inviterEmail);
        variables.put("expiresAt", expiresAt);
        variables.put("token", token);
        variables.put("acceptLink", acceptLink);
        variables.put("declineLink", declineLink);

        Context context = new Context();
        context.setVariables(variables);

        String htmlBody = templateEngine.process(EmailTemplates.CALENDAR_INVITE.getTemplate(), context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MULTIPART_MODE_RELATED, UTF_8.name());
        helper.setFrom(mailFrom);
        helper.setSubject(EmailTemplates.CALENDAR_INVITE.getSubject());
        helper.setText(htmlBody, true);
        helper.setTo(destinationEmail);

        // simple retry loop for transient SMTP errors
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                mailSender.send(mimeMessage);
                log.info("Invite email sent to {} for calendar {} (attempt {})", destinationEmail, calendarId, attempt);
                break;
            } catch (Exception ex) {
                log.warn("Attempt {}: failed to send invite email to {}: {}", attempt, destinationEmail, ex.getMessage());
                if (attempt >= Math.max(1, maxSendAttempts)) {
                    log.error("Giving up sending invite email to {} after {} attempts", destinationEmail, attempt, ex);
                    // optional: publish to a dead-letter topic here
                    throw new MessagingException("Failed to send invite email after " + attempt + " attempts", ex);
                }
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
