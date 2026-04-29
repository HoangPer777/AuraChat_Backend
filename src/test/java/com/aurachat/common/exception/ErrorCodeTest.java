package com.aurachat.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorCode enumeration.
 * Tests enum methods, mappings, code uniqueness, and error code retrieval.
 */
class ErrorCodeTest {
    
    @Test
    void testGetCode() {
        // Given
        ErrorCode errorCode = ErrorCode.AUTH_INVALID_CREDENTIALS;
        
        // When
        String code = errorCode.getCode();
        
        // Then
        assertEquals("AUTH_001", code);
    }
    
    @Test
    void testGetDefaultMessage() {
        // Given
        ErrorCode errorCode = ErrorCode.AUTH_INVALID_CREDENTIALS;
        
        // When
        String message = errorCode.getDefaultMessage();
        
        // Then
        assertEquals("Invalid username or password", message);
    }
    
    @Test
    void testGetHttpStatus() {
        // Given
        ErrorCode errorCode = ErrorCode.AUTH_INVALID_CREDENTIALS;
        
        // When
        HttpStatus status = errorCode.getHttpStatus();
        
        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, status);
    }
    
    @Test
    void testFromCodeWithValidCode() {
        // Given
        String code = "AUTH_001";
        
        // When
        ErrorCode result = ErrorCode.fromCode(code);
        
        // Then
        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, result);
    }
    
    @Test
    void testFromCodeWithInvalidCode() {
        // Given
        String invalidCode = "INVALID_999";
        
        // When
        ErrorCode result = ErrorCode.fromCode(invalidCode);
        
        // Then
        assertEquals(ErrorCode.SYSTEM_ERROR, result);
    }
    
    @Test
    void testFromCodeWithNullCode() {
        // Given
        String nullCode = null;
        
        // When
        ErrorCode result = ErrorCode.fromCode(nullCode);
        
        // Then
        assertEquals(ErrorCode.SYSTEM_ERROR, result);
    }
    
    @Test
    void testFromCodeWithEmptyCode() {
        // Given
        String emptyCode = "";
        
        // When
        ErrorCode result = ErrorCode.fromCode(emptyCode);
        
        // Then
        assertEquals(ErrorCode.SYSTEM_ERROR, result);
    }
    
    @Test
    void testAllErrorCodesHaveUniqueCode() {
        // Given
        ErrorCode[] allErrorCodes = ErrorCode.values();
        
        // When
        Set<String> codes = Arrays.stream(allErrorCodes)
                .map(ErrorCode::getCode)
                .collect(Collectors.toSet());
        
        // Then
        assertEquals(allErrorCodes.length, codes.size(), 
                "All error codes should have unique code values");
    }
    
    @Test
    void testAllErrorCodesHaveNonNullCode() {
        // Given & When & Then
        Arrays.stream(ErrorCode.values()).forEach(errorCode -> {
            assertNotNull(errorCode.getCode(), 
                    errorCode.name() + " should have non-null code");
            assertFalse(errorCode.getCode().isEmpty(), 
                    errorCode.name() + " should have non-empty code");
        });
    }
    
    @Test
    void testAllErrorCodesHaveNonNullDefaultMessage() {
        // Given & When & Then
        Arrays.stream(ErrorCode.values()).forEach(errorCode -> {
            assertNotNull(errorCode.getDefaultMessage(), 
                    errorCode.name() + " should have non-null default message");
            assertFalse(errorCode.getDefaultMessage().isEmpty(), 
                    errorCode.name() + " should have non-empty default message");
        });
    }
    
    @Test
    void testAllErrorCodesHaveNonNullHttpStatus() {
        // Given & When & Then
        Arrays.stream(ErrorCode.values()).forEach(errorCode -> {
            assertNotNull(errorCode.getHttpStatus(), 
                    errorCode.name() + " should have non-null HTTP status");
        });
    }
    
    @Test
    void testAuthenticationErrorsHaveUnauthorizedStatus() {
        // Given
        ErrorCode[] authErrors = {
            ErrorCode.AUTH_INVALID_CREDENTIALS,
            ErrorCode.AUTH_TOKEN_EXPIRED,
            ErrorCode.AUTH_TOKEN_INVALID,
            ErrorCode.AUTH_ACCOUNT_LOCKED,
            ErrorCode.AUTH_FAILED
        };
        
        // When & Then
        Arrays.stream(authErrors).forEach(errorCode -> {
            assertEquals(HttpStatus.UNAUTHORIZED, errorCode.getHttpStatus(),
                    errorCode.name() + " should have UNAUTHORIZED status");
        });
    }
    
    @Test
    void testAuthorizationErrorsHaveForbiddenStatus() {
        // Given
        ErrorCode[] authzErrors = {
            ErrorCode.ACCESS_DENIED,
            ErrorCode.MESSAGE_UNAUTHORIZED_ACCESS
        };
        
        // When & Then
        Arrays.stream(authzErrors).forEach(errorCode -> {
            assertEquals(HttpStatus.FORBIDDEN, errorCode.getHttpStatus(),
                    errorCode.name() + " should have FORBIDDEN status");
        });
    }
    
    @Test
    void testValidationErrorsHaveBadRequestStatus() {
        // Given
        ErrorCode[] validationErrors = {
            ErrorCode.VALIDATION_FAILED,
            ErrorCode.VALIDATION_REQUIRED_FIELD,
            ErrorCode.VALIDATION_INVALID_FORMAT,
            ErrorCode.VALIDATION_VALUE_TOO_LONG,
            ErrorCode.MEDIA_INVALID_TYPE,
            ErrorCode.MEDIA_SIZE_EXCEEDED
        };
        
        // When & Then
        Arrays.stream(validationErrors).forEach(errorCode -> {
            assertEquals(HttpStatus.BAD_REQUEST, errorCode.getHttpStatus(),
                    errorCode.name() + " should have BAD_REQUEST status");
        });
    }
    
    @Test
    void testNotFoundErrorsHaveNotFoundStatus() {
        // Given
        ErrorCode[] notFoundErrors = {
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.FRIEND_REQUEST_NOT_FOUND,
            ErrorCode.MESSAGE_CONVERSATION_NOT_FOUND
        };
        
        // When & Then
        Arrays.stream(notFoundErrors).forEach(errorCode -> {
            assertEquals(HttpStatus.NOT_FOUND, errorCode.getHttpStatus(),
                    errorCode.name() + " should have NOT_FOUND status");
        });
    }
    
    @Test
    void testBusinessLogicErrorsHaveUnprocessableEntityStatus() {
        // Given
        ErrorCode[] businessErrors = {
            ErrorCode.USER_EMAIL_EXISTS,
            ErrorCode.USER_PROFILE_UPDATE_FAILED,
            ErrorCode.FRIEND_REQUEST_EXISTS,
            ErrorCode.FRIEND_ALREADY_EXISTS,
            ErrorCode.FRIEND_SELF_REQUEST,
            ErrorCode.MESSAGE_SEND_FAILED,
            ErrorCode.CALL_USER_BUSY,
            ErrorCode.CALL_CONNECTION_FAILED,
            ErrorCode.MEDIA_UPLOAD_FAILED
        };
        
        // When & Then
        Arrays.stream(businessErrors).forEach(errorCode -> {
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, errorCode.getHttpStatus(),
                    errorCode.name() + " should have UNPROCESSABLE_ENTITY status");
        });
    }
    
    @Test
    void testSystemErrorsHaveInternalServerErrorStatus() {
        // Given
        ErrorCode[] systemErrors = {
            ErrorCode.SYSTEM_ERROR,
            ErrorCode.DATABASE_ERROR,
            ErrorCode.CONFIGURATION_ERROR
        };
        
        // When & Then
        Arrays.stream(systemErrors).forEach(errorCode -> {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorCode.getHttpStatus(),
                    errorCode.name() + " should have INTERNAL_SERVER_ERROR status");
        });
    }
    
    @Test
    void testExternalServiceErrorHasServiceUnavailableStatus() {
        // Given
        ErrorCode errorCode = ErrorCode.EXTERNAL_SERVICE_ERROR;
        
        // When
        HttpStatus status = errorCode.getHttpStatus();
        
        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, status);
    }
    
    @Test
    void testErrorCodeNamingConventionForAuthModule() {
        // Given
        ErrorCode[] authErrors = {
            ErrorCode.AUTH_INVALID_CREDENTIALS,
            ErrorCode.AUTH_TOKEN_EXPIRED,
            ErrorCode.AUTH_TOKEN_INVALID,
            ErrorCode.AUTH_ACCOUNT_LOCKED,
            ErrorCode.ACCESS_DENIED,
            ErrorCode.AUTH_FAILED
        };
        
        // When & Then
        Arrays.stream(authErrors).forEach(errorCode -> {
            assertTrue(errorCode.getCode().startsWith("AUTH_"),
                    errorCode.name() + " should have code starting with AUTH_");
        });
    }
    
    @Test
    void testErrorCodeNamingConventionForValidationModule() {
        // Given
        ErrorCode[] validationErrors = {
            ErrorCode.VALIDATION_FAILED,
            ErrorCode.VALIDATION_REQUIRED_FIELD,
            ErrorCode.VALIDATION_INVALID_FORMAT,
            ErrorCode.VALIDATION_VALUE_TOO_LONG
        };
        
        // When & Then
        Arrays.stream(validationErrors).forEach(errorCode -> {
            assertTrue(errorCode.getCode().startsWith("VAL_"),
                    errorCode.name() + " should have code starting with VAL_");
        });
    }
    
    @Test
    void testErrorCodeNamingConventionForUserModule() {
        // Given
        ErrorCode[] userErrors = {
            ErrorCode.USER_EMAIL_EXISTS,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.USER_PROFILE_UPDATE_FAILED
        };
        
        // When & Then
        Arrays.stream(userErrors).forEach(errorCode -> {
            assertTrue(errorCode.getCode().startsWith("USER_"),
                    errorCode.name() + " should have code starting with USER_");
        });
    }
    
    @Test
    void testErrorCodeNamingConventionForFriendModule() {
        // Given
        ErrorCode[] friendErrors = {
            ErrorCode.FRIEND_REQUEST_EXISTS,
            ErrorCode.FRIEND_REQUEST_NOT_FOUND,
            ErrorCode.FRIEND_ALREADY_EXISTS,
            ErrorCode.FRIEND_SELF_REQUEST
        };
        
        // When & Then
        Arrays.stream(friendErrors).forEach(errorCode -> {
            assertTrue(errorCode.getCode().startsWith("FRIEND_"),
                    errorCode.name() + " should have code starting with FRIEND_");
        });
    }
    
    @Test
    void testErrorCodeNamingConventionForMessageModule() {
        // Given
        ErrorCode[] messageErrors = {
            ErrorCode.MESSAGE_CONVERSATION_NOT_FOUND,
            ErrorCode.MESSAGE_SEND_FAILED,
            ErrorCode.MESSAGE_UNAUTHORIZED_ACCESS
        };
        
        // When & Then
        Arrays.stream(messageErrors).forEach(errorCode -> {
            assertTrue(errorCode.getCode().startsWith("MSG_"),
                    errorCode.name() + " should have code starting with MSG_");
        });
    }
    
    @Test
    void testErrorCodeNamingConventionForCallModule() {
        // Given
        ErrorCode[] callErrors = {
            ErrorCode.CALL_USER_BUSY,
            ErrorCode.CALL_CONNECTION_FAILED
        };
        
        // When & Then
        Arrays.stream(callErrors).forEach(errorCode -> {
            assertTrue(errorCode.getCode().startsWith("CALL_"),
                    errorCode.name() + " should have code starting with CALL_");
        });
    }
    
    @Test
    void testErrorCodeNamingConventionForMediaModule() {
        // Given
        ErrorCode[] mediaErrors = {
            ErrorCode.MEDIA_UPLOAD_FAILED,
            ErrorCode.MEDIA_INVALID_TYPE,
            ErrorCode.MEDIA_SIZE_EXCEEDED
        };
        
        // When & Then
        Arrays.stream(mediaErrors).forEach(errorCode -> {
            assertTrue(errorCode.getCode().startsWith("MEDIA_"),
                    errorCode.name() + " should have code starting with MEDIA_");
        });
    }
    
    @Test
    void testErrorCodeNamingConventionForSystemModule() {
        // Given
        ErrorCode[] systemErrors = {
            ErrorCode.SYSTEM_ERROR,
            ErrorCode.DATABASE_ERROR,
            ErrorCode.EXTERNAL_SERVICE_ERROR,
            ErrorCode.CONFIGURATION_ERROR
        };
        
        // When & Then
        Arrays.stream(systemErrors).forEach(errorCode -> {
            assertTrue(errorCode.getCode().startsWith("SYS_"),
                    errorCode.name() + " should have code starting with SYS_");
        });
    }
    
    @Test
    void testFromCodeForAllDefinedErrorCodes() {
        // Given & When & Then
        Arrays.stream(ErrorCode.values()).forEach(errorCode -> {
            ErrorCode result = ErrorCode.fromCode(errorCode.getCode());
            assertEquals(errorCode, result,
                    "fromCode should return correct ErrorCode for " + errorCode.name());
        });
    }
    
    @Test
    void testErrorCodeEnumHasExpectedNumberOfValues() {
        // Given
        ErrorCode[] allErrorCodes = ErrorCode.values();
        
        // When
        int count = allErrorCodes.length;
        
        // Then
        // Based on the design document, we should have:
        // 6 Auth errors + 4 Validation errors + 3 User errors + 4 Friend errors
        // + 3 Message errors + 2 Call errors + 3 Media errors + 4 System errors = 29
        assertEquals(29, count, "Should have 29 error codes defined");
    }
    
    @Test
    void testErrorCodeModuleGrouping() {
        // Given
        Set<String> expectedPrefixes = new HashSet<>(Arrays.asList(
            "AUTH_", "VAL_", "USER_", "FRIEND_", "MSG_", "CALL_", "MEDIA_", "SYS_"
        ));
        
        // When
        Set<String> actualPrefixes = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .map(code -> code.substring(0, code.indexOf('_') + 1))
                .collect(Collectors.toSet());
        
        // Then
        assertEquals(expectedPrefixes, actualPrefixes,
                "Error codes should be grouped by expected module prefixes");
    }
}
