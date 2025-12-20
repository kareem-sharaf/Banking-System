package com.banking.transaction.facade.impl;

import com.banking.account.dto.DepositRequest;
import com.banking.account.dto.WithdrawalRequest;
import com.banking.transaction.service.TransactionService;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.facade.TransactionFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionFacadeImpl implements TransactionFacade {

    private static final Logger logger = LoggerFactory.getLogger(TransactionFacadeImpl.class);

    private final TransactionService transactionService;

    @Override
    public TransactionResponse deposit(String accountNumber, DepositRequest request) {
        logger.info("Processing deposit via Facade for account: {}", accountNumber);
        return transactionService.deposit(accountNumber, request);
    }

    @Override
    public TransactionResponse withdraw(String accountNumber, WithdrawalRequest request) {
        logger.info("Processing withdrawal via Facade for account: {}", accountNumber);
        return transactionService.withdraw(accountNumber, request);
    }

    @Override
    public TransactionResponse transfer(String fromAccountNumber, TransferRequest request) {
        logger.info("Processing transfer via Facade from: {} to: {}", fromAccountNumber, request.getToAccountNumber());
        return transactionService.transfer(fromAccountNumber, request);
    }
}
