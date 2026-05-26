package com.fooddelivery.orderservice.client;

import com.fooddelivery.orderservice.dto.client.CustomerInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    @GetMapping("/api/customers/{id}")
    CustomerInfo getById(@PathVariable Long id);

    @GetMapping("/api/customers/by-username/{username}")
    CustomerInfo getByUsername(@PathVariable String username);
}
