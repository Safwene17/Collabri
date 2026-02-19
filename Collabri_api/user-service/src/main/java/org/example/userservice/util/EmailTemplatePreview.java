package org.example.userservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Email template preview utility for testing and debugging.
 * Allows rendering email templates with context data for inspection.
 * <p>
 * Usage:
 * <pre>
 * String html = emailTemplatePreview.renderVerificationEmail("John", "https://...", 24, "Collabri", "support@...");
 * System.out.println(html);  // inspect rendered HTML
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTemplatePreview {

    private final SpringTemplateEngine emailTemplateEngine;

    /**
     * Preview verification email template with given context.
     *
     * @param name         User's first name
     * @param actionLink   Full verification link
     * @param ttlHours     Token validity in hours
     * @param appName      Application name
     * @param supportEmail Support contact email
     * @return Rendered HTML string
     */
    public String renderVerificationEmail(String name, String actionLink, long ttlHours,
                                          String appName, String supportEmail) {
        Context context = new Context();
        context.setVariable("name", name != null && !name.isBlank() ? name : "there");
        context.setVariable("actionLink", actionLink);
        context.setVariable("ttl", ttlHours);
        context.setVariable("ttlUnit", ttlHours == 1 ? "hour" : "hours");
        context.setVariable("appName", appName);
        context.setVariable("supportEmail", supportEmail);

        return emailTemplateEngine.process("verify-email", context);
    }

    /**
     * Preview password reset email template with given context.
     *
     * @param name         User's first name
     * @param actionLink   Full reset password link
     * @param ttlMinutes   Token validity in minutes
     * @param appName      Application name
     * @param supportEmail Support contact email
     * @return Rendered HTML string
     */
    public String renderResetPasswordEmail(String name, String actionLink, long ttlMinutes,
                                           String appName, String supportEmail) {
        Context context = new Context();
        context.setVariable("name", name != null && !name.isBlank() ? name : "there");
        context.setVariable("actionLink", actionLink);
        context.setVariable("ttl", ttlMinutes);
        context.setVariable("ttlUnit", ttlMinutes == 1 ? "minute" : "minutes");
        context.setVariable("appName", appName);
        context.setVariable("supportEmail", supportEmail);

        return emailTemplateEngine.process("reset-password", context);
    }

    /**
     * Generic template renderer - use for custom or new templates.
     *
     * @param templateName Template name (without .html extension)
     * @param variables    Map of variable names to values
     * @return Rendered HTML string
     */
    public String renderTemplate(String templateName, java.util.Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        return emailTemplateEngine.process(templateName, context);
    }
}

