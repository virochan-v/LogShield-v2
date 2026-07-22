package com.logshield.logshieldv2.exception;

import com.logshield.logshieldv2.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
    public ResponseEntity<ApiResponse<Void>> handleLogShieldException(
            LogShieldException ex) {

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catches any unhandled exception — last line of defence.
     * Returns HTTP 500 Internal Server Error.
     * Never exposes internal stack traces to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred. Please try again."));
    }
}