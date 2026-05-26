package com.david.delivery_service.event;

public record OrderPlacedEvent(
        Long orderId,
        Long restaurantId,
        String restaurantAddress,
        String deliveryAddress,
        String customerUsername
) {}
