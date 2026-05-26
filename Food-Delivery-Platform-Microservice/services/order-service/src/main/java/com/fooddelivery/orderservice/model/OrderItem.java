package com.fooddelivery.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Snapshot reference — no JPA join to Restaurant Service's MenuItem entity
    @Column(nullable = false)
    private Long menuItemId;

    // Name snapshot captured at order time so reads never depend on Restaurant Service
    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private int quantity;

    // Price snapshot — protects against future price changes affecting past orders
    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal subtotal;

    private String specialInstructions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
