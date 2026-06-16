package com.fooddelivery.orderservice.client;

import com.fooddelivery.orderservice.client.fallback.CustomerClientFallbackFactory;
import com.fooddelivery.orderservice.dto.client.CustomerInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", fallbackFactory = CustomerClientFallbackFactory.class)
public interface CustomerClient {

    @GetMapping("/api/customers/internal/{id}")
    CustomerInfo getById(@PathVariable Long id);
}
