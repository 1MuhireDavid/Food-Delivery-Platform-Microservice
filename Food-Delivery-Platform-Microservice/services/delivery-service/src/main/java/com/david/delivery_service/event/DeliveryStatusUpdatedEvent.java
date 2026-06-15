package com.david.delivery_service.event;

import java.time.Instant;

public record DeliveryStatusUpdatedEvent(
        Long deliveryId,
        Long orderId,
        String status,
        String driverName,
        Instant updatedAt
) {}
