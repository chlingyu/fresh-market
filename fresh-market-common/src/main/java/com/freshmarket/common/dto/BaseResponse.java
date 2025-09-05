package com.freshmarket.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 统一响应封装类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一响应格式")
public class BaseResponse<T> {

    @Schema(description = "响应码", example = "SUCCESS")
    private String code;

    @Schema(description = "响应信息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    public BaseResponse() {
        this.timestamp = Instant.now();
    }

    public BaseResponse(String code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public BaseResponse(String code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    // 成功响应
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>("SUCCESS", "操作成功", data);
    }

    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>("SUCCESS", "操作成功");
    }

    // 失败响应
    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(code, message);
    }

    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>("INTERNAL_ERROR", message);
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}