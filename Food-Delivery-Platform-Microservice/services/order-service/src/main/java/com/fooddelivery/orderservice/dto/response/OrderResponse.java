package com.fooddelivery.orderservice.dto.response;

import com.fooddelivery.orderservice.model.Order;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {

    private Long id;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private String deliveryAddress;
    private String specialInstructions;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedDeliveryTime;

    private Long customerId;
    private Long restaurantId;
    // Enriched by Feign at write time; null on read-only lookups to avoid coupling
    private String customerName;
    private String restaurantName;

    private List<OrderItemDetail> items;

    public static OrderResponse fromEntity(Order order) {
        OrderResponse dto = new OrderResponse();
        dto.setId(order.getId());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDeliveryFee(order.getDeliveryFee());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setSpecialInstructions(order.getSpecialInstructions());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setEstimatedDeliveryTime(order.getEstimatedDeliveryTime());
        dto.setCustomerId(order.getCustomerId());
        dto.setRestaurantId(order.getRestaurantId());
        dto.setItems(order.getItems().stream().map(item -> {
            OrderItemDetail detail = new OrderItemDetail();
            detail.setId(item.getId());
            detail.setMenuItemId(item.getMenuItemId());
            detail.setItemName(item.getItemName());
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(item.getUnitPrice());
            detail.setSubtotal(item.getSubtotal());
            detail.setSpecialInstructions(item.getSpecialInstructions());
            return detail;
        }).toList());
        return dto;
    }
}
