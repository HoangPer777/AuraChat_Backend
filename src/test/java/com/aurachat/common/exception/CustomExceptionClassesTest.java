package com.aurachat.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all custom exception classes.
 * Tests exception creation with different parameters, getter methods, and toString implementations.
 * 
 * Requirements: 4.7, 4.8
 */
@DisplayName("Custom Exception Classes Tests")
class CustomExceptionClassesTest {
    
    @Nested
    @DisplayName("ValidationException Tests")
    class ValidationExceptionTests {
        
        @Test
        @DisplayName("Should create ValidationException with field details")
        void shouldCreateValidationExceptionWithFieldDetails() {
            // Given
            String fieldName = "email";
            String invalidValue = "invalid-email";
            String validationMessage = "Email format is invalid";
            
            // When
            ValidationException exception = new ValidationException(fieldName, invalidValue, validationMessage);
            
            // Then
            assertNotNull(exception);
            assertEquals(ErrorCode.VALIDATION_FAILED.getCode(), exception.getErrorCode());
            assertEquals(validationMessage, exception.getMessage());
            assertEquals(fieldName, exception.getFieldName());
            assertEquals(invalidValue, exception.getInvalidValue());
            assertEquals(validationMessage, exception.getValidationMessage());
            assertNotNull(exception.getContext());
            assertTrue(exception.getContext().isEmpty());
        }
        
        @Test
        @DisplayName("Should create ValidationException with specific error code")
        void shouldCreateValidationExceptionWithSpecificErrorCode() {
            // Given
            ErrorCode errorCode = ErrorCode.VALIDATION_REQUIRED_FIELD;
            String fieldName = "username";
            Object invalidValue = null;
            String validationMessage = "Username is required";
            
            // When
            ValidationException exception = new ValidationException(errorCode, fieldName, invalidValue, validationMessage);
            
            // Then
            assertNotNull(exception);
            assertEquals(errorCode.getCode(), exception.getErrorCode());
            assertEquals(validationMessage, exception.getMessage());
            assertEquals(fieldName, exception.getFieldName());
            assertNull(exception.getInvalidValue());
            assertEquals(validationMessage, exception.getValidationMessage());
        }
        
        @Test
        @DisplayName("Should handle null invalid value")
        void shouldHandleNullInvalidValue() {
            // Given
            String fieldName = "password";
            Object invalidValue = null;
            String validationMessage = "Password is required";
            
            // When
            ValidationException exception = new ValidationException(fieldName, invalidValue, validationMessage);
            
            // Then
            assertNull(exception.getInvalidValue());
            assertEquals(fieldName, exception.getFieldName());
        }
        
        @Test
        @DisplayName("Should handle complex object as invalid value")
        void shouldHandleComplexObjectAsInvalidValue() {
            // Given
            String fieldName = "user";
            Object invalidValue = new Object() {
                @Override
                public String toString() {
                    return "ComplexObject";
                }
            };
            String validationMessage = "Invalid user object";
            
            // When
            ValidationException exception = new ValidationException(fieldName, invalidValue, validationMessage);
            
            // Then
            assertNotNull(exception.getInvalidValue());
            assertEquals(invalidValue, exception.getInvalidValue());
        }
        
        @Test
        @DisplayName("Should provide meaningful toString representation")
        void shouldProvideMeaningfulToString() {
            // Given
            String fieldName = "age";
            Integer invalidValue = -5;
            String validationMessage = "Age must be positive";
            ValidationException exception = new ValidationException(fieldName, invalidValue, validationMessage);
            
            // When
            String result = exception.toString();
            
            // Then
            assertNotNull(result);
            assertTrue(result.contains("ValidationException"));
            assertTrue(result.contains(fieldName));
            assertTrue(result.contains(invalidValue.toString()));
            assertTrue(result.contains(ErrorCode.VALIDATION_FAILED.getCode()));
        }
        
