package com.fooddelivery.orderservice.client;

import com.fooddelivery.orderservice.client.fallback.RestaurantClientFallbackFactory;
import com.fooddelivery.orderservice.dto.client.MenuItemInfo;
import com.fooddelivery.orderservice.dto.client.RestaurantInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "restaurant-service", fallbackFactory = RestaurantClientFallbackFactory.class)
public interface RestaurantClient {

    @GetMapping("/api/restaurants/internal/{id}")
    RestaurantInfo getById(@PathVariable Long id);

    @GetMapping("/api/restaurants/internal/menu-item/{menuItemId}")
    MenuItemInfo getMenuItemById(@PathVariable Long menuItemId);
}
