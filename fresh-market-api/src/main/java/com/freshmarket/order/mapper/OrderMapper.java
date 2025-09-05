package com.freshmarket.order.mapper;

import com.freshmarket.order.dto.OrderItemResponse;
import com.freshmarket.order.dto.OrderResponse;
import com.freshmarket.order.entity.Order;
import com.freshmarket.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 订单映射器
 * 使用MapStruct自动生成Entity和DTO之间的转换代码
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrderMapper {

    /**
     * 订单实体转换为响应DTO
     */
    @Mapping(source = "orderNumber", target = "orderNumber")
    @Mapping(source = "shippingAddress", target = "shippingAddress")
    @Mapping(source = "phone", target = "phone")
    @Mapping(source = "notes", target = "notes")
    OrderResponse toOrderResponse(Order order);

    /**
     * 订单项实体转换为响应DTO
     */
    @Mapping(source = "productId", target = "productId")
    @Mapping(source = "productName", target = "productName")
    @Mapping(source = "productPrice", target = "productPrice")
    @Mapping(source = "quantity", target = "quantity")
    @Mapping(source = "subtotal", target = "subtotal")
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);

    /**
     * 订单项实体列表转换为响应DTO列表
     */
    List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> orderItems);
}