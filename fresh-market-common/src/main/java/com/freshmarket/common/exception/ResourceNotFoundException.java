package com.freshmarket.common.exception;

/**
 * 资源未找到异常
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super("NOT_FOUND", String.format("%s not found with identifier: %s", resourceType, identifier));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super("NOT_FOUND", message, cause);
    }
}