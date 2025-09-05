package com.freshmarket.common.exception;

/**
 * 业务异常基类
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final Object data;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }

    public BusinessException(String errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}