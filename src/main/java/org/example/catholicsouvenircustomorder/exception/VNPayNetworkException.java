package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when VNPay API call fails due to network issues
 * This is a retryable exception
 */
public class VNPayNetworkException extends VNPayException {
    
    public VNPayNetworkException(String message) {
        super(message);
    }
    
    public VNPayNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}