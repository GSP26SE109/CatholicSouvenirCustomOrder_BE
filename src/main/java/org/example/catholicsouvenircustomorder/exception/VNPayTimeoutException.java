package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when VNPay API call times out
 * This is a retryable exception
 */
public class VNPayTimeoutException extends VNPayException {
    
    public VNPayTimeoutException(String message) {
        super(message);
    }
    
    public VNPayTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}