package com.banking.transaction.controller;

import com.banking.account.dto.DepositRequest;
import com.banking.account.dto.WithdrawalRequest;
import com.banking.transaction.dto.MoneyTransferRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.facade.TransactionFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transaction Controller
 * 
 * Handles all transaction-related operations including deposits, withdrawals, and transfers.
 * Delegates business logic to TransactionFacade.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionFacade transactionFacade;

    /**
     * Deposit money into an account
     */
    @PostMapping("/{accountNumber}/deposit")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(transactionFacade.deposit(accountNumber, request));
    }

    /**
     * Withdraw money from an account
     */
    @PostMapping("/{accountNumber}/withdraw")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable String accountNumber,
            @Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.ok(transactionFacade.withdraw(accountNumber, request));
    }

    /**
     * Transfer money between accounts
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody MoneyTransferRequest request) {
        return ResponseEntity.ok(transactionFacade.transfer(request.getFromAccountNumber(), request));
    }
}
