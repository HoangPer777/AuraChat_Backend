package com.aurachat.common.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Base abstract class for all custom exceptions in the AuraChat application.
 * Provides error code support and context information for exception handling.
 * 
 * All custom exceptions should extend this class to ensure consistent
 * error handling and reporting across the application.
 */
@Getter
public abstract class CustomException extends RuntimeException {
    
    /**
     * Application-specific error code for this exception
     */
    private final String errorCode;
    
    /**
     * Additional context information about the exception
     */
    private final Map<String, Object> context;
    
    /**
     * Constructs a new CustomException with the specified error code and message.
     *
     * @param errorCode the application-specific error code
     * @param message the detail message
     */
    protected CustomException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    /**
     * Constructs a new CustomException with the specified error code, message, and cause.
     *
     * @param errorCode the application-specific error code
     * @param message the detail message
     * @param cause the cause of this exception
     */
    protected CustomException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }
    
    /**
     * Adds a context entry to this exception.
     *
     * @param key the context key
     * @param value the context value
     * @return this exception instance for method chaining
     */
    public CustomException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, message=%s, context=%s]",
                getClass().getSimpleName(),
                errorCode,
                getMessage(),
                context);
    }
}
