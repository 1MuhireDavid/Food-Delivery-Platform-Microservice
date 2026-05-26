package com.david.restaurant_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public class RestaurantClient {
    @FeignClient(name = "restaurant-service", url = "${services.restaurant.url}")
    public interface RestaurantClient {
        @GetMapping("/api/restaurants/{id}")
        RestaurantResponse getRestaurantById(@PathVariable Long id);

        @GetMapping("/api/restaurants/menu-items/{menuItemId}")
        MenuItemResponse getMenuItemById(@PathVariable Long menuItemId);
    }
}
