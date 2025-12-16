package com.banking.exception;

/**
 * Exception thrown when account has insufficient balance for a transaction
 * 
 * @author Banking System
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
