package org.example.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Thymeleaf configuration for email templates.
 * Configures a dedicated template engine for email templates with proper encoding and caching.
 * This is separate from the main web template engine to avoid conflicts.
 */
@Configuration
public class ThymeleafEmailConfig {

    /**
     * Template resolver for email templates.
     * Uses classpath resource location with HTML template mode and UTF-8 encoding.
     */
    @Bean("emailTemplateResolver")
    public SpringResourceTemplateResolver emailTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/email/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        resolver.setOrder(0);  // First priority for email templates
        resolver.setCheckExistence(true);  // Check file exists before loading
        return resolver;
    }

    /**
     * Template engine for email templates.
     * Configured with the email template resolver.
     * This is a separate, dedicated engine for email templates only.
     */
    @Bean("emailTemplateEngine")
    public SpringTemplateEngine emailTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        SpringResourceTemplateResolver emailTemplateResolver = emailTemplateResolver();
        engine.addTemplateResolver(emailTemplateResolver);
        return engine;
    }
}


