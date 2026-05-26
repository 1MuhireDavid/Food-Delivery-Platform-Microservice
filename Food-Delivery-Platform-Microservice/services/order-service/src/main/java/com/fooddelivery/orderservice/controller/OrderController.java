package com.fooddelivery.orderservice.controller;

import com.fooddelivery.orderservice.dto.request.PlaceOrderRequest;
import com.fooddelivery.orderservice.dto.response.OrderResponse;
import com.fooddelivery.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // API Gateway strips JWT and injects X-Customer-Id before forwarding the request
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestHeader("X-Customer-Id") Long customerId,
            @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(customerId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderResponse>> getByRestaurant(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrdersByRestaurant(restaurantId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable Long id,
            @RequestHeader("X-Customer-Id") Long customerId) {
        return ResponseEntity.ok(orderService.cancelOrder(id, customerId));
    }
}