        @Test
        @DisplayName("Should support context addition")
        void shouldSupportContextAddition() {
            // Given
            ValidationException exception = new ValidationException("email", "bad@", "Invalid email");
            
            // When
            exception.addContext("attemptedAt", "2024-12-01T10:00:00");
            exception.addContext("ipAddress", "192.168.1.1");
            
            // Then
            assertEquals(2, exception.getContext().size());
            assertEquals("2024-12-01T10:00:00", exception.getContext().get("attemptedAt"));
            assertEquals("192.168.1.1", exception.getContext().get("ipAddress"));
        }
    }
    
    @Nested
    @DisplayName("BusinessLogicException Tests")
    class BusinessLogicExceptionTests {
        
        @Test
        @DisplayName("Should create BusinessLogicException with error code and business rule")
        void shouldCreateBusinessLogicExceptionWithErrorCodeAndBusinessRule() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_EMAIL_EXISTS;
            String businessRule = "Email must be unique";
            
            // When
            BusinessLogicException exception = new BusinessLogicException(errorCode, businessRule);
            
            // Then
            assertNotNull(exception);
            assertEquals(errorCode.getCode(), exception.getErrorCode());
            assertEquals(errorCode.getDefaultMessage(), exception.getMessage());
            assertEquals(businessRule, exception.getBusinessRule());
            assertNotNull(exception.getContext());
        }
        
        @Test
        @DisplayName("Should create BusinessLogicException with custom message")
        void shouldCreateBusinessLogicExceptionWithCustomMessage() {
            // Given
            ErrorCode errorCode = ErrorCode.FRIEND_ALREADY_EXISTS;
            String customMessage = "You are already friends with this user";
            String businessRule = "Cannot send friend request to existing friend";
            
            // When
            BusinessLogicException exception = new BusinessLogicException(errorCode, customMessage, businessRule);
            
            // Then
            assertNotNull(exception);
            assertEquals(errorCode.getCode(), exception.getErrorCode());
            assertEquals(customMessage, exception.getMessage());
            assertEquals(businessRule, exception.getBusinessRule());
        }
        
        @Test
        @DisplayName("Should handle different error codes")
        void shouldHandleDifferentErrorCodes() {
            // Given
            ErrorCode[] errorCodes = {
                ErrorCode.USER_EMAIL_EXISTS,
                ErrorCode.FRIEND_REQUEST_EXISTS,
                ErrorCode.MESSAGE_SEND_FAILED,
                ErrorCode.CALL_USER_BUSY
            };
            
            // When & Then
            for (ErrorCode errorCode : errorCodes) {
                BusinessLogicException exception = new BusinessLogicException(errorCode, "Test rule");
                assertEquals(errorCode.getCode(), exception.getErrorCode());
                assertEquals(errorCode.getDefaultMessage(), exception.getMessage());
            }
        }
        
        @Test
        @DisplayName("Should provide meaningful toString representation")
        void shouldProvideMeaningfulToString() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_EMAIL_EXISTS;
            String businessRule = "Email uniqueness constraint";
            BusinessLogicException exception = new BusinessLogicException(errorCode, businessRule);
            
            // When
            String result = exception.toString();
            
