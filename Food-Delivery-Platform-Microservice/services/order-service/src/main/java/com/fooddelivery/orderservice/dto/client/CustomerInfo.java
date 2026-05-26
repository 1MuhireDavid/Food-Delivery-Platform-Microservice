package com.fooddelivery.orderservice.dto.client;

import lombok.Data;

@Data
public class CustomerInfo {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String deliveryAddress;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
