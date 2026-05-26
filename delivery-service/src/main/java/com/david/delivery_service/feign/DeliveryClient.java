package com.david.delivery_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class DeliveryClient {
    @FeignClient(name = "delivery-service", url = "${services.delivery.url}")
    public interface DeliveryClient {
        @PostMapping("/api/deliveries/create-for-order")
        DeliveryResponse createForOrder(@RequestBody CreateDeliveryRequest request);
    }
}
