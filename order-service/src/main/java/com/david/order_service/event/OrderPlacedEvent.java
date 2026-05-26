package com.david.order_service.event;

public record OrderPlacedEvent(
        Long orderId,
        Long customerId,
        Long restaurantId,
        String restaurantAddress,
        String deliveryAddress
) {}