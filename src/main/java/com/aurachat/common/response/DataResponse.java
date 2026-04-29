package com.aurachat.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic response wrapper for successful API responses.
 * Provides a consistent structure for all success responses in the application.
 *
 * @param <T> The type of data being returned
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataResponse<T> {
    
    /**
     * Indicates whether the operation was successful.
     * Always true for DataResponse.
     */
    private boolean success;
    
    /**
     * Human-readable message describing the operation result.
     */
    private String message;
    
    /**
     * The actual response payload.
     */
    private T data;
    
    /**
     * Timestamp when the response was generated.
     */
    private LocalDateTime timestamp;
    
    /**
     * Creates a successful response with data and message.
     *
     * @param data The response payload
     * @param message Success message
     * @param <T> The type of data
     * @return DataResponse instance
     */
    public static <T> DataResponse<T> success(T data, String message) {
        return DataResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a successful response with data and default message.
     *
     * @param data The response payload
     * @param <T> The type of data
     * @return DataResponse instance
     */
    public static <T> DataResponse<T> success(T data) {
        return success(data, "Operation completed successfully");
    }
    
    /**
     * Creates a successful response with only a message (no data).
     *
     * @param message Success message
     * @param <T> The type of data
     * @return DataResponse instance
     */
    public static <T> DataResponse<T> success(String message) {
        return DataResponse.<T>builder()
                .success(true)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
