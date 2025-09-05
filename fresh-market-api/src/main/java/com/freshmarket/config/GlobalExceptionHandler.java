package com.freshmarket.config;

import com.freshmarket.common.dto.BaseResponse;
import com.freshmarket.common.exception.BusinessException;
import com.freshmarket.common.exception.DuplicateResourceException;
import com.freshmarket.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionException;
import org.springframework.retry.ExhaustedRetryException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException ex) {
        logger.warn("Business exception: {}", ex.getMessage());
        
        BaseResponse<Void> response = BaseResponse.error(ex.getCode(), ex.getMessage());
        HttpStatus status = getHttpStatusFromErrorCode(ex.getCode());
        
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * 参数校验异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Validation exception: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        BaseResponse<Map<String, String>> response = BaseResponse.error("VALIDATION_ERROR", "请求参数校验失败");
        response.setData(errors);
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 访问拒绝异常处理
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        
        BaseResponse<Void> response = BaseResponse.error("ACCESS_DENIED", "访问被拒绝");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * 资源未找到异常处理
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        
        BaseResponse<Void> response = BaseResponse.error(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 资源重复异常处理
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<BaseResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex) {
        logger.warn("Resource conflict: {}", ex.getMessage());
        
        BaseResponse<Void> response = BaseResponse.error(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 认证失败异常处理
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        logger.warn("Bad credentials: {}", ex.getMessage());
        
        BaseResponse<Void> response = BaseResponse.error("UNAUTHORIZED", "用户名或密码错误");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 缺失请求参数异常处理
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingParameterException(MissingServletRequestParameterException ex) {
        logger.warn("Missing request parameter: {}", ex.getMessage());
        
        BaseResponse<Void> response = BaseResponse.error("BAD_REQUEST", 
            String.format("缺失必需参数: %s", ex.getParameterName()));
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 参数类型不匹配异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        logger.warn("Type mismatch: {}", ex.getMessage());
        
        BaseResponse<Void> response = BaseResponse.error("BAD_REQUEST", 
            String.format("参数类型错误: %s", ex.getName()));
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 乐观锁失败异常处理
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<BaseResponse<Void>> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        logger.warn("Optimistic locking failure (concurrent update detected): {}", ex.getMessage());
        
        BaseResponse<Void> response = BaseResponse.error("CONCURRENT_UPDATE_ERROR", 
            "数据已被其他用户修改，请刷新后重试");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 重试耗尽异常处理
     */
    @ExceptionHandler(ExhaustedRetryException.class)
    public ResponseEntity<BaseResponse<Void>> handleExhaustedRetryException(ExhaustedRetryException ex) {
        logger.error("Retry attempts exhausted: {}", ex.getMessage(), ex);
        
        BaseResponse<Void> response = BaseResponse.error("RETRY_EXHAUSTED", 
            "操作失败，请稍后重试");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 事务异常处理
     */
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<BaseResponse<Void>> handleTransactionException(TransactionException ex) {
        logger.error("Transaction failure: {}", ex.getMessage(), ex);
        
        BaseResponse<Void> response = BaseResponse.error("TRANSACTION_ERROR", 
            "事务处理失败，操作已回滚");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 库存相关异常处理 (基于消息内容)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        String message = ex.getMessage();
        
        // 根据异常消息内容进行细分处理
        if (message != null) {
            if (message.contains("库存不足") || message.contains("库存扣减失败") || 
                message.contains("insufficient stock") || message.contains("超卖")) {
                logger.warn("Inventory shortage: {}", message);
                BaseResponse<Void> response = BaseResponse.error("INSUFFICIENT_INVENTORY", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            if (message.contains("订单状态") || message.contains("无法取消") || 
                message.contains("无法更新") || message.contains("状态不允许")) {
                logger.warn("Invalid order state transition: {}", message);
                BaseResponse<Void> response = BaseResponse.error("INVALID_ORDER_STATE", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            
            if (message.contains("支付") || message.contains("payment")) {
                logger.warn("Payment state issue: {}", message);
                BaseResponse<Void> response = BaseResponse.error("PAYMENT_STATE_ERROR", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
        }
        
        // 默认处理
        logger.warn("Illegal state: {}", message);
        BaseResponse<Void> response = BaseResponse.error("INVALID_OPERATION", 
            message != null ? message : "操作状态无效");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 业务参数异常处理 (基于消息内容)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage();
        
        // 根据异常消息内容进行细分处理
        if (message != null) {
            if (message.contains("商品不存在") || message.contains("商品已下架") ||
                message.contains("product not found") || message.contains("已下架")) {
                logger.warn("Product availability issue: {}", message);
                BaseResponse<Void> response = BaseResponse.error("PRODUCT_UNAVAILABLE", message);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            if (message.contains("用户") || message.contains("user")) {
                logger.warn("User related argument error: {}", message);
                BaseResponse<Void> response = BaseResponse.error("USER_ERROR", message);
                return ResponseEntity.badRequest().body(response);
            }
            
            if (message.contains("订单创建失败")) {
                logger.warn("Order creation failed: {}", message);
                BaseResponse<Void> response = BaseResponse.error("ORDER_CREATION_FAILED", message);
                return ResponseEntity.badRequest().body(response);
            }
        }
        
        // 默认处理
        logger.warn("Illegal argument: {}", message);
        BaseResponse<Void> response = BaseResponse.error("INVALID_PARAMETER", 
            message != null ? message : "请求参数无效");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 系统内部异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleInternalException(Exception ex) {
        logger.error("Internal server error", ex);
        
        BaseResponse<Void> response = BaseResponse.error("INTERNAL_ERROR", "系统内部错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 根据错误码获取HTTP状态码
     */
    private HttpStatus getHttpStatusFromErrorCode(String errorCode) {
        return switch (errorCode) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "BAD_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "CONFLICT" -> HttpStatus.CONFLICT;
            case "INSUFFICIENT_INVENTORY" -> HttpStatus.CONFLICT;
            case "INVALID_ORDER_STATE" -> HttpStatus.CONFLICT;
            case "PAYMENT_STATE_ERROR" -> HttpStatus.CONFLICT;
            case "PRODUCT_UNAVAILABLE" -> HttpStatus.NOT_FOUND;
            case "CONCURRENT_UPDATE_ERROR" -> HttpStatus.CONFLICT;
            case "RETRY_EXHAUSTED" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "TRANSACTION_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}