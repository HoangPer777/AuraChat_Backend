package com.aurachat.common.exception;

import lombok.Getter;

/**
 * Exception thrown when input validation fails.
 * Contains information about the field that failed validation,
 * the invalid value, and the validation message.
 */
@Getter
public class ValidationException extends CustomException {
    
    /**
     * Name of the field that failed validation
     */
    private final String fieldName;
    
    /**
     * The invalid value that was provided
     */
    private final Object invalidValue;
    
    /**
     * Detailed validation message explaining why validation failed
     */
    private final String validationMessage;
    
    /**
     * Constructs a new ValidationException with field details.
     *
     * @param fieldName the name of the field that failed validation
     * @param invalidValue the invalid value that was provided
     * @param validationMessage detailed message explaining the validation failure
     */
    public ValidationException(String fieldName, Object invalidValue, String validationMessage) {
        super(ErrorCode.VALIDATION_FAILED.getCode(), validationMessage);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.validationMessage = validationMessage;
    }
    
    /**
     * Constructs a new ValidationException with a specific error code.
     *
     * @param errorCode the specific validation error code
     * @param fieldName the name of the field that failed validation
     * @param invalidValue the invalid value that was provided
     * @param validationMessage detailed message explaining the validation failure
     */
    public ValidationException(ErrorCode errorCode, String fieldName, Object invalidValue, String validationMessage) {
        super(errorCode.getCode(), validationMessage);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.validationMessage = validationMessage;
    }
    
    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, fieldName=%s, invalidValue=%s, message=%s, context=%s]",
                getClass().getSimpleName(),
                getErrorCode(),
                fieldName,
                invalidValue,
                getMessage(),
                getContext());
    }
}
