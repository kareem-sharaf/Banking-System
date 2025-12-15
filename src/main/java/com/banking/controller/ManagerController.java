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
 * Manager Controller - Endpoints accessible to managers and admins
 * Requires: MANAGER or ADMIN role
 */
@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getReports() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Manager reports endpoint");
        response.put("access", "Requires MANAGER or ADMIN role");
        response.put("user", auth.getName());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Analytics dashboard");
        response.put("access", "Requires MANAGER or ADMIN role");
        response.put("user", auth.getName());
        response.put("features", new String[]{
            "Transaction Analytics",
            "Customer Statistics",
            "Performance Metrics"
        });
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User management - Manager level");
        response.put("user", auth.getName());
        response.put("access", "Requires MANAGER or ADMIN role");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}

