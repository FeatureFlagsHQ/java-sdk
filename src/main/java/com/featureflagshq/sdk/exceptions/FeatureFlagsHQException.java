package com.featureflagshq.sdk.exceptions;

public class FeatureFlagsHQException extends Exception {
    
    public FeatureFlagsHQException(String message) {
        super(message);
    }
    
    public FeatureFlagsHQException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public FeatureFlagsHQException(Throwable cause) {
        super(cause);
    }
}