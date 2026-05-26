package com.fooddelivery.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owned by Customer Service — stored as a plain foreign key value, no JPA join
    @Column(nullable = false)
    private Long customerId;

    // Owned by Restaurant Service — stored as a plain foreign key value, no JPA join
    @Column(nullable = false)
    private Long restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Builder.Default
    private BigDecimal deliveryFee = new BigDecimal("2.99");

    private String deliveryAddress;
    private String specialInstructions;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime estimatedDeliveryTime;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = OrderStatus.PLACED;
        if (deliveryFee == null) deliveryFee = new BigDecimal("2.99");
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        PLACED,
        CONFIRMED,
        PREPARING,
        READY_FOR_PICKUP,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED
    }
}
