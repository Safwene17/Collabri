package org.example.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration for Spring Cloud Gateway MVC
 * Handles Cross-Origin Resource Sharing for all gateway routes
 */
@Configuration
public class Webconfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Allow requests from frontend origins
                .allowedOrigins(
                    "http://localhost:5000"  // Vue.js dev server
                )
                // Allow all standard HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // Allow all headers (adjust as needed for security)
                .allowedHeaders("*")
                // Allow credentials (cookies, authorization headers)
                .allowCredentials(true)
                // Expose response headers to frontend
                .exposedHeaders("Authorization", "Content-Type")
                // Cache preflight response for 1 hour
                .maxAge(3600);
    }
}