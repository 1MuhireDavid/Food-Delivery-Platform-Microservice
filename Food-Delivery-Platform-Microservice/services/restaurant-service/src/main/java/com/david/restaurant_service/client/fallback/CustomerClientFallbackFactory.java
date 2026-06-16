package com.david.restaurant_service.client.fallback;

import com.david.restaurant_service.client.CustomerClient;
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
            public void promoteToRestaurantOwner(String username) {
                // Promotion is a best-effort side effect; log and continue so restaurant creation succeeds.
                log.warn("Could not promote {} to RESTAURANT_OWNER — Customer Service unavailable: {}",
                        username, cause.getMessage());
            }
        };
    }
}
