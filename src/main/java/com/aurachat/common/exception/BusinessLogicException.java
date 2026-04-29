package com.aurachat.common.exception;

import lombok.Getter;

/**
 * Exception thrown when a business rule is violated.
 * Contains information about the business rule that was violated
 * and the context in which the violation occurred.
 */
@Getter
public class BusinessLogicException extends CustomException {
    
    /**
     * Description of the business rule that was violated
     */
    private final String businessRule;
    
    /**
     * Constructs a new BusinessLogicException with an error code and business rule.
     *
     * @param errorCode the specific business logic error code
     * @param businessRule description of the business rule that was violated
     */
    public BusinessLogicException(ErrorCode errorCode, String businessRule) {
        super(errorCode.getCode(), errorCode.getDefaultMessage());
        this.businessRule = businessRule;
    }
    
    /**
     * Constructs a new BusinessLogicException with an error code, custom message, and business rule.
     *
     * @param errorCode the specific business logic error code
     * @param message custom error message
     * @param businessRule description of the business rule that was violated
     */
    public BusinessLogicException(ErrorCode errorCode, String message, String businessRule) {
        super(errorCode.getCode(), message);
        this.businessRule = businessRule;
    }
    
    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, businessRule=%s, message=%s, context=%s]",
                getClass().getSimpleName(),
                getErrorCode(),
                businessRule,
                getMessage(),
                getContext());
    }
}
