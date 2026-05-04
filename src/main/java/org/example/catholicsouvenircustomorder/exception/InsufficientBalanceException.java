package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when artisan wallet doesn't have sufficient balance for refund
 * Specifically used during Artisan cancellation when they cannot cover the refund amount
 * Requirements: 3.4, 10.4
 */
public class InsufficientBalanceException extends RuntimeException {
    
    public static final String ERROR_CODE = "BALANCE_001";
    
    private final String errorCode;
    
    public InsufficientBalanceException(String message) {
        super(message);
        this.errorCode = ERROR_CODE;
    }
    
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ERROR_CODE;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
