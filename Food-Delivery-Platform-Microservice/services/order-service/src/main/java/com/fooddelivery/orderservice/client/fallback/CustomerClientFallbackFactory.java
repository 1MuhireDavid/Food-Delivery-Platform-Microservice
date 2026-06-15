package com.fooddelivery.orderservice.client.fallback;

import com.fooddelivery.orderservice.client.CustomerClient;
import com.fooddelivery.orderservice.dto.client.CustomerInfo;
import com.fooddelivery.orderservice.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerClientFallbackFactory implements FallbackFactory<CustomerClient> {

    private static final Logger log = LoggerFactory.getLogger(CustomerClientFallbackFactory.class);

    @Override
    public CustomerClient create(Throwable cause) {
        log.error("Customer Service call failed: {}", cause.getMessage());
        return new CustomerClient() {
            @Override
            public CustomerInfo getById(Long id) {
                throw new ServiceUnavailableException(
                        "Customer Service is temporarily unavailable. Please try again later.");
            }

            @Override
            public CustomerInfo getByUsername(String username) {
                throw new ServiceUnavailableException(
                        "Customer Service is temporarily unavailable. Please try again later.");
            }
        };
    }
}
