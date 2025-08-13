package com.featureflagshq.sdk.utils;

import com.featureflagshq.sdk.config.Constants;
import com.featureflagshq.sdk.exceptions.ValidationException;

import java.util.regex.Pattern;

public final class ValidationUtils {
    
    private static final Pattern USER_ID_PATTERN = Pattern.compile(Constants.REGEX_USER_ID_PATTERN);
    private static final Pattern FLAG_NAME_PATTERN = Pattern.compile(Constants.REGEX_FLAG_NAME_PATTERN);
    
    private ValidationUtils() {
        // Utility class - prevent instantiation
    }
    
    public static String validateString(String value, String fieldName) throws ValidationException {
        return validateString(value, fieldName, 255);
    }
    
    public static String validateString(String value, String fieldName, int maxLength) throws ValidationException {
        if (value == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
        
        if (!(value instanceof String)) {
            throw new ValidationException(fieldName + " must be a string");
        }
        
        value = value.trim();
        if (value.isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
        
        if (value.length() > maxLength) {
            throw new ValidationException(fieldName + " too long (max " + maxLength + " characters)");
        }
        
        // Check for dangerous characters
        if (SecurityUtils.hasDangerousCharacters(value)) {
            throw new ValidationException(fieldName + " contains invalid characters");
        }
        
        // Basic SQL injection prevention
        if (SecurityUtils.hasSqlInjectionPatterns(value)) {
            throw new ValidationException(fieldName + " contains potentially dangerous content");
        }
        
        return value;
    }
    
    public static String validateUserId(String userId) throws ValidationException {
        if (userId == null) {
            throw new ValidationException("userId cannot be null");
        }
        
        userId = validateString(userId, "userId", Constants.MAX_USER_ID_LENGTH);
        
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            LoggingUtils.logWarning("Potentially unsafe userId pattern: " + 
                SecurityUtils.maskSensitiveValue(userId));
        }
        
        return userId;
    }
    
    public static String validateFlagName(String flagName) throws ValidationException {
        if (flagName == null) {
            throw new ValidationException("flagName cannot be null");
        }
        
        flagName = validateString(flagName, "flagName", Constants.MAX_FLAG_NAME_LENGTH);
        
        if (!FLAG_NAME_PATTERN.matcher(flagName).matches()) {
            throw new ValidationException("flagName contains invalid characters");
        }
        
        return flagName;
    }
    
    public static String validateUrl(String url) throws ValidationException {
        if (url == null || url.trim().isEmpty()) {
            throw new ValidationException("URL must be a non-empty string");
        }
        
        if (!SecurityUtils.isValidUrl(url)) {
            throw new ValidationException("Invalid URL format or unsupported protocol");
        }
        
        return url.replaceAll("/$", "");
    }
    
    public static String validateEnvironment(String environment) throws ValidationException {
        if (environment == null || environment.trim().isEmpty()) {
            throw new ValidationException("Environment cannot be null or empty");
        }
        
        return validateString(environment, "environment", 50);
    }
    
    public static int validateTimeout(int timeout) throws ValidationException {
        if (timeout <= 0) {
            throw new ValidationException("Timeout must be positive");
        }
        
        if (timeout > 300) { // 5 minutes max
            throw new ValidationException("Timeout too large (max 300 seconds)");
        }
        
        return timeout;
    }
    
    public static int validateMaxRetries(int maxRetries) throws ValidationException {
        if (maxRetries < 0) {
            throw new ValidationException("Max retries cannot be negative");
        }
        
        if (maxRetries > 10) {
            throw new ValidationException("Max retries too large (max 10)");
        }
        
        return maxRetries;
    }
    
    public static long validateInterval(long intervalMs, String fieldName) throws ValidationException {
        if (intervalMs <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
        
        if (intervalMs < 1000) { // Minimum 1 second
            throw new ValidationException(fieldName + " too small (min 1000ms)");
        }
        
        if (intervalMs > 3600000) { // Maximum 1 hour
            throw new ValidationException(fieldName + " too large (max 1 hour)");
        }
        
        return intervalMs;
    }
    
    public static boolean isValidSegmentKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        try {
            validateString(key, "segment_key", Constants.MAX_SEGMENT_KEY_LENGTH);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }
    
    public static Object validateSegmentValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // Allow basic types only
        if (value instanceof String || 
            value instanceof Number || 
            value instanceof Boolean) {
            return value;
        }
        
        // Convert to string for safety
        return String.valueOf(value);
    }
}