package com.freshmarket.common.exception;

/**
 * 业务异常类
 */
public class BusinessException extends RuntimeException {
    
    private final String code;
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    // 常见业务异常
    public static BusinessException of(String message) {
        return new BusinessException("BUSINESS_ERROR", message);
    }
    
    public static BusinessException notFound(String message) {
        return new BusinessException("NOT_FOUND", message);
    }
    
    public static BusinessException unauthorized(String message) {
        return new BusinessException("UNAUTHORIZED", message);
    }
    
    public static BusinessException forbidden(String message) {
        return new BusinessException("FORBIDDEN", message);
    }
    
    public static BusinessException badRequest(String message) {
        return new BusinessException("BAD_REQUEST", message);
    }
}