package com.aurachat.common.exception;

import lombok.Getter;

/**
 * Exception thrown when authentication fails.
 * Contains information about the reason for authentication failure
 * and the action that was attempted.
 */
@Getter
public class AuthenticationException extends CustomException {
    
    /**
     * Reason why authentication failed
     */
    private final String reason;
    
    /**
     * Action that was attempted when authentication failed
     */
    private final String attemptedAction;
    
    /**
     * Constructs a new AuthenticationException with reason and attempted action.
     *
     * @param reason the reason why authentication failed
     * @param attemptedAction the action that was attempted
     */
    public AuthenticationException(String reason, String attemptedAction) {
        super(ErrorCode.AUTH_FAILED.getCode(), "Authentication failed: " + reason);
        this.reason = reason;
        this.attemptedAction = attemptedAction;
    }
    
    /**
     * Constructs a new AuthenticationException with a specific error code.
     *
     * @param errorCode the specific authentication error code
     * @param reason the reason why authentication failed
     * @param attemptedAction the action that was attempted
     */
    public AuthenticationException(ErrorCode errorCode, String reason, String attemptedAction) {
        super(errorCode.getCode(), errorCode.getDefaultMessage() + ": " + reason);
        this.reason = reason;
        this.attemptedAction = attemptedAction;
    }
    
    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, reason=%s, attemptedAction=%s, message=%s, context=%s]",
                getClass().getSimpleName(),
                getErrorCode(),
                reason,
                attemptedAction,
                getMessage(),
                getContext());
    }
}
