package com.banking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Customer Controller - Endpoints accessible to customers and above
 * Requires: CUSTOMER, TELLER, MANAGER, or ADMIN role
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCustomerAccounts() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Customer accounts endpoint");
        response.put("access", "Requires CUSTOMER, TELLER, MANAGER, or ADMIN role");
        response.put("user", auth.getName());
        response.put("timestamp", LocalDateTime.now());
        response.put("note", "This is a test endpoint for Keycloak authentication");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCustomerTransactions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Customer transactions endpoint");
        response.put("access", "Requires CUSTOMER, TELLER, MANAGER, or ADMIN role");
        response.put("user", auth.getName());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCustomerProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Customer profile endpoint");
        response.put("user", auth.getName());
        response.put("authorities", auth.getAuthorities());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}
