package com.david.delivery_service.event;

public record OrderPlacedEvent(
        Long orderId,
        Long customerId,
        Long restaurantId,
        String customerUsername,
        String deliveryAddress,
        String restaurantAddress
) {}
