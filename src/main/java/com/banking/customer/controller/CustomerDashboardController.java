package com.banking.customer.controller;

import com.banking.customer.dto.CustomerDashboardDto;
import com.banking.customer.module.entity.facade.CustomerDashboardFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer Dashboard Controller
 * 
 * Provides aggregated view data for the customer dashboard.
 */
@RestController
@RequestMapping("/api/customer/dashboard")
@RequiredArgsConstructor
public class CustomerDashboardController {

    private final CustomerDashboardFacade customerDashboardFacade;

    /**
     * Get the dashboard data for the currently authenticated customer.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerDashboardDto> getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        return ResponseEntity.ok(customerDashboardFacade.getCustomerDashboard(username));
    }
}
