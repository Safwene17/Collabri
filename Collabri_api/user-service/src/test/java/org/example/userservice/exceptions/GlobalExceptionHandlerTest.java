package org.example.userservice.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.example.userservice.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // Protects API-level exception-to-status translation rules.
    @Nested
    @DisplayName("Exception mappings")
    class ExceptionMappings {

        @Test
        @DisplayName("should map CustomException 4xx as-is")
        void shouldMapCustomException4xx() {
            ResponseEntity<ApiResponse<?>> response = handler.handleCustomException(
                    new CustomException("Bad input", HttpStatus.BAD_REQUEST)
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().message()).isEqualTo("Bad input");
        }

        @Test
        @DisplayName("should map CustomException 5xx as-is")
        void shouldMapCustomException5xx() {
            ResponseEntity<ApiResponse<?>> response = handler.handleCustomException(
                    new CustomException("System down", HttpStatus.INTERNAL_SERVER_ERROR)
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("System down");
        }

        @Test
        @DisplayName("should map data integrity error containing Email to 422")
        void shouldMapEmailDataIntegrityTo422() {
            ResponseEntity<ApiResponse<?>> response = handler.handleDataIntegrityViolationException(
                    new DataIntegrityViolationException("Email must be unique")
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).contains("Email");
        }

        @Test
        @DisplayName("should map non-email data integrity error to 409")
        void shouldMapNonEmailDataIntegrityTo409() {
            ResponseEntity<ApiResponse<?>> response = handler.handleDataIntegrityViolationException(
                    new DataIntegrityViolationException("duplicate key value violates unique constraint")
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).contains("duplicate key");
        }

        @Test
        @DisplayName("should map authentication errors to 401 and WWW-Authenticate header")
        void shouldMapAuthenticationException() {
            ResponseEntity<ApiResponse<?>> response = handler.handleAuthenticationException(
                    new AuthenticationException("Bad credentials") {
                    }
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Unauthorized");
        }

        @Test
        @DisplayName("should map access denied to 403")
        void shouldMapAccessDenied() {
            ResponseEntity<ApiResponse<?>> response = handler.handleAccessDenied(
                    new AccessDeniedException("forbidden")
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Access denied");
        }

        @Test
        @DisplayName("should map no-such-element to 404")
        void shouldMapNotFound() {
            ResponseEntity<ApiResponse<?>> response = handler.handleNotFound(
                    new NoSuchElementException("missing")
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Resource not found");
        }

        @Test
        @DisplayName("should map illegal argument to 400")
        void shouldMapIllegalArgument() {
            ResponseEntity<ApiResponse<?>> response = handler.handleIllegalArgument(
                    new IllegalArgumentException("invalid state")
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("invalid state");
        }

        @Test
        @DisplayName("should map fallback exception to 500")
        void shouldMapFallbackException() {
            ResponseEntity<ApiResponse<?>> response = handler.handleAll(new RuntimeException("boom"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Internal server error");
        }
    }

    // Protects validation and malformed payload error body contract.
    @Nested
    @DisplayName("Validation handlers")
    class ValidationHandlers {

        @Test
        @DisplayName("should return field-level map for method argument validation failures")
        void shouldMapMethodArgumentNotValid() throws Exception {
            Method method = TestInput.class.getDeclaredMethod("accept", String.class);
            MethodParameter methodParameter = new MethodParameter(method, 0);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

            ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                    ex,
                    new HttpHeaders(),
                    HttpStatus.BAD_REQUEST,
                    new ServletWebRequest(new MockHttpServletRequest())
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(ApiResponse.class);
            ApiResponse<?> body = (ApiResponse<?>) response.getBody();
            assertThat(body.message()).isEqualTo("Validation failed");
            assertThat(body.errors()).containsKey("email");
        }

        @Test
        @DisplayName("should map malformed body to generic message")
        void shouldMapMalformedBody() {
            ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(
                    new HttpMessageNotReadableException("Invalid JSON"),
                    new HttpHeaders(),
                    HttpStatus.BAD_REQUEST,
                    new ServletWebRequest(new MockHttpServletRequest())
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isInstanceOf(ApiResponse.class);
            ApiResponse<?> body = (ApiResponse<?>) response.getBody();
            assertThat(body.message()).isEqualTo("Malformed request body");
        }

        @Test
        @DisplayName("should use first constraint violation message")
        void shouldMapConstraintViolationMessage() {
            @SuppressWarnings("unchecked")
            ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("email is invalid");

            ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));
            ResponseEntity<ApiResponse<?>> response = handler.handleConstraintViolation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("email is invalid");
        }

        @Test
        @DisplayName("should use default constraint violation message when none exist")
        void shouldMapConstraintViolationFallback() {
            ConstraintViolationException ex = new ConstraintViolationException("none", Collections.emptySet());

            ResponseEntity<ApiResponse<?>> response = handler.handleConstraintViolation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Constraint violation");
        }
    }

    private static class TestInput {
        @SuppressWarnings("unused")
        void accept(String email) {
            // Method exists only to create a MethodParameter for validation error construction.
        }
    }
}

