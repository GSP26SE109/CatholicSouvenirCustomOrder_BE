package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when commission calculation fails or produces invalid results
 */
public class CommissionCalculationException extends RuntimeException {
    public CommissionCalculationException(String message) {
        super(message);
    }
    
    public CommissionCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
