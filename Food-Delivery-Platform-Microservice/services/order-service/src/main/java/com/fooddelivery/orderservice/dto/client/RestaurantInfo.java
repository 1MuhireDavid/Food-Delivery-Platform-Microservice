package com.fooddelivery.orderservice.dto.client;

import lombok.Data;

@Data
public class RestaurantInfo {
    private Long id;
    private String name;
    private String address;
    private boolean active;
    private int estimatedDeliveryMinutes;
}
