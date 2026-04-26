package org.example.catholicsouvenircustomorder.exception;

/**
 * Base exception for VNPay related errors
 */
public class VNPayException extends Exception {
    
    private final String errorCode;
    
    public VNPayException(String message) {
        super(message);
        this.errorCode = null;
    }
    
    public VNPayException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }
    
    public VNPayException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public VNPayException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}