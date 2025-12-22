package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.UpdateAccountRequest;
import com.banking.account.service.AccountService;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Account Controller
 * 
 * REST API for Account Management (CRUD, Grouping).
 * Transaction operations have been moved to TransactionController.
 * 
 * @author Banking System
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    /**
     * Get all accounts
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    /**
     * Get accounts by customer ID
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    /**
     * Create a new account
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        logger.info("Received request to create account for customer: {}", request.getCustomerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    /**
     * Get account details
     */
    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    /**
     * Update account details
     */
    @PutMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(accountNumber, request));
    }

    /**
     * Close account
     */
    @DeleteMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> closeAccount(@PathVariable String accountNumber) {
        accountService.closeAccount(accountNumber);
        return ResponseEntity.noContent().build();
    }

    /**
     * Link accounts (Parent/Child grouping)
     */
    @PostMapping("/{parentAccountNumber}/link/{childAccountNumber}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> linkAccounts(
            @PathVariable String parentAccountNumber,
            @PathVariable String childAccountNumber) {
        accountService.linkAccounts(parentAccountNumber, childAccountNumber);
        return ResponseEntity.ok().build();
    }
}
