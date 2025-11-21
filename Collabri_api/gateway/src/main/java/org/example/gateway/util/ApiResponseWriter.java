package org.example.gateway.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.gateway.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Writes an ApiResponse object as JSON to the HttpServletResponse.
 */
@Component
@RequiredArgsConstructor
public class ApiResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int status, ApiResponse<?> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(objectMapper.writeValueAsBytes(body));
    }
}
