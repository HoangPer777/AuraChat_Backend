package com.aurachat.common.exception;

import lombok.Getter;

/**
 * Exception thrown when a system-level error occurs.
 * Contains information about the system component where the error occurred
 * and the underlying cause of the error.
 */
@Getter
public class SystemException extends CustomException {
    
    /**
     * The system component where the error occurred
     */
    private final String component;
    
    /**
     * Constructs a new SystemException with component, message, and cause.
     *
     * @param component the system component where the error occurred
     * @param message detailed error message
     * @param cause the underlying cause of the error
     */
    public SystemException(String component, String message, Throwable cause) {
        super(ErrorCode.SYSTEM_ERROR.getCode(), message, cause);
        this.component = component;
    }
    
    /**
     * Constructs a new SystemException with a specific error code.
     *
     * @param errorCode the specific system error code
     * @param component the system component where the error occurred
     * @param message detailed error message
     * @param cause the underlying cause of the error
     */
    public SystemException(ErrorCode errorCode, String component, String message, Throwable cause) {
        super(errorCode.getCode(), message, cause);
        this.component = component;
    }
    
    /**
     * Constructs a new SystemException without a cause.
     *
     * @param component the system component where the error occurred
     * @param message detailed error message
     */
    public SystemException(String component, String message) {
        super(ErrorCode.SYSTEM_ERROR.getCode(), message);
        this.component = component;
    }
    
    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, component=%s, message=%s, context=%s]",
                getClass().getSimpleName(),
                getErrorCode(),
                component,
                getMessage(),
                getContext());
    }
}
