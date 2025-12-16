package com.banking.exception;

/**
 * Exception thrown when an account is not found
 * 
 * @author Banking System
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }

    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
