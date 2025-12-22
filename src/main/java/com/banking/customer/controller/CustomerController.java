package com.banking.customer.controller;

import com.banking.core.enums.CustomerStatus;
import com.banking.customer.dto.CreateCustomerRequest;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.UpdateCustomerRequest;
import com.banking.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer Controller
 *
 * REST API for Customer Management (CRUD).
 * Also includes customer-specific endpoints.
 *
 * @author Banking System
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    /**
     * Create a new customer
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        logger.info("Received request to create customer for user ID: {}", request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    /**
     * Get all customers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    /**
     * Get customer by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    /**
     * Get customer by user ID
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerResponse> getCustomerByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(customerService.getCustomerByUserId(userId));
    }

    /**
     * Get customer by customer number
     */
    @GetMapping("/number/{customerNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerResponse> getCustomerByCustomerNumber(@PathVariable String customerNumber) {
        return ResponseEntity.ok(customerService.getCustomerByCustomerNumber(customerNumber));
    }

    /**
     * Get customers by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<CustomerResponse>> getCustomersByStatus(@PathVariable CustomerStatus status) {
        return ResponseEntity.ok(customerService.getCustomersByStatus(status));
    }

    /**
     * Update customer
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    /**
     * Delete customer
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if customer exists by user ID
     */
    @GetMapping("/exists/user/{userId}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Boolean> existsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(customerService.existsByUserId(userId));
    }

    /**
     * Check if customer exists by customer number
     */
    @GetMapping("/exists/number/{customerNumber}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Boolean> existsByCustomerNumber(@PathVariable String customerNumber) {
        return ResponseEntity.ok(customerService.existsByCustomerNumber(customerNumber));
    }

    // Legacy customer-specific endpoints (keeping for backward compatibility)

    /**
     * Get customer accounts (legacy endpoint)
     */
    @GetMapping("/customer/accounts")
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

    /**
     * Get customer transactions (legacy endpoint)
     */
    @GetMapping("/customer/transactions")
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

    /**
     * Get customer profile (legacy endpoint)
     */
    @GetMapping("/customer/profile")
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
