package com.banking.core.auth.controller;

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
 * Admin Controller - Endpoints accessible only to admins
 * Requires: ADMIN role only
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("message", "Admin Dashboard");
        dashboard.put("access", "Requires ADMIN role only");
        dashboard.put("user", auth.getName());
        dashboard.put("features", new String[] {
                "User Management",
                "System Configuration",
                "Reports Generation",
                "Audit Logs",
                "Role Management"
        });
        dashboard.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Get all users - Admin only");
        response.put("user", auth.getName());
        response.put("access", "Requires ADMIN role only");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "System information - Admin only");
        response.put("user", auth.getName());
        response.put("systemStatus", "OPERATIONAL");
        response.put("access", "Requires ADMIN role only");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "System configuration - Admin only");
        response.put("user", auth.getName());
        response.put("access", "Requires ADMIN role only");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}
