package com.featureflagshq.sdk.exceptions;

public class NetworkException extends FeatureFlagsHQException {
    
    public NetworkException(String message) {
        super(message);
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}