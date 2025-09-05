package com.freshmarket.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseResponse测试类
 */
class BaseResponseTest {
    
    @Test
    void shouldCreateSuccessResponse() {
        // Given
        String testData = "test data";
        
        // When
        BaseResponse<String> response = BaseResponse.success(testData);
        
        // Then
        assertThat(response.getCode()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("操作成功");
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getTimestamp()).isNotNull();
    }
    
    @Test
    void shouldCreateSuccessResponseWithoutData() {
        // When
        BaseResponse<Void> response = BaseResponse.success();
        
        // Then
        assertThat(response.getCode()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("操作成功");
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }
    
    @Test
    void shouldCreateErrorResponse() {
        // Given
        String errorCode = "TEST_ERROR";
        String errorMessage = "Test error message";
        
        // When
        BaseResponse<Void> response = BaseResponse.error(errorCode, errorMessage);
        
        // Then
        assertThat(response.getCode()).isEqualTo(errorCode);
        assertThat(response.getMessage()).isEqualTo(errorMessage);
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }
    
    @Test
    void shouldCreateErrorResponseWithDefaultCode() {
        // Given
        String errorMessage = "Test error message";
        
        // When
        BaseResponse<Void> response = BaseResponse.error(errorMessage);
        
        // Then
        assertThat(response.getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getMessage()).isEqualTo(errorMessage);
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }
}