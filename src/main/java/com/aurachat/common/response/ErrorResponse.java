package com.aurachat.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response wrapper for error responses.
 * Provides a consistent structure for all error responses in the application.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    
    /**
     * Indicates whether the operation was successful.
     * Always false for ErrorResponse.
     */
    private boolean success;
    
    /**
     * Human-readable error message.
     */
    private String message;
    
    /**
     * Application-specific error code for categorizing errors.
     */
    private String errorCode;
    
    /**
     * Unique tracking identifier for this error instance.
     * Used for debugging and error tracking.
     */
    private String errorId;
    
    /**
     * Additional error details, such as field-specific validation errors.
     */
    private Map<String, Object> details;
    
    /**
     * Timestamp when the error occurred.
     */
    private LocalDateTime timestamp;
    
    /**
     * Creates an error response with error code, message, and tracking ID.
     *
     * @param errorCode Application-specific error code
     * @param message Error message
     * @param errorId Unique tracking identifier
     * @return ErrorResponse instance
     */
    public static ErrorResponse error(String errorCode, String message, String errorId) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorId(errorId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates an error response with error code, message, tracking ID, and additional details.
     *
     * @param errorCode Application-specific error code
     * @param message Error message
     * @param errorId Unique tracking identifier
     * @param details Additional error details
     * @return ErrorResponse instance
     */
    public static ErrorResponse error(String errorCode, String message, String errorId, Map<String, Object> details) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorId(errorId)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates an error response with only error code and message.
     *
     * @param errorCode Application-specific error code
     * @param message Error message
     * @return ErrorResponse instance
     */
    public static ErrorResponse error(String errorCode, String message) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
