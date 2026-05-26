package com.david.restaurant_service.dto;

import com.david.restaurant_service.model.MenuItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MenuItemResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private boolean available;
    private String imageUrl;
    private Long restaurantId;

    public static MenuItemResponse fromEntity(MenuItem m) {
        return MenuItemResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .description(m.getDescription())
                .price(m.getPrice())
                .category(m.getCategory())
                .available(m.isAvailable())
                .imageUrl(m.getImageUrl())
                .restaurantId(m.getRestaurant().getId())
                .build();
    }
}
