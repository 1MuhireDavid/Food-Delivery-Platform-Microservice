package com.fooddelivery.customer.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String phone;
    private String deliveryAddress;
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    public enum Role { CUSTOMER, RESTAURANT_OWNER, DELIVERY_PARTNER, ADMIN }
}
