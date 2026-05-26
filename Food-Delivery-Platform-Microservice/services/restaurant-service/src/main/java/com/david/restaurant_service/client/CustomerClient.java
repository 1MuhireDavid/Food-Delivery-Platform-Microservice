package com.david.restaurant_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    @PatchMapping("/api/customers/internal/{username}/promote-to-owner")
    void promoteToRestaurantOwner(@PathVariable("username") String username);
}
