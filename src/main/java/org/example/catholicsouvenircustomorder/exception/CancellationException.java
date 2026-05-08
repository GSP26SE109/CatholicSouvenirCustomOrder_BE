package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when order cancellation fails due to business rule violations
 * Requirements: 2.7, 3.4, 10.4
 */
public class CancellationException extends RuntimeException {
    
    // Error codes for cancellation errors
    public static final String STAGE_COMPLETED = "CANCEL_001";
    public static final String INVALID_REASON = "CANCEL_002";
    public static final String UNAUTHORIZED = "CANCEL_003";
    public static final String ALREADY_CANCELLED = "CANCEL_004";
    public static final String REFUND_FAILED = "CANCEL_005";
    public static final String INSUFFICIENT_BALANCE = "CANCEL_006";
    public static final String NO_PAID_STAGES = "CANCEL_007";
    public static final String INVALID_ORDER_STATUS = "CANCEL_008";
    public static final String INVALID_INITIATOR = "CANCEL_009";
    public static final String NO_REFUNDABLE_STAGES = "CANCEL_010";
    
    private final String errorCode;
    
    public CancellationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public CancellationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
