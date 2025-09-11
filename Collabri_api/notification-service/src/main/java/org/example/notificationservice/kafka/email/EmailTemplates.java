package org.example.notificationservice.kafka.email;

import lombok.Getter;

public enum EmailTemplates {
    CALENDAR_INVITE("calendar-invite.html", "Calendar Invitation");


    @Getter
    private final String template;

    @Getter
    private final String subject;

    EmailTemplates(String template, String subject) {
        this.template = template;
        this.subject = subject;
    }
}
