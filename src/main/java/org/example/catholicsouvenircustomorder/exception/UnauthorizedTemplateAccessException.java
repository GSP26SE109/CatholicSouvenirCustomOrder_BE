package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when a user attempts to access or modify a template/request
 * they don't have permission for.
 */
public class UnauthorizedTemplateAccessException extends RuntimeException {
    
    private final String resourceType;
    private final String action;
    
    public UnauthorizedTemplateAccessException(String message) {
        super(message);
        this.resourceType = null;
        this.action = null;
    }
    
    public UnauthorizedTemplateAccessException(String message, String resourceType, String action) {
        super(message);
        this.resourceType = resourceType;
        this.action = action;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getAction() {
        return action;
    }
}
