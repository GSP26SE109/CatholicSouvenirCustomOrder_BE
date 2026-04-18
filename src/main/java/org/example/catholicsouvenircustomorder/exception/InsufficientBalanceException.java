package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when artisan wallet doesn't have sufficient balance for refund
 * Requirements: 4.7, 11.1
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
