package com.david.restaurant_service.dto;

import com.david.restaurant_service.model.Restaurant;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RestaurantResponse {
    private Long id;
    private String name;
    private String description;
    private String cuisineType;
    private String address;
    private String city;
    private String phone;
    private boolean active;
    private double rating;
    private int estimatedDeliveryMinutes;
    private String ownerUsername;
    private LocalDateTime createdAt;

    public static RestaurantResponse fromEntity(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .cuisineType(r.getCuisineType())
                .address(r.getAddress())
                .city(r.getCity())
                .phone(r.getPhone())
                .active(r.isActive())
                .rating(r.getRating())
                .estimatedDeliveryMinutes(r.getEstimatedDeliveryMinutes())
                .ownerUsername(r.getOwnerUsername())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
