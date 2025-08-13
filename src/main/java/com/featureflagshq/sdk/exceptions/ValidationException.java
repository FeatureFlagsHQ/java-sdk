package com.featureflagshq.sdk.exceptions;

public class ValidationException extends FeatureFlagsHQException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}