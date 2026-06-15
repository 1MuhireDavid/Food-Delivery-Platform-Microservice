package com.fooddelivery.orderservice.client.fallback;

import com.fooddelivery.orderservice.client.RestaurantClient;
import com.fooddelivery.orderservice.dto.client.MenuItemInfo;
import com.fooddelivery.orderservice.dto.client.RestaurantInfo;
import com.fooddelivery.orderservice.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RestaurantClientFallbackFactory implements FallbackFactory<RestaurantClient> {

    private static final Logger log = LoggerFactory.getLogger(RestaurantClientFallbackFactory.class);

    @Override
    public RestaurantClient create(Throwable cause) {
        log.error("Restaurant Service call failed: {}", cause.getMessage());
        return new RestaurantClient() {
            @Override
            public RestaurantInfo getById(Long id) {
                throw new ServiceUnavailableException(
                        "Restaurant Service is temporarily unavailable. Please try again later.");
            }

            @Override
            public MenuItemInfo getMenuItemById(Long menuItemId) {
                throw new ServiceUnavailableException(
                        "Restaurant Service is temporarily unavailable. Please try again later.");
            }
        };
    }
}
