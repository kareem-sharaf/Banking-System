package com.banking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Teller Controller - Endpoints accessible to tellers and above
 * Requires: TELLER, MANAGER, or ADMIN role
 */
@RestController
@RequestMapping("/api/teller")
public class TellerController {

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getTellerTransactions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Teller transactions endpoint");
        response.put("access", "Requires TELLER, MANAGER, or ADMIN role");
        response.put("user", auth.getName());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-transaction")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> processTransaction(@RequestBody Map<String, Object> transaction) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transaction processed successfully (test)");
        response.put("transaction", transaction);
        response.put("processedBy", auth.getName());
        response.put("access", "Requires TELLER, MANAGER, or ADMIN role");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getTellerDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Teller dashboard");
        response.put("user", auth.getName());
        response.put("role", "TELLER");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}

