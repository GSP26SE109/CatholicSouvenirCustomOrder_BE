package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when zone input validation fails.
 * This includes missing required zones, invalid input types, or constraint violations.
 */
public class ZoneValidationException extends RuntimeException {
    
    private final String zoneName;
    private final String violationType;
    
    public ZoneValidationException(String message) {
        super(message);
        this.zoneName = null;
        this.violationType = null;
    }
    
    public ZoneValidationException(String message, String zoneName, String violationType) {
        super(message);
        this.zoneName = zoneName;
        this.violationType = violationType;
    }
    
    public String getZoneName() {
        return zoneName;
    }
    
    public String getViolationType() {
        return violationType;
    }
}
