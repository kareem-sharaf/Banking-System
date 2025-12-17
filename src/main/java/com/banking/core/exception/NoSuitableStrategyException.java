package com.banking.core.exception;

/**
 * Exception thrown when no suitable interest calculation strategy is found
 * for an account type.
 * 
 * @author Banking System
 */
public class NoSuitableStrategyException extends RuntimeException {

    public NoSuitableStrategyException(String message) {
        super(message);
    }

    public NoSuitableStrategyException(String message, Throwable cause) {
        super(message, cause);
    }
}
