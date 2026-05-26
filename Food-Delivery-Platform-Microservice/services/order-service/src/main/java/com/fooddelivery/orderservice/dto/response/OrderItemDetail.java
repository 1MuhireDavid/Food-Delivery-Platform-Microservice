package com.fooddelivery.orderservice.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDetail {
    private Long id;
    private Long menuItemId;
    private String itemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String specialInstructions;
}
