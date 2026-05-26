package com.fooddelivery.orderservice.dto.client;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MenuItemInfo {
    private Long id;
    private String name;
    private BigDecimal price;
    private boolean available;
    private Long restaurantId;
}
