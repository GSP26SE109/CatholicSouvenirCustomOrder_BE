package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when refund processing fails
 * This includes VNPay API errors and other refund-related failures
 * Requirements: 3.4, 10.4
 */
public class RefundProcessingException extends RuntimeException {
    
    // Error codes for refund processing errors
    public static final String VNPAY_ERROR = "REFUND_001";
    public static final String VNPAY_TIMEOUT = "REFUND_002";
    public static final String VNPAY_NETWORK_ERROR = "REFUND_003";
    public static final String PAYMENT_NOT_FOUND = "REFUND_004";
    public static final String INVALID_REFUND_AMOUNT = "REFUND_005";
    public static final String REFUND_ALREADY_PROCESSED = "REFUND_006";
    public static final String PARTIAL_REFUND_FAILURE = "REFUND_007";
    
    private final String errorCode;
    private final String vnpayResponseCode;
    
    public RefundProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.vnpayResponseCode = null;
    }
    
    public RefundProcessingException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.vnpayResponseCode = null;
    }
    
    public RefundProcessingException(String message, String errorCode, String vnpayResponseCode) {
        super(message);
        this.errorCode = errorCode;
        this.vnpayResponseCode = vnpayResponseCode;
    }
    
    public RefundProcessingException(String message, String errorCode, String vnpayResponseCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.vnpayResponseCode = vnpayResponseCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getVnpayResponseCode() {
        return vnpayResponseCode;
    }
}
