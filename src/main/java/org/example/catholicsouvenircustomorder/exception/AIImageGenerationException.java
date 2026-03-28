package org.example.catholicsouvenircustomorder.exception;

public class AIImageGenerationException extends RuntimeException {
    
    public AIImageGenerationException(String message) {
        super(message);
    }
    
    public AIImageGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
