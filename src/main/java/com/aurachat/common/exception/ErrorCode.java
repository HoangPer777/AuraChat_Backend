package com.aurachat.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

/**
 * Enumeration of all application error codes with HTTP status mappings.
 * Error codes are grouped by module for better organization and maintainability.
 * 
 * Naming convention: MODULE_ERROR_TYPE
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // ========== Authentication & Authorization ==========
    
    /**
     * Invalid username or password provided during login
     */
    AUTH_INVALID_CREDENTIALS("AUTH_001", "Invalid username or password", HttpStatus.UNAUTHORIZED),
    
    /**
     * Authentication token has expired and needs to be refreshed
     */
    AUTH_TOKEN_EXPIRED("AUTH_002", "Authentication token has expired", HttpStatus.UNAUTHORIZED),
    
    /**
     * Authentication token is invalid or malformed
     */
    AUTH_TOKEN_INVALID("AUTH_003", "Invalid authentication token", HttpStatus.UNAUTHORIZED),
    
    /**
     * User account is locked due to security reasons
     */
    AUTH_ACCOUNT_LOCKED("AUTH_004", "Account is locked", HttpStatus.UNAUTHORIZED),
    
    /**
     * User does not have permission to access the requested resource
     */
    ACCESS_DENIED("AUTH_005", "Access denied", HttpStatus.FORBIDDEN),
    
    /**
     * Authentication failed for unspecified reason
     */
    AUTH_FAILED("AUTH_006", "Authentication failed", HttpStatus.UNAUTHORIZED),
    
    // ========== Validation ==========
    
    /**
     * General validation failure
     */
    VALIDATION_FAILED("VAL_001", "Validation failed", HttpStatus.BAD_REQUEST),
    
    /**
     * Required field is missing from the request
     */
    VALIDATION_REQUIRED_FIELD("VAL_002", "Required field is missing", HttpStatus.BAD_REQUEST),
    
    /**
     * Field format is invalid (e.g., email format, date format)
     */
    VALIDATION_INVALID_FORMAT("VAL_003", "Invalid field format", HttpStatus.BAD_REQUEST),
    
    /**
     * Field value exceeds maximum allowed length
     */
    VALIDATION_VALUE_TOO_LONG("VAL_004", "Field value exceeds maximum length", HttpStatus.BAD_REQUEST),
    
    // ========== Business Logic - User Management ==========
    
    /**
     * Email address already exists in the system
     */
    USER_EMAIL_EXISTS("USER_001", "Email address already exists", HttpStatus.UNPROCESSABLE_ENTITY),
    
    /**
     * User with the specified ID or email not found
     */
    USER_NOT_FOUND("USER_002", "User not found", HttpStatus.NOT_FOUND),
    
    /**
     * Failed to update user profile information
     */
    USER_PROFILE_UPDATE_FAILED("USER_003", "Failed to update user profile", HttpStatus.UNPROCESSABLE_ENTITY),
    
    // ========== Business Logic - Friend Management ==========
    
    /**
     * Friend request already exists between the two users
     */
    FRIEND_REQUEST_EXISTS("FRIEND_001", "Friend request already exists", HttpStatus.UNPROCESSABLE_ENTITY),
    
    /**
     * Friend request with the specified ID not found
     */
    FRIEND_REQUEST_NOT_FOUND("FRIEND_002", "Friend request not found", HttpStatus.NOT_FOUND),
    
    /**
     * Users are already friends
     */
    FRIEND_ALREADY_EXISTS("FRIEND_003", "Users are already friends", HttpStatus.UNPROCESSABLE_ENTITY),
    
    /**
     * Cannot send friend request to yourself
     */
    FRIEND_SELF_REQUEST("FRIEND_004", "Cannot send friend request to yourself", HttpStatus.UNPROCESSABLE_ENTITY),
    
    // ========== Business Logic - Message Management ==========
    
    /**
     * Conversation with the specified ID not found
     */
    MESSAGE_CONVERSATION_NOT_FOUND("MSG_001", "Conversation not found", HttpStatus.NOT_FOUND),
    
    /**
     * Failed to send message
     */
    MESSAGE_SEND_FAILED("MSG_002", "Failed to send message", HttpStatus.UNPROCESSABLE_ENTITY),
    
    /**
     * User does not have access to the conversation
     */
    MESSAGE_UNAUTHORIZED_ACCESS("MSG_003", "Unauthorized access to conversation", HttpStatus.FORBIDDEN),

    /**
     * Message with the specified ID not found
     */
    MESSAGE_NOT_FOUND("MSG_004", "Message not found", HttpStatus.NOT_FOUND),

    /**
     * User is not a group admin and cannot perform this action
     */
    NOT_GROUP_ADMIN("MSG_005", "Only group admin can perform this action", HttpStatus.FORBIDDEN),

    /**
     * User is already a member of the conversation
     */
    CONVERSATION_MEMBER_EXISTS("MSG_006", "User is already a member of this conversation", HttpStatus.UNPROCESSABLE_ENTITY),
    
    // ========== Business Logic - Call Management ==========
    
    /**
     * User is currently busy and cannot receive calls
     */
    CALL_USER_BUSY("CALL_001", "User is currently busy", HttpStatus.UNPROCESSABLE_ENTITY),
    
    /**
     * Failed to establish call connection
     */
    CALL_CONNECTION_FAILED("CALL_002", "Failed to establish call connection", HttpStatus.UNPROCESSABLE_ENTITY),
    
    // ========== Business Logic - Media Management ==========
    
    /**
     * Failed to upload media file
     */
    MEDIA_UPLOAD_FAILED("MEDIA_001", "Failed to upload media file", HttpStatus.UNPROCESSABLE_ENTITY),
    
    /**
     * Media file type is not supported
     */
    MEDIA_INVALID_TYPE("MEDIA_002", "Invalid media file type", HttpStatus.BAD_REQUEST),
    
    /**
     * Media file size exceeds the maximum allowed limit
     */
    MEDIA_SIZE_EXCEEDED("MEDIA_003", "Media file size exceeds limit", HttpStatus.BAD_REQUEST),
    
    // ========== System Errors ==========
    
    /**
     * Internal system error occurred
     */
    SYSTEM_ERROR("SYS_001", "Internal system error", HttpStatus.INTERNAL_SERVER_ERROR),
    
    /**
     * Database operation failed
     */
    DATABASE_ERROR("SYS_002", "Database operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    
    /**
     * External service is unavailable
     */
    EXTERNAL_SERVICE_ERROR("SYS_003", "External service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    
    /**
     * System configuration error
     */
    CONFIGURATION_ERROR("SYS_004", "System configuration error", HttpStatus.INTERNAL_SERVER_ERROR);
    
    /**
     * Unique error code identifier
     */
    private final String code;
    
    /**
     * Default error message for this error code
     */
    private final String defaultMessage;
    
    /**
     * HTTP status code to return for this error
     */
    private final HttpStatus httpStatus;
    
    /**
     * Finds an ErrorCode by its code string.
     * Returns SYSTEM_ERROR if the code is not found.
     *
     * @param code the error code string to search for
     * @return the matching ErrorCode, or SYSTEM_ERROR if not found
     */
    public static ErrorCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.getCode().equals(code))
                .findFirst()
                .orElse(SYSTEM_ERROR);
    }
}
