package com.fooddelivery.orderservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private LocalDateTime occurredAt;
}
