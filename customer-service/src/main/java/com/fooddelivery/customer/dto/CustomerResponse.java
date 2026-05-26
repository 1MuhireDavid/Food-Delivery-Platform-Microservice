package com.fooddelivery.customer.dto;

import com.fooddelivery.customer.model.Customer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String deliveryAddress;
    private String city;
    private String role;

    public static CustomerResponse fromEntity(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .username(c.getUsername())
                .email(c.getEmail())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .phone(c.getPhone())
                .deliveryAddress(c.getDeliveryAddress())
                .city(c.getCity())
                .role(c.getRole().name())
                .build();
    }
}
