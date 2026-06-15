package com.david.delivery_service.event;

public record OrderCancelledEvent(
        Long orderId,
        Long customerId,
        Long restaurantId
) {}
