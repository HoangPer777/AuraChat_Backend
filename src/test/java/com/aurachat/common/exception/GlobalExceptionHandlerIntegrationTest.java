package com.aurachat.common.exception;

import com.aurachat.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GlobalExceptionHandler.
 * Tests the complete exception handling flow from controller to response,
 * including HTTP status code mapping, error ID generation, and logging functionality.
 * 
 * Requirements tested: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Test controller that throws various exceptions for testing purposes
     */
    @RestController
    @RequestMapping("/test-exceptions")
    static class TestExceptionController {
        
        @GetMapping("/validation")
        public String throwValidationException() {
            throw new ValidationException("email", "invalid@", "Email format is invalid");
        }
        
        @PostMapping("/validation-bean")
        public String validateBean(@Valid @RequestBody TestRequest request) {
            return "success";
        }
        
        @GetMapping("/authentication")
        public String throwAuthenticationException() {
            throw new AuthenticationException("Invalid credentials", "login");
        }
        
        @GetMapping("/authorization")
        public String throwAuthorizationException() {
            throw new AuthorizationException("/admin/users", "ADMIN");
        }
        
        @GetMapping("/access-denied")
        public String throwAccessDeniedException() {
            throw new AccessDeniedException("Access denied");
        }
        
        @GetMapping("/business-logic")
        public String throwBusinessLogicException() {
            throw new BusinessLogicException(ErrorCode.USER_EMAIL_EXISTS, "email_uniqueness");
        }
        
        @GetMapping("/system")
        public String throwSystemException() {
            throw new SystemException("database", "Connection failed", new RuntimeException("DB error"));
        }
        
        @GetMapping("/generic")
        public String throwGenericException() {
            throw new RuntimeException("Unexpected error");
        }
    }
    
    /**
     * Test request DTO for validation testing
     */
    static class TestRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        private String email;
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    /**
     * Requirement 1.2: Test ValidationException handling
     * WHEN a Validation_Exception occurs, THE Global_Exception_Handler SHALL return 
     * HTTP status 400 with detailed validation error messages
     */
    @Test
    void testValidationExceptionReturnsHttp400() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/test-exceptions/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify response structure
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertFalse(response.isSuccess());
        assertNotNull(response.getMessage());
        assertNotNull(response.getErrorCode());
        assertNotNull(response.getErrorId());
        assertTrue(response.getErrorId().startsWith("ERR_"));
        assertNotNull(response.getTimestamp());
        
        // Verify validation details
        assertNotNull(response.getDetails());
        assertTrue(response.getDetails().containsKey("email"));
    }
    
    /**
     * Requirement 1.2: Test Spring validation (@Valid) exception handling
     * WHEN a Validation_Exception occurs, THE Global_Exception_Handler SHALL return 
     * HTTP status 400 with detailed validation error messages
     */
    @Test
    void testMethodArgumentNotValidExceptionReturnsHttp400() throws Exception {
        // Given
        TestRequest invalidRequest = new TestRequest();
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("short");
        
        // When & Then
        MvcResult result = mockMvc.perform(post("/test-exceptions/validation-bean")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify response structure
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertFalse(response.isSuccess());
        assertEquals("Validation failed", response.getMessage());
        assertEquals(ErrorCode.VALIDATION_FAILED.getCode(), response.getErrorCode());
        assertNotNull(response.getErrorId());
        assertTrue(response.getErrorId().startsWith("ERR_"));
        
        // Verify field-specific validation errors
        assertNotNull(response.getDetails());
        assertTrue(response.getDetails().containsKey("email") || 
                   response.getDetails().containsKey("password"));
    }
    
    /**
     * Requirement 1.3: Test AuthenticationException handling
     * WHEN an Authentication_Exception occurs, THE Global_Exception_Handler SHALL return 
     * HTTP status 401 with authentication error details
     */
    @Test
    void testAuthenticationExceptionReturnsHttp401() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/test-exceptions/authentication"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify response structure
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertFalse(response.isSuccess());
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("Authentication failed") || 
                   response.getMessage().contains("Invalid credentials"));
        assertNotNull(response.getErrorCode());
        assertNotNull(response.getErrorId());
        assertTrue(response.getErrorId().startsWith("ERR_"));
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * Requirement 1.4: Test AuthorizationException handling
     * WHEN an Authorization_Exception occurs, THE Global_Exception_Handler SHALL return 
     * HTTP status 403 with authorization error details
     */
    @Test
    void testAuthorizationExceptionReturnsHttp403() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/test-exceptions/authorization"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify response structure
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertFalse(response.isSuccess());
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("Access denied") || 
                   response.getMessage().contains("/admin/users"));
        assertNotNull(response.getErrorCode());
        assertEquals(ErrorCode.ACCESS_DENIED.getCode(), response.getErrorCode());
        assertNotNull(response.getErrorId());
        assertTrue(response.getErrorId().startsWith("ERR_"));
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * Requirement 1.4: Test Spring Security AccessDeniedException handling
     * WHEN an Authorization_Exception occurs, THE Global_Exception_Handler SHALL return 
     * HTTP status 403 with authorization error details
     */
    @Test
    void testAccessDeniedExceptionReturnsHttp403() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/test-exceptions/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify response structure
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertFalse(response.isSuccess());
        assertEquals("Access denied", response.getMessage());
        assertEquals(ErrorCode.ACCESS_DENIED.getCode(), response.getErrorCode());
        assertNotNull(response.getErrorId());
        assertTrue(response.getErrorId().startsWith("ERR_"));
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * Requirement 1.5: Test BusinessLogicException handling
     * WHEN a Business_Logic_Exception occurs, THE Global_Exception_Handler SHALL return 
     * HTTP status 422 with business logic error details
     */
    @Test
    void testBusinessLogicExceptionReturnsHttp422() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/test-exceptions/business-logic"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify response structure
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertFalse(response.isSuccess());
        assertNotNull(response.getMessage());
        assertEquals(ErrorCode.USER_EMAIL_EXISTS.getCode(), response.getErrorCode());
        assertNotNull(response.getErrorId());
        assertTrue(response.getErrorId().startsWith("ERR_"));
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * Requirement 1.6: Test SystemException handling
     * WHEN a System_Exception occurs, THE Global_Exception_Handler SHALL return 
     * HTTP status 500 with generic error message and log detailed error information
     */
    @Test
    void testSystemExceptionReturnsHttp500() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/test-exceptions/system"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify response structure
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertFalse(response.isSuccess());
        assertEquals("Internal system error", response.getMessage());
        assertNotNull(response.getErrorCode());
        assertNotNull(response.getErrorId());
        assertTrue(response.getErrorId().startsWith("ERR_"));
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * Requirement 1.1, 1.6: Test generic exception handling
     * WHEN any unhandled exception occurs in the application, THE Global_Exception_Handler 
     * SHALL catch the exception and return a standardized Error_Response
     */
    @Test
    void testGenericExceptionReturnsHttp500() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/test-exceptions/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify response structure
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertFalse(response.isSuccess());
        assertEquals("Internal system error", response.getMessage());
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), response.getErrorCode());
        assertNotNull(response.getErrorId());
        assertTrue(response.getErrorId().startsWith("ERR_"));
        assertNotNull(response.getTimestamp());
    }
    
    /**
     * Requirement 1.7: Test error ID generation
     * THE Global_Exception_Handler SHALL include a unique error tracking ID 
     * in each Error_Response for debugging purposes
     */
    @Test
    void testErrorIdGenerationIsUnique() throws Exception {
        // When - Make multiple requests
        MvcResult result1 = mockMvc.perform(get("/test-exceptions/validation"))
                .andExpect(status().isBadRequest())
                .andReturn();
        
        MvcResult result2 = mockMvc.perform(get("/test-exceptions/validation"))
                .andExpect(status().isBadRequest())
                .andReturn();
        
        // Then - Verify error IDs are unique
        ErrorResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        ErrorResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertNotNull(response1.getErrorId());
        assertNotNull(response2.getErrorId());
        assertNotEquals(response1.getErrorId(), response2.getErrorId());
        
        // Verify error ID format: ERR_YYYYMMDD_HHMMSS_UUID
        assertTrue(response1.getErrorId().matches("ERR_\\d{8}_\\d{6}_[A-Z0-9]{8}"));
        assertTrue(response2.getErrorId().matches("ERR_\\d{8}_\\d{6}_[A-Z0-9]{8}"));
    }
    
    /**
     * Requirement 1.7: Test error ID format
     * THE Global_Exception_Handler SHALL include a unique error tracking ID 
     * in each Error_Response for debugging purposes
     */
    @Test
    void testErrorIdFormatIsConsistent() throws Exception {
        // When - Test different exception types
        MvcResult validationResult = mockMvc.perform(get("/test-exceptions/validation"))
                .andReturn();
        MvcResult authResult = mockMvc.perform(get("/test-exceptions/authentication"))
                .andReturn();
        MvcResult businessResult = mockMvc.perform(get("/test-exceptions/business-logic"))
                .andReturn();
        
        // Then - Verify all error IDs follow the same format
        ErrorResponse validationResponse = objectMapper.readValue(
                validationResult.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        ErrorResponse authResponse = objectMapper.readValue(
                authResult.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        ErrorResponse businessResponse = objectMapper.readValue(
                businessResult.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        String errorIdPattern = "ERR_\\d{8}_\\d{6}_[A-Z0-9]{8}";
        assertTrue(validationResponse.getErrorId().matches(errorIdPattern));
        assertTrue(authResponse.getErrorId().matches(errorIdPattern));
        assertTrue(businessResponse.getErrorId().matches(errorIdPattern));
    }
    
    /**
     * Requirement 1.8: Test logging functionality with user context
     * THE Global_Exception_Handler SHALL log all exceptions with appropriate log levels
     */
    @Test
    @WithMockUser(username = "testuser@example.com")
    void testExceptionLoggingWithAuthenticatedUser() throws Exception {
        // When - Trigger exception with authenticated user
        mockMvc.perform(get("/test-exceptions/business-logic"))
                .andExpect(status().isUnprocessableEntity());
        
        // Note: Actual log verification would require a log appender or log capture
        // This test verifies the endpoint works with authenticated user context
        // The GlobalExceptionHandler will log with user ID from SecurityContext
    }
    
    /**
     * Requirement 1.1: Test standardized error response structure
     * WHEN any unhandled exception occurs in the application, THE Global_Exception_Handler 
     * SHALL catch the exception and return a standardized Error_Response
     */
    @Test
    void testErrorResponseStructureIsConsistent() throws Exception {
        // When - Test multiple exception types
        MvcResult[] results = {
                mockMvc.perform(get("/test-exceptions/validation")).andReturn(),
                mockMvc.perform(get("/test-exceptions/authentication")).andReturn(),
                mockMvc.perform(get("/test-exceptions/authorization")).andReturn(),
                mockMvc.perform(get("/test-exceptions/business-logic")).andReturn(),
                mockMvc.perform(get("/test-exceptions/system")).andReturn()
        };
        
        // Then - Verify all responses have consistent structure
        for (MvcResult result : results) {
            ErrorResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), 
                    ErrorResponse.class
            );
            
            // All error responses must have these fields
            assertNotNull(response);
            assertFalse(response.isSuccess());
            assertNotNull(response.getMessage());
            assertNotNull(response.getErrorCode());
            assertNotNull(response.getErrorId());
            assertNotNull(response.getTimestamp());
        }
    }
    
    /**
     * Test HTTP status code mapping for each exception type
     * Verifies that each exception type maps to the correct HTTP status code
     */
    @Test
    void testHttpStatusCodeMappingForAllExceptionTypes() throws Exception {
        // Validation -> 400
        mockMvc.perform(get("/test-exceptions/validation"))
                .andExpect(status().isBadRequest());
        
        // Authentication -> 401
        mockMvc.perform(get("/test-exceptions/authentication"))
                .andExpect(status().isUnauthorized());
        
        // Authorization -> 403
        mockMvc.perform(get("/test-exceptions/authorization"))
                .andExpect(status().isForbidden());
        
        // Business Logic -> 422
        mockMvc.perform(get("/test-exceptions/business-logic"))
                .andExpect(status().isUnprocessableEntity());
        
        // System -> 500
        mockMvc.perform(get("/test-exceptions/system"))
                .andExpect(status().isInternalServerError());
        
        // Generic -> 500
        mockMvc.perform(get("/test-exceptions/generic"))
                .andExpect(status().isInternalServerError());
    }
    
    /**
     * Test that error responses include timestamp in ISO format
     */
    @Test
    void testErrorResponseIncludesTimestamp() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/test-exceptions/validation"))
                .andReturn();
        
        // Then
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertNotNull(response.getTimestamp());
        // Verify timestamp is recent (within last minute)
        assertTrue(response.getTimestamp().isAfter(
                java.time.LocalDateTime.now().minusMinutes(1)
        ));
    }
    
    /**
     * Test that validation errors include field-specific details
     */
    @Test
    void testValidationErrorIncludesFieldDetails() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/test-exceptions/validation"))
                .andReturn();
        
        // Then
        ErrorResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ErrorResponse.class
        );
        
        assertNotNull(response.getDetails());
        assertFalse(response.getDetails().isEmpty());
        assertTrue(response.getDetails().containsKey("email"));
    }
}
