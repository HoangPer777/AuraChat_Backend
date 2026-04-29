package com.aurachat.common.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorResponse class.
 * Tests builder methods, static factory methods, and field validation.
 */
class ErrorResponseTest {
    
    @Test
    void testBuilderWithAllFields() {
        // Given
        String message = "Error occurred";
        String errorCode = "ERR_001";
        String errorId = "ERR_20241201_123456_ABC";
        Map<String, Object> details = new HashMap<>();
        details.put("field", "value");
        LocalDateTime timestamp = LocalDateTime.now();
        
        // When
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorId(errorId)
                .details(details)
                .timestamp(timestamp)
                .build();
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(errorId, response.getErrorId());
        assertEquals(details, response.getDetails());
        assertEquals(timestamp, response.getTimestamp());
    }
    
    @Test
    void testErrorWithCodeMessageAndId() {
        // Given
        String errorCode = "AUTH_001";
        String message = "Authentication failed";
        String errorId = "ERR_20241201_123456_XYZ";
        
        // When
        ErrorResponse response = ErrorResponse.error(errorCode, message, errorId);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(errorId, response.getErrorId());
        assertNull(response.getDetails());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithCodeMessageIdAndDetails() {
        // Given
        String errorCode = "VAL_001";
        String message = "Validation failed";
        String errorId = "ERR_20241201_123456_VAL";
        Map<String, Object> details = new HashMap<>();
        details.put("email", "Invalid email format");
        details.put("password", "Password too short");
        
        // When
        ErrorResponse response = ErrorResponse.error(errorCode, message, errorId, details);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(errorId, response.getErrorId());
        assertEquals(details, response.getDetails());
        assertEquals(2, response.getDetails().size());
        assertEquals("Invalid email format", response.getDetails().get("email"));
        assertEquals("Password too short", response.getDetails().get("password"));
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithCodeAndMessageOnly() {
        // Given
        String errorCode = "SYS_001";
        String message = "System error";
        
        // When
        ErrorResponse response = ErrorResponse.error(errorCode, message);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertNull(response.getErrorId());
        assertNull(response.getDetails());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithNullDetails() {
        // Given
        String errorCode = "ERR_001";
        String message = "Error message";
        String errorId = "ERR_ID_123";
        
        // When
        ErrorResponse response = ErrorResponse.error(errorCode, message, errorId, null);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(errorId, response.getErrorId());
        assertNull(response.getDetails());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithEmptyDetails() {
        // Given
        String errorCode = "ERR_001";
        String message = "Error message";
        String errorId = "ERR_ID_123";
        Map<String, Object> details = new HashMap<>();
        
        // When
        ErrorResponse response = ErrorResponse.error(errorCode, message, errorId, details);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(errorId, response.getErrorId());
        assertNotNull(response.getDetails());
        assertTrue(response.getDetails().isEmpty());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithComplexDetails() {
        // Given
        String errorCode = "VAL_002";
        String message = "Multiple validation errors";
        String errorId = "ERR_VAL_456";
        Map<String, Object> details = new HashMap<>();
        details.put("username", "Username already exists");
        details.put("age", 25);
        details.put("active", true);
        Map<String, String> nested = new HashMap<>();
        nested.put("street", "Invalid street name");
        details.put("address", nested);
        
        // When
        ErrorResponse response = ErrorResponse.error(errorCode, message, errorId, details);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals(4, response.getDetails().size());
        assertEquals("Username already exists", response.getDetails().get("username"));
        assertEquals(25, response.getDetails().get("age"));
        assertEquals(true, response.getDetails().get("active"));
        assertTrue(response.getDetails().get("address") instanceof Map);
    }
    
    @Test
    void testTimestampIsRecent() {
        // Given
        LocalDateTime before = LocalDateTime.now();
        
        // When
        ErrorResponse response = ErrorResponse.error("ERR_001", "Error message", "ERR_ID");
        
        // Then
        LocalDateTime after = LocalDateTime.now();
        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp().isAfter(before.minusSeconds(1)));
        assertTrue(response.getTimestamp().isBefore(after.plusSeconds(1)));
    }
    
    @Test
    void testNoArgsConstructor() {
        // When
        ErrorResponse response = new ErrorResponse();
        
        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess()); // default boolean value is false
        assertNull(response.getMessage());
        assertNull(response.getErrorCode());
        assertNull(response.getErrorId());
        assertNull(response.getDetails());
        assertNull(response.getTimestamp());
    }
    
    @Test
    void testAllArgsConstructor() {
        // Given
        boolean success = false;
        String message = "Error message";
        String errorCode = "ERR_001";
        String errorId = "ERR_ID_123";
        Map<String, Object> details = new HashMap<>();
        details.put("field", "value");
        LocalDateTime timestamp = LocalDateTime.now();
        
        // When
        ErrorResponse response = new ErrorResponse(success, message, errorCode, errorId, details, timestamp);
        
        // Then
        assertEquals(success, response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(errorId, response.getErrorId());
        assertEquals(details, response.getDetails());
        assertEquals(timestamp, response.getTimestamp());
    }
    
    @Test
    void testSettersAndGetters() {
        // Given
        ErrorResponse response = new ErrorResponse();
        String message = "Updated error";
        String errorCode = "ERR_002";
        String errorId = "ERR_ID_456";
        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");
        LocalDateTime timestamp = LocalDateTime.now();
        
        // When
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorCode(errorCode);
        response.setErrorId(errorId);
        response.setDetails(details);
        response.setTimestamp(timestamp);
        
        // Then
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(errorId, response.getErrorId());
        assertEquals(details, response.getDetails());
        assertEquals(timestamp, response.getTimestamp());
    }
    
    @Test
    void testErrorCodeFormats() {
        // Test different error code formats
        String[] errorCodes = {
            "AUTH_001",
            "VAL_002",
            "USER_003",
            "SYS_001",
            "FRIEND_001",
            "MSG_001"
        };
        
        for (String errorCode : errorCodes) {
            // When
            ErrorResponse response = ErrorResponse.error(errorCode, "Test message");
            
            // Then
            assertEquals(errorCode, response.getErrorCode());
            assertFalse(response.isSuccess());
        }
    }
    
    @Test
    void testErrorIdFormats() {
        // Test different error ID formats
        String[] errorIds = {
            "ERR_20241201_123456_ABC",
            "ERR_20241201_123456_XYZ",
            "ERR_ID_123",
            "ERROR_TRACKING_456"
        };
        
        for (String errorId : errorIds) {
            // When
            ErrorResponse response = ErrorResponse.error("ERR_001", "Test message", errorId);
            
            // Then
            assertEquals(errorId, response.getErrorId());
            assertFalse(response.isSuccess());
        }
    }
    
    @Test
    void testMultipleInstancesHaveDifferentTimestamps() throws InterruptedException {
        // Given & When
        ErrorResponse response1 = ErrorResponse.error("ERR_001", "Error 1", "ID_1");
        Thread.sleep(10); // Small delay to ensure different timestamps
        ErrorResponse response2 = ErrorResponse.error("ERR_002", "Error 2", "ID_2");
        
        // Then
        assertNotEquals(response1.getTimestamp(), response2.getTimestamp());
        assertTrue(response2.getTimestamp().isAfter(response1.getTimestamp()));
    }
    
    @Test
    void testSuccessFieldIsAlwaysFalseForErrorResponses() {
        // Test all factory methods set success to false
        ErrorResponse response1 = ErrorResponse.error("ERR_001", "Message");
        ErrorResponse response2 = ErrorResponse.error("ERR_001", "Message", "ID");
        ErrorResponse response3 = ErrorResponse.error("ERR_001", "Message", "ID", new HashMap<>());
        
        assertFalse(response1.isSuccess());
        assertFalse(response2.isSuccess());
        assertFalse(response3.isSuccess());
    }
    
    @Test
    void testDetailsCanBeModifiedAfterCreation() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("field1", "value1");
        ErrorResponse response = ErrorResponse.error("ERR_001", "Message", "ID", details);
        
        // When
        response.getDetails().put("field2", "value2");
        
        // Then
        assertEquals(2, response.getDetails().size());
        assertEquals("value1", response.getDetails().get("field1"));
        assertEquals("value2", response.getDetails().get("field2"));
    }
}
