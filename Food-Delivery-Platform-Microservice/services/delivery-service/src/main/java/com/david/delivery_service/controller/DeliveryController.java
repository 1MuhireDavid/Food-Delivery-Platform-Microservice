package com.david.delivery_service.controller;

import com.david.delivery_service.dto.DeliveryResponse;
import com.david.delivery_service.service.DeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.getById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryResponse> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.getByOrderId(orderId));
    }

    @GetMapping
    public ResponseEntity<List<DeliveryResponse>> getByStatus(@RequestParam String status) {
        return ResponseEntity.ok(deliveryService.getByStatus(status));
    }

    @GetMapping("/my-deliveries")
    public ResponseEntity<List<DeliveryResponse>> getMyDeliveries(Authentication auth) {
        return ResponseEntity.ok(deliveryService.getMyDeliveries(auth.getName()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryResponse> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, status));
    }

    @PostMapping("/order/{orderId}/cancel")
    public ResponseEntity<DeliveryResponse> cancel(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.cancelDelivery(orderId));
    }
}
