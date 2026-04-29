package com.aurachat.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomException abstract class.
 * Tests constructor behavior, getter methods, context management, and toString implementation.
 */
class CustomExceptionTest {
    
    /**
     * Concrete implementation of CustomException for testing purposes
     */
    private static class TestException extends CustomException {
        public TestException(String errorCode, String message) {
            super(errorCode, message);
        }
        
        public TestException(String errorCode, String message, Throwable cause) {
            super(errorCode, message, cause);
        }
    }
    
    @Test
    void testConstructorWithErrorCodeAndMessage() {
        // Given
        String errorCode = "TEST_001";
        String message = "Test exception message";
        
        // When
        TestException exception = new TestException(errorCode, message);
        
        // Then
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
        assertNotNull(exception.getContext());
        assertTrue(exception.getContext().isEmpty());
        assertNull(exception.getCause());
    }
    
    @Test
    void testConstructorWithErrorCodeMessageAndCause() {
        // Given
        String errorCode = "TEST_002";
        String message = "Test exception with cause";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        TestException exception = new TestException(errorCode, message, cause);
        
        // Then
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
        assertNotNull(exception.getContext());
        assertTrue(exception.getContext().isEmpty());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    void testGetErrorCode() {
        // Given
        String errorCode = "TEST_003";
        TestException exception = new TestException(errorCode, "Test message");
        
        // When
        String result = exception.getErrorCode();
        
        // Then
        assertEquals(errorCode, result);
    }
    
    @Test
    void testGetMessage() {
        // Given
        String message = "Test exception message";
        TestException exception = new TestException("TEST_004", message);
        
        // When
        String result = exception.getMessage();
        
        // Then
        assertEquals(message, result);
    }
    
    @Test
    void testGetContextInitiallyEmpty() {
        // Given
        TestException exception = new TestException("TEST_005", "Test message");
        
        // When
        var context = exception.getContext();
        
        // Then
        assertNotNull(context);
        assertTrue(context.isEmpty());
    }
    
    @Test
    void testAddContextSingleEntry() {
        // Given
        TestException exception = new TestException("TEST_006", "Test message");
        String key = "userId";
        String value = "user123";
        
        // When
        CustomException result = exception.addContext(key, value);
        
        // Then
        assertSame(exception, result); // Verify method chaining
        assertEquals(1, exception.getContext().size());
        assertEquals(value, exception.getContext().get(key));
    }
    
    @Test
    void testAddContextMultipleEntries() {
        // Given
        TestException exception = new TestException("TEST_007", "Test message");
        
        // When
        exception.addContext("userId", "user123")
                 .addContext("action", "login")
                 .addContext("timestamp", 1234567890L);
        
        // Then
        assertEquals(3, exception.getContext().size());
        assertEquals("user123", exception.getContext().get("userId"));
        assertEquals("login", exception.getContext().get("action"));
        assertEquals(1234567890L, exception.getContext().get("timestamp"));
    }
    
    @Test
    void testAddContextWithNullValue() {
        // Given
        TestException exception = new TestException("TEST_008", "Test message");
        
        // When
        exception.addContext("nullKey", null);
        
        // Then
        assertEquals(1, exception.getContext().size());
        assertTrue(exception.getContext().containsKey("nullKey"));
        assertNull(exception.getContext().get("nullKey"));
    }
    
    @Test
    void testAddContextOverwriteExistingKey() {
        // Given
        TestException exception = new TestException("TEST_009", "Test message");
        exception.addContext("key", "value1");
        
        // When
        exception.addContext("key", "value2");
        
        // Then
        assertEquals(1, exception.getContext().size());
        assertEquals("value2", exception.getContext().get("key"));
    }
    
    @Test
    void testToStringWithoutContext() {
        // Given
        TestException exception = new TestException("TEST_010", "Test message");
        
        // When
        String result = exception.toString();
        
        // Then
        assertTrue(result.contains("TestException"));
        assertTrue(result.contains("errorCode=TEST_010"));
        assertTrue(result.contains("message=Test message"));
        assertTrue(result.contains("context={}"));
    }
    
    @Test
    void testToStringWithContext() {
        // Given
        TestException exception = new TestException("TEST_011", "Test message");
        exception.addContext("userId", "user123")
                 .addContext("action", "login");
        
        // When
        String result = exception.toString();
        
        // Then
        assertTrue(result.contains("TestException"));
        assertTrue(result.contains("errorCode=TEST_011"));
        assertTrue(result.contains("message=Test message"));
        assertTrue(result.contains("userId"));
        assertTrue(result.contains("user123"));
        assertTrue(result.contains("action"));
        assertTrue(result.contains("login"));
    }
    
    @Test
    void testExceptionIsRuntimeException() {
        // Given
        TestException exception = new TestException("TEST_012", "Test message");
        
        // Then
        assertTrue(exception instanceof RuntimeException);
    }
    
    @Test
    void testExceptionCanBeThrown() {
        // Given
        String errorCode = "TEST_013";
        String message = "Test throwable exception";
        
        // When & Then
        assertThrows(TestException.class, () -> {
            throw new TestException(errorCode, message);
        });
    }
    
    @Test
    void testExceptionWithCausePreservesCauseMessage() {
        // Given
        String causeMessage = "Root cause message";
        Throwable cause = new IllegalArgumentException(causeMessage);
        TestException exception = new TestException("TEST_014", "Test message", cause);
        
        // When
        Throwable actualCause = exception.getCause();
        
        // Then
        assertNotNull(actualCause);
        assertEquals(causeMessage, actualCause.getMessage());
        assertTrue(actualCause instanceof IllegalArgumentException);
    }
    
    @Test
    void testContextIsModifiable() {
        // Given
        TestException exception = new TestException("TEST_015", "Test message");
        exception.addContext("key1", "value1");
        
        // When
        exception.getContext().put("key2", "value2");
        
        // Then
        assertEquals(2, exception.getContext().size());
        assertEquals("value1", exception.getContext().get("key1"));
        assertEquals("value2", exception.getContext().get("key2"));
    }
}
