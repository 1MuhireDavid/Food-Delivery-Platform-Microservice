package com.fooddelivery.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class PlaceOrderRequest {

    @NotNull
    private Long restaurantId;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    private String deliveryAddress;
    private String specialInstructions;
}
