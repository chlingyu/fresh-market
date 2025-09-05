package com.freshmarket.order.controller;

import com.freshmarket.common.dto.BaseResponse;
import com.freshmarket.order.dto.CreateOrderRequest;
import com.freshmarket.order.dto.OrderResponse;
import com.freshmarket.order.dto.OrderSearchRequest;
import com.freshmarket.order.enums.OrderStatus;
import com.freshmarket.order.service.OrderService;
import com.freshmarket.payment.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单管理", description = "订单的创建、查询、状态更新、取消等相关API")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "创建订单",
        description = "创建新的订单，需要用户登录",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "订单创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或库存不足"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
        }
    )
    public BaseResponse<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {
        Long userId = getUserId(authentication);
        OrderResponse order = orderService.createOrder(userId, request);
        return BaseResponse.success(order);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取订单详情",
        description = "根据订单ID获取订单的详细信息",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "订单不存在")
        }
    )
    public BaseResponse<OrderResponse> getOrder(
            Authentication authentication,
            @Parameter(description = "订单ID", example = "1") 
            @PathVariable Long id) {
        Long userId = getUserId(authentication);
        OrderResponse order = orderService.getOrderById(userId, id);
        return BaseResponse.success(order);
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(
        summary = "根据订单编号获取订单",
        description = "根据订单编号获取订单详情",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "订单不存在")
        }
    )
    public BaseResponse<OrderResponse> getOrderByNumber(
            Authentication authentication,
            @Parameter(description = "订单编号", example = "ORD16912345678901234") 
            @PathVariable String orderNumber) {
        Long userId = getUserId(authentication);
        OrderResponse order = orderService.getOrderByOrderNumber(userId, orderNumber);
        return BaseResponse.success(order);
    }

    @GetMapping
    @Operation(
        summary = "搜索订单",
        description = "根据条件搜索用户订单，支持分页、排序和多种筛选条件",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "搜索成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<Page<OrderResponse>> searchOrders(
            Authentication authentication,
            @Parameter(description = "订单状态") 
            @RequestParam(required = false) OrderStatus status,
            
            @Parameter(description = "开始时间") 
            @RequestParam(required = false) Instant startTime,
            
            @Parameter(description = "结束时间") 
            @RequestParam(required = false) Instant endTime,
            
            @Parameter(description = "订单编号关键词") 
            @RequestParam(required = false) String orderNumberKeyword,
            
            @Parameter(description = "页码(从0开始)") 
            @RequestParam(defaultValue = "0") Integer page,
            
            @Parameter(description = "每页大小") 
            @RequestParam(defaultValue = "20") Integer size,
            
            @Parameter(description = "排序字段") 
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "排序方向") 
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        OrderSearchRequest searchRequest = new OrderSearchRequest();
        searchRequest.setStatus(status);
        searchRequest.setStartTime(startTime);
        searchRequest.setEndTime(endTime);
        searchRequest.setOrderNumberKeyword(orderNumberKeyword);
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDir(sortDir);
        
        Long userId = getUserId(authentication);
        Page<OrderResponse> orders = orderService.searchOrders(userId, searchRequest);
        return BaseResponse.success(orders);
    }

    @PutMapping("/{id}/status")
    @Operation(
        summary = "更新订单状态",
        description = "更新指定订单的状态",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "状态转换不合法"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "订单不存在")
        }
    )
    public BaseResponse<OrderResponse> updateOrderStatus(
            Authentication authentication,
            @Parameter(description = "订单ID", example = "1") 
            @PathVariable Long id,
            @Parameter(description = "新的订单状态", example = "PAID") 
            @RequestParam OrderStatus status) {
        Long userId = getUserId(authentication);
        OrderResponse order = orderService.updateOrderStatus(userId, id, status);
        return BaseResponse.success(order);
    }

    @PostMapping("/{id}/pay")
    @Operation(
        summary = "发起订单支付",
        description = "为订单创建支付记录，返回支付信息。订单状态将通过支付事件异步更新",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "支付发起成功"),
            @ApiResponse(responseCode = "400", description = "订单状态不允许支付"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "订单不存在")
        }
    )
    public BaseResponse<PaymentResponse> payOrder(
            Authentication authentication,
            @Parameter(description = "订单ID", example = "1") 
            @PathVariable Long id,
            @Parameter(description = "支付方式", example = "ALIPAY")
            @RequestParam(defaultValue = "ALIPAY") String paymentMethod) {
        Long userId = getUserId(authentication);
        PaymentResponse payment = orderService.initiatePayment(userId, id, paymentMethod);
        return BaseResponse.success(payment);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "取消订单",
        description = "取消指定订单，恢复商品库存",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "400", description = "订单状态不允许取消"),
            @ApiResponse(responseCode = "401", description = "未授权访问"),
            @ApiResponse(responseCode = "404", description = "订单不存在")
        }
    )
    public BaseResponse<Void> cancelOrder(
            Authentication authentication,
            @Parameter(description = "订单ID", example = "1") 
            @PathVariable Long id) {
        Long userId = getUserId(authentication);
        orderService.cancelOrder(userId, id);
        return BaseResponse.success();
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "获取订单统计信息",
        description = "获取当前用户的订单统计信息，按状态分组",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权访问")
        }
    )
    public BaseResponse<Map<OrderStatus, Long>> getOrderStatistics(Authentication authentication) {
        Long userId = getUserId(authentication);
        Map<OrderStatus, Long> statistics = orderService.getUserOrderStatistics(userId);
        return BaseResponse.success(statistics);
    }

    private Long getUserId(Authentication authentication) {
        // 这里假设从Authentication中可以获取到用户ID
        // 实际实现可能需要从SecurityContext或JWT token中解析
        return Long.valueOf(authentication.getName());
    }
}