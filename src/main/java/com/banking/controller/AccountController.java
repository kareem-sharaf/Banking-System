package com.banking.controller;

import com.banking.dto.DepositRequest;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawalRequest;
import com.banking.service.AccountService;
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
 * REST API endpoints for account operations including deposits, withdrawals,
 * and transfers.
 * All operations use the Observer Pattern to notify various services about
 * account events.
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
     * Deposit money into an account
     * 
     * POST /api/accounts/{accountNumber}/deposit
     * 
     * @param accountNumber The account number to deposit to
     * @param request       The deposit request containing amount and optional
     *                      details
     * @return TransactionResponse with transaction details
     */
    @PostMapping("/{accountNumber}/deposit")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {

        logger.info("Received deposit request for account: {}", accountNumber);

        TransactionResponse response = accountService.deposit(accountNumber, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Withdraw money from an account
     * 
     * POST /api/accounts/{accountNumber}/withdraw
     * 
     * @param accountNumber The account number to withdraw from
     * @param request       The withdrawal request
     * @return TransactionResponse
     */
    @PostMapping("/{accountNumber}/withdraw")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable String accountNumber,
            @Valid @RequestBody WithdrawalRequest request) {

        logger.info("Received withdrawal request for account: {}", accountNumber);

        TransactionResponse response = accountService.withdraw(accountNumber, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Transfer money between accounts
     * 
     * POST /api/accounts/{fromAccountNumber}/transfer
     * 
     * @param fromAccountNumber The source account number
     * @param request           The transfer request containing destination account
     *                          and amount
     * @return TransactionResponse
     */
    @PostMapping("/{fromAccountNumber}/transfer")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> transfer(
            @PathVariable String fromAccountNumber,
            @Valid @RequestBody TransferRequest request) {

        logger.info("Received transfer request from account: {} to account: {}",
                fromAccountNumber, request.getToAccountNumber());

        TransactionResponse response = accountService.transfer(fromAccountNumber, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
