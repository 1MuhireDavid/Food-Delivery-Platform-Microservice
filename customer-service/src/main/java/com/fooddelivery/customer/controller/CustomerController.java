package com.fooddelivery.customer.controller;

import com.fooddelivery.customer.dto.CustomerResponse;
import com.fooddelivery.customer.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(customerService.getProfile(authentication.getName()));
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @PatchMapping("/internal/{username}/promote-to-owner")
    public ResponseEntity<Void> promoteToRestaurantOwner(@PathVariable String username) {
        customerService.promoteToRestaurantOwner(username);
        return ResponseEntity.noContent().build();
    }
}
