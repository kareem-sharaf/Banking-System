package com.banking.account.controller;

import com.banking.account.dto.AccountTypeResponse;
import com.banking.account.dto.CreateAccountTypeRequest;
import com.banking.account.dto.UpdateAccountTypeRequest;
import com.banking.account.service.AccountTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Account Type Controller
 *
 * REST API for Account Type Management (CRUD).
 *
 * @author Banking System
 */
@RestController
@RequestMapping("/api/account-types")
@RequiredArgsConstructor
public class AccountTypeController {

    private static final Logger logger = LoggerFactory.getLogger(AccountTypeController.class);

    private final AccountTypeService accountTypeService;

    /**
     * Create a new account type
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountTypeResponse> createAccountType(@Valid @RequestBody CreateAccountTypeRequest request) {
        logger.info("Received request to create account type: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(accountTypeService.createAccountType(request));
    }

    /**
     * Get all account types
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<AccountTypeResponse>> getAllAccountTypes() {
        return ResponseEntity.ok(accountTypeService.getAllAccountTypes());
    }

    /**
     * Get account type by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<AccountTypeResponse> getAccountTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(accountTypeService.getAccountTypeById(id));
    }

    /**
     * Get account type by code
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<AccountTypeResponse> getAccountTypeByCode(@PathVariable String code) {
        return ResponseEntity.ok(accountTypeService.getAccountTypeByCode(code));
    }

    /**
     * Update account type
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountTypeResponse> updateAccountType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountTypeRequest request) {
        return ResponseEntity.ok(accountTypeService.updateAccountType(id, request));
    }

    /**
     * Delete account type
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccountType(@PathVariable Long id) {
        accountTypeService.deleteAccountType(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if account type exists by code
     */
    @GetMapping("/exists/code/{code}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Boolean> existsByCode(@PathVariable String code) {
        return ResponseEntity.ok(accountTypeService.existsByCode(code));
    }

    /**
     * Check if account type exists by name
     */
    @GetMapping("/exists/name/{name}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Boolean> existsByName(@PathVariable String name) {
        return ResponseEntity.ok(accountTypeService.existsByName(name));
    }
}
