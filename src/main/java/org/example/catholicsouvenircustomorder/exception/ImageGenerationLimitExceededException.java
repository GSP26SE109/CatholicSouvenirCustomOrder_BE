package org.example.catholicsouvenircustomorder.exception;

/**
 * Exception thrown when AI image generation limit is exceeded.
 * Each custom request has a maximum number of image generations allowed.
 */
public class ImageGenerationLimitExceededException extends RuntimeException {
    
    private final int currentCount;
    private final int maxCount;
    
    public ImageGenerationLimitExceededException(String message) {
        super(message);
        this.currentCount = 0;
        this.maxCount = 0;
    }
    
    public ImageGenerationLimitExceededException(String message, int currentCount, int maxCount) {
        super(message);
        this.currentCount = currentCount;
        this.maxCount = maxCount;
    }
    
    public int getCurrentCount() {
        return currentCount;
    }
    
    public int getMaxCount() {
        return maxCount;
    }
}
