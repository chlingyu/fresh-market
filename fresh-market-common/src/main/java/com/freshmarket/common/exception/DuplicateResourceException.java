package com.freshmarket.common.exception;

/**
 * 资源重复异常
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super("CONFLICT", message);
    }

    public DuplicateResourceException(String resourceType, String identifier) {
        super("CONFLICT", String.format("%s already exists with identifier: %s", resourceType, identifier));
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super("CONFLICT", message, cause);
    }
}