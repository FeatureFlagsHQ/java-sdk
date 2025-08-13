package com.featureflagshq.sdk.exceptions;

public class AuthenticationException extends FeatureFlagsHQException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}