package com.logshield.logshieldv2.exception;

import com.logshield.logshieldv2.model.LogShieldResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;import java.util.stream.Collectors;

/**
 * Centralized exception handler for all LogShield v2 controllers.
 *
 * @RestControllerAdvice intercepts every exception thrown anywhere
 * in the application and converts it to a proper HTTP response.
 * Controllers never handle exceptions directly — clean separation.
 *
 * DIP compliant — controllers depend on throwing exceptions,
 * not on knowing how to format error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all LogShield-specific exceptions.
     * Returns the HTTP status code embedded in the exception.
     */
    @ExceptionHandler(LogShieldException.class)
    public ResponseEntity<LogShieldResponse<Void>> handleLogShieldException(
            LogShieldException ex) {

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(LogShieldResponse.error(ex.getMessage()));
    }

    /**
     * Handles Bean Validation failures — @NotBlank, @Pattern, @Size violations.
     * Collects all field errors into one readable message.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<LogShieldResponse<Void>>
    handleValidationException(MethodArgumentNotValidException ex) {

        // Collect all field error messages into one string
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(LogShieldResponse.error(
                        "Validation failed: " + errors));
    }

    /**
     * Catches any unhandled exception — last line of defence.
     * Returns HTTP 500 Internal Server Error.
     * Never exposes internal stack traces to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<LogShieldResponse<Void>> handleGenericException(
            Exception ex,
            jakarta.servlet.http.HttpServletRequest request) {

        String path = request.getRequestURI();

        // Let Spring handle Springdoc internal errors — do not intercept
        if (path.contains("/v3/api-docs") || path.contains("/swagger")) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LogShieldResponse.error(
                            "Springdoc error: " + ex.getMessage()));
        }

        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(LogShieldResponse.error(
                        "An unexpected error occurred. Please try again."));
    }
}