package com.aurachat.common.exception;

import com.aurachat.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the AuraChat application.
 * Catches all unhandled exceptions and returns standardized error responses.
 * 
 * This handler provides:
 * - Consistent error response format across the application
 * - Unique error tracking IDs for debugging
 * - Appropriate HTTP status code mapping
 * - Structured logging with context information
 * - Sensitive data masking in logs
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    private static final DateTimeFormatter ERROR_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Handles ValidationException thrown by application code.
     * Returns HTTP 400 Bad Request with validation error details.
     */
    @ExceptionHandler(com.aurachat.common.exception.ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(
            com.aurachat.common.exception.ValidationException ex, 
            HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        logException(ex, errorId, request, "INFO");
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getFieldName() != null) {
            details.put(ex.getFieldName(), ex.getMessage());
        }
        
        return ErrorResponse.error(
                ex.getErrorCode(),
                ex.getMessage(),
                errorId,
                details.isEmpty() ? null : details
        );
    }
    
    /**
     * Handles Spring's MethodArgumentNotValidException for @Valid annotation failures.
     * Returns HTTP 400 Bad Request with field-specific validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        logException(ex, errorId, request, "INFO");
        
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        
        return ErrorResponse.error(
                ErrorCode.VALIDATION_FAILED.getCode(),
                "Validation failed",
                errorId,
                details
        );
    }
    
    /**
     * Handles AuthenticationException thrown by application code.
     * Returns HTTP 401 Unauthorized with authentication error details.
     */
    @ExceptionHandler(com.aurachat.common.exception.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(
            com.aurachat.common.exception.AuthenticationException ex,
            HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        logException(ex, errorId, request, "WARN");
        
        return ErrorResponse.error(
                ex.getErrorCode(),
                ex.getMessage(),
                errorId
        );
    }
    
    /**
     * Handles Spring Security's AuthenticationException.
     * Returns HTTP 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleSpringAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        logException(ex, errorId, request, "WARN");
        
        return ErrorResponse.error(
                ErrorCode.AUTH_FAILED.getCode(),
                "Authentication failed",
                errorId
        );
    }
    
    /**
     * Handles AuthorizationException thrown by application code.
     * Returns HTTP 403 Forbidden with authorization error details.
     */
    @ExceptionHandler(com.aurachat.common.exception.AuthorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAuthorizationException(
            com.aurachat.common.exception.AuthorizationException ex,
            HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        logException(ex, errorId, request, "WARN");
        
        return ErrorResponse.error(
                ex.getErrorCode(),
                ex.getMessage(),
                errorId
        );
    }
    
    /**
     * Handles Spring Security's AccessDeniedException.
     * Returns HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        logException(ex, errorId, request, "WARN");
        
        return ErrorResponse.error(
                ErrorCode.ACCESS_DENIED.getCode(),
                "Access denied",
                errorId
        );
    }
    
    /**
     * Handles BusinessLogicException thrown by application code.
     * Returns HTTP 422 Unprocessable Entity with business logic error details.
     */
    @ExceptionHandler(BusinessLogicException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleBusinessLogicException(
            BusinessLogicException ex,
            HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        logException(ex, errorId, request, "WARN");
        
        return ErrorResponse.error(
                ex.getErrorCode(),
                ex.getMessage(),
                errorId
        );
    }
    
    /**
     * Handles SystemException thrown by application code.
     * Returns HTTP 500 Internal Server Error with generic error message.
     * Detailed error information is logged but not exposed to clients.
     */
    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleSystemException(
            SystemException ex,
            HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        logException(ex, errorId, request, "ERROR");
        
        return ErrorResponse.error(
                ex.getErrorCode(),
                "Internal system error",
                errorId
        );
    }
    
    /**
     * Handles all other unhandled exceptions.
     * Returns HTTP 500 Internal Server Error with generic error message.
     * This is the catch-all handler for unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        logException(ex, errorId, request, "ERROR");
        
        return ErrorResponse.error(
                ErrorCode.SYSTEM_ERROR.getCode(),
                "Internal system error",
                errorId
        );
    }
    
    /**
     * Generates a unique error tracking ID.
     * Format: ERR_YYYYMMDD_HHMMSS_UUID
     * 
     * @return Unique error ID string
     */
    private String generateErrorId() {
        String timestamp = LocalDateTime.now().format(ERROR_ID_FORMATTER);
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ERR_%s_%s", timestamp, uuid);
    }
    
    /**
     * Logs exception with appropriate log level and context information.
     * Includes error ID, user ID, request URI, HTTP method, and exception details.
     * Sensitive information is masked in log messages.
     * 
     * @param ex The exception to log
     * @param errorId The unique error tracking ID
     * @param request The HTTP request that caused the exception
     * @param logLevel The log level to use (INFO, WARN, ERROR)
     */
    private void logException(Exception ex, String errorId, HttpServletRequest request, String logLevel) {
        String userId = getCurrentUserId();
        String requestUri = request.getRequestURI();
        String httpMethod = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        
        String logMessage = String.format(
                "Exception [%s] - User: %s, URI: %s %s, IP: %s, UserAgent: %s, Message: %s",
                errorId,
                userId != null ? userId : "anonymous",
                httpMethod,
                requestUri,
                ipAddress,
                userAgent != null ? userAgent : "unknown",
                maskSensitiveData(ex.getMessage())
        );
        
        switch (logLevel) {
            case "ERROR":
                log.error(logMessage, ex);
                break;
            case "WARN":
                log.warn(logMessage);
                break;
            case "INFO":
            default:
                log.info(logMessage);
                break;
        }
    }
    
    /**
     * Gets the current user ID from the security context.
     * Returns null if no user is authenticated.
     * 
     * @return User ID or null
     */
    private String getCurrentUserId() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // Ignore exceptions when getting user ID
        }
        return null;
    }
    
    /**
     * Gets the client IP address from the request.
     * Checks X-Forwarded-For header for proxied requests.
     * 
     * @param request The HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Masks sensitive information in log messages.
     * Replaces passwords, tokens, and secrets with asterisks.
     * 
     * @param message The message to mask
     * @return Masked message
     */
    private String maskSensitiveData(String message) {
        if (message == null) {
            return null;
        }
        
        // Mask common sensitive field patterns
        return message
                .replaceAll("(?i)(password|token|secret|key|authorization)[\"']?\\s*[:=]\\s*[\"']?[^\\s,\"']+", "$1=***")
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9\\-._~+/]+=*", "Bearer ***");
    }
}