            // Then
            assertNotNull(result);
            assertTrue(result.contains("BusinessLogicException"));
            assertTrue(result.contains(errorCode.getCode()));
            assertTrue(result.contains(businessRule));
        }
        
        @Test
        @DisplayName("Should support context addition")
        void shouldSupportContextAddition() {
            // Given
            BusinessLogicException exception = new BusinessLogicException(
                ErrorCode.USER_EMAIL_EXISTS, 
                "Email uniqueness"
            );
            
            // When
            exception.addContext("email", "test@example.com");
            exception.addContext("userId", 123L);
            
            // Then
            assertEquals(2, exception.getContext().size());
            assertEquals("test@example.com", exception.getContext().get("email"));
            assertEquals(123L, exception.getContext().get("userId"));
        }
    }
    
    @Nested
    @DisplayName("AuthenticationException Tests")
    class AuthenticationExceptionTests {
        
        @Test
        @DisplayName("Should create AuthenticationException with reason and attempted action")
        void shouldCreateAuthenticationExceptionWithReasonAndAction() {
            // Given
            String reason = "Invalid password";
            String attemptedAction = "login";
            
            // When
            AuthenticationException exception = new AuthenticationException(reason, attemptedAction);
            
            // Then
            assertNotNull(exception);
            assertEquals(ErrorCode.AUTH_FAILED.getCode(), exception.getErrorCode());
            assertTrue(exception.getMessage().contains(reason));
            assertEquals(reason, exception.getReason());
            assertEquals(attemptedAction, exception.getAttemptedAction());
            assertNotNull(exception.getContext());
        }
        
        @Test
        @DisplayName("Should create AuthenticationException with specific error code")
        void shouldCreateAuthenticationExceptionWithSpecificErrorCode() {
            // Given
            ErrorCode errorCode = ErrorCode.AUTH_TOKEN_EXPIRED;
            String reason = "Token expired 5 minutes ago";
            String attemptedAction = "access protected resource";
            
            // When
            AuthenticationException exception = new AuthenticationException(errorCode, reason, attemptedAction);
            
            // Then
            assertNotNull(exception);
            assertEquals(errorCode.getCode(), exception.getErrorCode());
            assertTrue(exception.getMessage().contains(errorCode.getDefaultMessage()));
            assertTrue(exception.getMessage().contains(reason));
            assertEquals(reason, exception.getReason());
            assertEquals(attemptedAction, exception.getAttemptedAction());
        }
        
        @Test
        @DisplayName("Should handle different authentication error codes")
        void shouldHandleDifferentAuthenticationErrorCodes() {
            // Given
            ErrorCode[] errorCodes = {
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                ErrorCode.AUTH_TOKEN_EXPIRED,
                ErrorCode.AUTH_TOKEN_INVALID,
                ErrorCode.AUTH_ACCOUNT_LOCKED
            };
            
            // When & Then
            for (ErrorCode errorCode : errorCodes) {
                AuthenticationException exception = new AuthenticationException(
                    errorCode, 
                    "Test reason", 
                    "Test action"
                );
                assertEquals(errorCode.getCode(), exception.getErrorCode());
            }
        }
        
        @Test
        @DisplayName("Should provide meaningful toString representation")
        void shouldProvideMeaningfulToString() {
            // Given
            String reason = "Invalid credentials";
            String attemptedAction = "login";
            AuthenticationException exception = new AuthenticationException(reason, attemptedAction);
            
            // When
            String result = exception.toString();
            
            // Then
            assertNotNull(result);
            assertTrue(result.contains("AuthenticationException"));
            assertTrue(result.contains(reason));
            assertTrue(result.contains(attemptedAction));
            assertTrue(result.contains(ErrorCode.AUTH_FAILED.getCode()));
        }
        
        @Test
        @DisplayName("Should support context addition")
        void shouldSupportContextAddition() {
            // Given
            AuthenticationException exception = new AuthenticationException(
                "Invalid token", 
                "API access"
            );
            
            // When
            exception.addContext("username", "testuser");
            exception.addContext("timestamp", "2024-12-01T10:00:00");
            
            // Then
            assertEquals(2, exception.getContext().size());
            assertEquals("testuser", exception.getContext().get("username"));
        }
    }
    
    @Nested
    @DisplayName("AuthorizationException Tests")
    class AuthorizationExceptionTests {
        
        @Test
        @DisplayName("Should create AuthorizationException with resource and permission")
        void shouldCreateAuthorizationExceptionWithResourceAndPermission() {
            // Given
            String resource = "/api/admin/users";
            String requiredPermission = "ADMIN_ACCESS";
            
            // When
            AuthorizationException exception = new AuthorizationException(resource, requiredPermission);
            
            // Then
            assertNotNull(exception);
            assertEquals(ErrorCode.ACCESS_DENIED.getCode(), exception.getErrorCode());
            assertTrue(exception.getMessage().contains(resource));
            assertEquals(resource, exception.getResource());
            assertEquals(requiredPermission, exception.getRequiredPermission());
            assertNull(exception.getCurrentUser());
            assertNotNull(exception.getContext());
        }
        
        @Test
        @DisplayName("Should create AuthorizationException with current user")
        void shouldCreateAuthorizationExceptionWithCurrentUser() {
            // Given
            String resource = "/api/messages/123";
            String requiredPermission = "READ_MESSAGE";
            String currentUser = "user@example.com";
            
            // When
            AuthorizationException exception = new AuthorizationException(
                resource, 
                requiredPermission, 
                currentUser
            );
            
            // Then
            assertNotNull(exception);
            assertEquals(ErrorCode.ACCESS_DENIED.getCode(), exception.getErrorCode());
            assertTrue(exception.getMessage().contains(resource));
            assertTrue(exception.getMessage().contains(requiredPermission));
            assertTrue(exception.getMessage().contains(currentUser));
            assertEquals(resource, exception.getResource());
            assertEquals(requiredPermission, exception.getRequiredPermission());
            assertEquals(currentUser, exception.getCurrentUser());
        }
        
        @Test
        @DisplayName("Should handle different resource types")
        void shouldHandleDifferentResourceTypes() {
            // Given
            String[] resources = {
                "/api/admin/users",
                "/api/messages/123",
                "/api/calls/456",
                "conversation:789"
            };
            
            // When & Then
            for (String resource : resources) {
                AuthorizationException exception = new AuthorizationException(
                    resource, 
                    "READ"
                );
                assertEquals(resource, exception.getResource());
                assertTrue(exception.getMessage().contains(resource));
            }
        }
        
        @Test
        @DisplayName("Should provide meaningful toString representation")
        void shouldProvideMeaningfulToString() {
            // Given
            String resource = "/api/admin";
            String permission = "ADMIN_ACCESS";
            String user = "testuser";
            AuthorizationException exception = new AuthorizationException(resource, permission, user);
            
            // When
            String result = exception.toString();
            
            // Then
            assertNotNull(result);
            assertTrue(result.contains("AuthorizationException"));
            assertTrue(result.contains(resource));
            assertTrue(result.contains(permission));
            assertTrue(result.contains(user));
            assertTrue(result.contains(ErrorCode.ACCESS_DENIED.getCode()));
        }
        
        @Test
        @DisplayName("Should support context addition")
        void shouldSupportContextAddition() {
            // Given
            AuthorizationException exception = new AuthorizationException(
                "/api/admin", 
                "ADMIN_ACCESS"
            );
            
            // When
            exception.addContext("requestedAt", "2024-12-01T10:00:00");
            exception.addContext("ipAddress", "192.168.1.1");
            
            // Then
            assertEquals(2, exception.getContext().size());
            assertEquals("2024-12-01T10:00:00", exception.getContext().get("requestedAt"));
        }
    }
    
    @Nested
    @DisplayName("SystemException Tests")
    class SystemExceptionTests {
        
        @Test
        @DisplayName("Should create SystemException with component, message, and cause")
        void shouldCreateSystemExceptionWithComponentMessageAndCause() {
            // Given
            String component = "DatabaseService";
            String message = "Failed to connect to database";
            Throwable cause = new RuntimeException("Connection timeout");
            
            // When
            SystemException exception = new SystemException(component, message, cause);
            
            // Then
            assertNotNull(exception);
            assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getErrorCode());
            assertEquals(message, exception.getMessage());
            assertEquals(component, exception.getComponent());
            assertEquals(cause, exception.getCause());
            assertNotNull(exception.getContext());
        }
        
        @Test
        @DisplayName("Should create SystemException with specific error code")
        void shouldCreateSystemExceptionWithSpecificErrorCode() {
            // Given
            ErrorCode errorCode = ErrorCode.DATABASE_ERROR;
            String component = "UserRepository";
            String message = "Failed to execute query";
            Throwable cause = new RuntimeException("SQL syntax error");
            
            // When
            SystemException exception = new SystemException(errorCode, component, message, cause);
            
            // Then
            assertNotNull(exception);
            assertEquals(errorCode.getCode(), exception.getErrorCode());
            assertEquals(message, exception.getMessage());
            assertEquals(component, exception.getComponent());
            assertEquals(cause, exception.getCause());
        }
        
        @Test
        @DisplayName("Should create SystemException without cause")
        void shouldCreateSystemExceptionWithoutCause() {
            // Given
            String component = "ConfigurationService";
            String message = "Invalid configuration parameter";
            
            // When
            SystemException exception = new SystemException(component, message);
            
            // Then
            assertNotNull(exception);
            assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getErrorCode());
            assertEquals(message, exception.getMessage());
            assertEquals(component, exception.getComponent());
            assertNull(exception.getCause());
        }
        
        @Test
        @DisplayName("Should handle different system error codes")
        void shouldHandleDifferentSystemErrorCodes() {
            // Given
            ErrorCode[] errorCodes = {
                ErrorCode.SYSTEM_ERROR,
                ErrorCode.DATABASE_ERROR,
                ErrorCode.EXTERNAL_SERVICE_ERROR,
                ErrorCode.CONFIGURATION_ERROR
            };
            
            // When & Then
            for (ErrorCode errorCode : errorCodes) {
                SystemException exception = new SystemException(
                    errorCode,
                    "TestComponent",
                    "Test message",
                    new RuntimeException("Test cause")
                );
                assertEquals(errorCode.getCode(), exception.getErrorCode());
            }
        }
        
        @Test
        @DisplayName("Should handle different component names")
        void shouldHandleDifferentComponentNames() {
            // Given
            String[] components = {
                "DatabaseService",
                "EmailService",
                "FileStorageService",
                "CacheService",
                "MessageQueueService"
            };
            
            // When & Then
            for (String component : components) {
                SystemException exception = new SystemException(
                    component,
                    "Test error",
                    new RuntimeException("Test")
                );
                assertEquals(component, exception.getComponent());
            }
        }
        
        @Test
        @DisplayName("Should preserve cause exception chain")
        void shouldPreserveCauseExceptionChain() {
            // Given
            Throwable rootCause = new IllegalArgumentException("Invalid argument");
            Throwable intermediateCause = new RuntimeException("Processing failed", rootCause);
            String component = "DataProcessor";
            String message = "Failed to process data";
            
            // When
            SystemException exception = new SystemException(component, message, intermediateCause);
            
            // Then
            assertEquals(intermediateCause, exception.getCause());
            assertEquals(rootCause, exception.getCause().getCause());
        }
        
        @Test
        @DisplayName("Should provide meaningful toString representation")
        void shouldProvideMeaningfulToString() {
            // Given
            String component = "EmailService";
            String message = "Failed to send email";
            SystemException exception = new SystemException(component, message);
            
            // When
            String result = exception.toString();
            
            // Then
            assertNotNull(result);
            assertTrue(result.contains("SystemException"));
            assertTrue(result.contains(component));
            assertTrue(result.contains(ErrorCode.SYSTEM_ERROR.getCode()));
        }
        
        @Test
        @DisplayName("Should support context addition")
        void shouldSupportContextAddition() {
            // Given
            SystemException exception = new SystemException(
                "DatabaseService",
                "Connection failed"
            );
            
            // When
            exception.addContext("host", "localhost");
            exception.addContext("port", 5432);
            exception.addContext("database", "aurachat");
            
            // Then
            assertEquals(3, exception.getContext().size());
            assertEquals("localhost", exception.getContext().get("host"));
            assertEquals(5432, exception.getContext().get("port"));
            assertEquals("aurachat", exception.getContext().get("database"));
        }
    }
    
    @Nested
    @DisplayName("Cross-Exception Tests")
    class CrossExceptionTests {
        
        @Test
        @DisplayName("All exceptions should extend CustomException")
        void allExceptionsShouldExtendCustomException() {
            // Given & When & Then
            assertTrue(new ValidationException("field", "value", "message") instanceof CustomException);
            assertTrue(new BusinessLogicException(ErrorCode.USER_EMAIL_EXISTS, "rule") instanceof CustomException);
            assertTrue(new AuthenticationException("reason", "action") instanceof CustomException);
            assertTrue(new AuthorizationException("resource", "permission") instanceof CustomException);
            assertTrue(new SystemException("component", "message") instanceof CustomException);
        }
        
        @Test
        @DisplayName("All exceptions should extend RuntimeException")
        void allExceptionsShouldExtendRuntimeException() {
            // Given & When & Then
            assertTrue(new ValidationException("field", "value", "message") instanceof RuntimeException);
            assertTrue(new BusinessLogicException(ErrorCode.USER_EMAIL_EXISTS, "rule") instanceof RuntimeException);
            assertTrue(new AuthenticationException("reason", "action") instanceof RuntimeException);
            assertTrue(new AuthorizationException("resource", "permission") instanceof RuntimeException);
            assertTrue(new SystemException("component", "message") instanceof RuntimeException);
        }
        
        @Test
        @DisplayName("All exceptions should have non-null error codes")
        void allExceptionsShouldHaveNonNullErrorCodes() {
            // Given & When & Then
            assertNotNull(new ValidationException("field", "value", "message").getErrorCode());
            assertNotNull(new BusinessLogicException(ErrorCode.USER_EMAIL_EXISTS, "rule").getErrorCode());
            assertNotNull(new AuthenticationException("reason", "action").getErrorCode());
            assertNotNull(new AuthorizationException("resource", "permission").getErrorCode());
            assertNotNull(new SystemException("component", "message").getErrorCode());
        }
        
        @Test
        @DisplayName("All exceptions should have non-null messages")
        void allExceptionsShouldHaveNonNullMessages() {
            // Given & When & Then
            assertNotNull(new ValidationException("field", "value", "message").getMessage());
            assertNotNull(new BusinessLogicException(ErrorCode.USER_EMAIL_EXISTS, "rule").getMessage());
            assertNotNull(new AuthenticationException("reason", "action").getMessage());
            assertNotNull(new AuthorizationException("resource", "permission").getMessage());
            assertNotNull(new SystemException("component", "message").getMessage());
        }
        
        @Test
        @DisplayName("All exceptions should have empty context by default")
        void allExceptionsShouldHaveEmptyContextByDefault() {
            // Given & When & Then
            assertTrue(new ValidationException("field", "value", "message").getContext().isEmpty());
            assertTrue(new BusinessLogicException(ErrorCode.USER_EMAIL_EXISTS, "rule").getContext().isEmpty());
            assertTrue(new AuthenticationException("reason", "action").getContext().isEmpty());
            assertTrue(new AuthorizationException("resource", "permission").getContext().isEmpty());
            assertTrue(new SystemException("component", "message").getContext().isEmpty());
        }
        
        @Test
        @DisplayName("All exceptions should support context chaining")
        void allExceptionsShouldSupportContextChaining() {
            // Given
            ValidationException validationEx = new ValidationException("field", "value", "message");
            BusinessLogicException businessEx = new BusinessLogicException(ErrorCode.USER_EMAIL_EXISTS, "rule");
            
            // When
            CustomException result1 = validationEx.addContext("key1", "value1").addContext("key2", "value2");
            CustomException result2 = businessEx.addContext("key1", "value1").addContext("key2", "value2");
            
            // Then
            assertSame(validationEx, result1);
            assertSame(businessEx, result2);
            assertEquals(2, validationEx.getContext().size());
            assertEquals(2, businessEx.getContext().size());
        }
    }
}
