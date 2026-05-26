package com.david.delivery_service.dto;

import com.david.delivery_service.model.Delivery;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryResponse {
    private Long id;
    private String status;
    private Long orderId;
    private String customerUsername;
    private String driverName;
    private String driverPhone;
    private String pickupAddress;
    private String deliveryAddress;
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;

    public static DeliveryResponse fromEntity(Delivery d) {
        return DeliveryResponse.builder()
                .id(d.getId())
                .status(d.getStatus().name())
                .orderId(d.getOrderId())
                .customerUsername(d.getCustomerUsername())
                .driverName(d.getDriverName())
                .driverPhone(d.getDriverPhone())
                .pickupAddress(d.getPickupAddress())
                .deliveryAddress(d.getDeliveryAddress())
                .createdAt(d.getCreatedAt())
                .assignedAt(d.getAssignedAt())
                .pickedUpAt(d.getPickedUpAt())
                .deliveredAt(d.getDeliveredAt())
                .build();
    }
}
