package com.freshmarket.order.dto;

import com.freshmarket.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

import java.time.Instant;

/**
 * 订单搜索请求DTO
 */
@Schema(description = "订单搜索请求")
public class OrderSearchRequest {

    @Schema(description = "订单状态", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "开始时间")
    private Instant startTime;

    @Schema(description = "结束时间")
    private Instant endTime;

    @Schema(description = "订单编号关键词", example = "ORD169")
    private String orderNumberKeyword;

    @Min(value = 0, message = "页码不能为负数")
    @Schema(description = "页码(从0开始)", example = "0")
    private Integer page = 0;

    @Min(value = 1, message = "每页大小必须大于0")
    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "排序字段", example = "createdAt", 
           allowableValues = {"id", "orderNumber", "totalAmount", "createdAt", "updatedAt"})
    private String sortBy = "createdAt";

    @Schema(description = "排序方向", example = "desc", 
           allowableValues = {"asc", "desc"})
    private String sortDir = "desc";

    // 默认构造函数
    public OrderSearchRequest() {}

    // Getters and Setters
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getOrderNumberKeyword() {
        return orderNumberKeyword;
    }

    public void setOrderNumberKeyword(String orderNumberKeyword) {
        this.orderNumberKeyword = orderNumberKeyword;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }
}