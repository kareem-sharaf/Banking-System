package com.banking.core.exception;

/**
 * Exception thrown when account is in an invalid state for the requested
 * operation
 * 
 * @author Banking System
 */
public class InvalidAccountStateException extends RuntimeException {

    public InvalidAccountStateException(String message) {
        super(message);
    }

    public InvalidAccountStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
