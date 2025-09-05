package com.freshmarket.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider测试类
 */
class JwtTokenProviderTest {
    
    private JwtTokenProvider tokenProvider;
    private final String testSecret = "test-jwt-secret-key-for-unit-testing-must-be-long-enough";
    private final int testExpiration = 86400000; // 24 hours
    
    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(testSecret, testExpiration);
    }
    
    @Test
    void shouldGenerateValidToken() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        
        // When
        String token = tokenProvider.generateToken(userId, username);
        
        // Then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }
    
    @Test
    void shouldExtractUsernameFromToken() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String token = tokenProvider.generateToken(userId, username);
        
        // When
        String extractedUsername = tokenProvider.getUsernameFromToken(token);
        
        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }
    
    @Test
    void shouldExtractUserIdFromToken() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String token = tokenProvider.generateToken(userId, username);
        
        // When
        Long extractedUserId = tokenProvider.getUserIdFromToken(token);
        
        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }
    
    @Test
    void shouldValidateValidToken() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String token = tokenProvider.generateToken(userId, username);
        
        // When
        boolean isValid = tokenProvider.validateToken(token);
        
        // Then
        assertThat(isValid).isTrue();
    }
    
    @Test
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        // When
        boolean isValid = tokenProvider.validateToken(invalidToken);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void shouldRejectEmptyToken() {
        // Given
        String emptyToken = "";
        
        // When
        boolean isValid = tokenProvider.validateToken(emptyToken);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void shouldReturnCorrectExpirationTime() {
        // When
        int expirationInSeconds = tokenProvider.getExpirationInSeconds();
        
        // Then
        assertThat(expirationInSeconds).isEqualTo(testExpiration / 1000);
    }
}