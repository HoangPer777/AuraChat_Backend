package com.aurachat.common.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataResponse class.
 * Tests builder methods, static factory methods, and field validation.
 */
class DataResponseTest {
    
    @Test
    void testBuilderWithAllFields() {
        // Given
        String message = "Test message";
        String data = "Test data";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // When
        DataResponse<String> response = DataResponse.<String>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(timestamp)
                .build();
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertEquals(timestamp, response.getTimestamp());
    }
    
    @Test
    void testSuccessWithDataAndMessage() {
        // Given
        String data = "Test data";
        String message = "Operation successful";
        
        // When
        DataResponse<String> response = DataResponse.success(data, message);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithDataOnly() {
        // Given
        List<String> data = List.of("Test data");
        
        // When
        DataResponse<List<String>> response = DataResponse.success(data);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("Operation completed successfully", response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithMessageOnly() {
        // Given
        String message = "Operation completed";
        
        // When
        DataResponse<String> response = DataResponse.success(message);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithNullData() {
        // Given
        String message = "No data available";
        
        // When
        DataResponse<String> response = DataResponse.success(null, message);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithComplexObject() {
        // Given
        TestUser user = new TestUser("John", "john@example.com");
        String message = "User retrieved";
        
        // When
        DataResponse<TestUser> response = DataResponse.success(user, message);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(user, response.getData());
        assertEquals("John", response.getData().getName());
        assertEquals("john@example.com", response.getData().getEmail());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithCollection() {
        // Given
        List<String> items = Arrays.asList("Item1", "Item2", "Item3");
        String message = "Items retrieved";
        
        // When
        DataResponse<List<String>> response = DataResponse.success(items, message);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(items, response.getData());
        assertEquals(3, response.getData().size());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testTimestampIsRecent() {
        // Given
        LocalDateTime before = LocalDateTime.now();
        
        // When
        DataResponse<String> response = DataResponse.success("data", "message");
        
        // Then
        LocalDateTime after = LocalDateTime.now();
        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp().isAfter(before.minusSeconds(1)));
        assertTrue(response.getTimestamp().isBefore(after.plusSeconds(1)));
    }
    
    @Test
    void testNoArgsConstructor() {
        // When
        DataResponse<String> response = new DataResponse<>();
        
        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess()); // default boolean value is false
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNull(response.getTimestamp());
    }
    
    @Test
    void testAllArgsConstructor() {
        // Given
        boolean success = true;
        String message = "Test message";
        String data = "Test data";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // When
        DataResponse<String> response = new DataResponse<>(success, message, data, timestamp);
        
        // Then
        assertEquals(success, response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertEquals(timestamp, response.getTimestamp());
    }
    
    @Test
    void testSettersAndGetters() {
        // Given
        DataResponse<String> response = new DataResponse<>();
        String message = "Updated message";
        String data = "Updated data";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // When
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(timestamp);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertEquals(timestamp, response.getTimestamp());
    }
    
    @Test
    void testGenericTypeWithInteger() {
        // Given
        Integer data = 42;
        String message = "Number retrieved";
        
        // When
        DataResponse<Integer> response = DataResponse.success(data, message);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertEquals(42, response.getData());
    }
    
    @Test
    void testGenericTypeWithBoolean() {
        // Given
        Boolean data = true;
        String message = "Boolean value";
        
        // When
        DataResponse<Boolean> response = DataResponse.success(data, message);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertTrue(response.getData());
    }
    
    @Test
    void testMultipleInstancesHaveDifferentTimestamps() throws InterruptedException {
        // Given & When
        DataResponse<String> response1 = DataResponse.success("data1", "message1");
        Thread.sleep(10); // Small delay to ensure different timestamps
        DataResponse<String> response2 = DataResponse.success("data2", "message2");
        
        // Then
        assertNotEquals(response1.getTimestamp(), response2.getTimestamp());
        assertTrue(response2.getTimestamp().isAfter(response1.getTimestamp()));
    }
    
    // Helper class for testing complex objects
    private static class TestUser {
        private final String name;
        private final String email;
        
        public TestUser(String name, String email) {
            this.name = name;
            this.email = email;
        }
        
        public String getName() {
            return name;
        }
        
        public String getEmail() {
            return email;
        }
    }
}
