package com.banking.exception;

/**
 * General exception for interest calculation errors
 * 
 * @author Banking System
 */
public class InterestCalculationException extends RuntimeException {

    public InterestCalculationException(String message) {
        super(message);
    }

    public InterestCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
