package com.freshmarket.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BusinessException测试类
 */
class BusinessExceptionTest {
    
    @Test
    void shouldCreateBusinessException() {
        // Given
        String code = "TEST_ERROR";
        String message = "Test error message";
        
        // When
        BusinessException exception = new BusinessException(code, message);
        
        // Then
        assertThat(exception.getCode()).isEqualTo(code);
        assertThat(exception.getMessage()).isEqualTo(message);
    }
    
    @Test
    void shouldCreateBusinessExceptionWithCause() {
        // Given
        String code = "TEST_ERROR";
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        BusinessException exception = new BusinessException(code, message, cause);
        
        // Then
        assertThat(exception.getCode()).isEqualTo(code);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
    
    @Test
    void shouldCreateBusinessExceptionUsingOf() {
        // Given
        String message = "Test error message";
        
        // When
        BusinessException exception = BusinessException.of(message);
        
        // Then
        assertThat(exception.getCode()).isEqualTo("BUSINESS_ERROR");
        assertThat(exception.getMessage()).isEqualTo(message);
    }
    
    @Test
    void shouldCreateNotFoundException() {
        // Given
        String message = "Resource not found";
        
        // When
        BusinessException exception = BusinessException.notFound(message);
        
        // Then
        assertThat(exception.getCode()).isEqualTo("NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo(message);
    }
    
    @Test
    void shouldCreateUnauthorizedException() {
        // Given
        String message = "Unauthorized access";
        
        // When
        BusinessException exception = BusinessException.unauthorized(message);
        
        // Then
        assertThat(exception.getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(exception.getMessage()).isEqualTo(message);
    }
    
    @Test
    void shouldCreateForbiddenException() {
        // Given
        String message = "Access forbidden";
        
        // When
        BusinessException exception = BusinessException.forbidden(message);
        
        // Then
        assertThat(exception.getCode()).isEqualTo("FORBIDDEN");
        assertThat(exception.getMessage()).isEqualTo(message);
    }
    
    @Test
    void shouldCreateBadRequestException() {
        // Given
        String message = "Bad request";
        
        // When
        BusinessException exception = BusinessException.badRequest(message);
        
        // Then
        assertThat(exception.getCode()).isEqualTo("BAD_REQUEST");
        assertThat(exception.getMessage()).isEqualTo(message);
    }
}