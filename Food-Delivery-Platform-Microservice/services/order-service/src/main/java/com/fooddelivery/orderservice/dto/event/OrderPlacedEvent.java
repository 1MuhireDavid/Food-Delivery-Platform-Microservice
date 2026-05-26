package com.fooddelivery.orderservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private String customerName;
    private String deliveryAddress;
    private String restaurantAddress;
    private BigDecimal totalAmount;
    private LocalDateTime estimatedDeliveryTime;
    private List<OrderItemSummary> items;
    private LocalDateTime occurredAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemSummary {
        private Long menuItemId;
        private String itemName;
        private int quantity;
        private BigDecimal unitPrice;
    }
}
