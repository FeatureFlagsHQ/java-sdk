package com.featureflagshq.sdk.utils;

import com.featureflagshq.sdk.config.Constants;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoggingUtils {
    
    private static final Logger logger = Logger.getLogger("FeatureFlagsHQ");
    private static final boolean ENABLE_LOGGING = Constants.DEFAULT_ENABLE_LOGGING;
    
    private LoggingUtils() {
        // Utility class - prevent instantiation
    }
    
    public static void logInfo(String message) {
        if (ENABLE_LOGGING) {
            logger.info(sanitizeLogMessage(message));
        }
    }
    
    public static void logWarning(String message) {
        if (ENABLE_LOGGING) {
            logger.warning(sanitizeLogMessage(message));
        }
    }
    
    public static void logError(String message) {
        if (ENABLE_LOGGING) {
            logger.severe(sanitizeLogMessage(message));
        }
    }
    
    public static void logError(String message, Throwable throwable) {
        if (ENABLE_LOGGING) {
            logger.log(Level.SEVERE, sanitizeLogMessage(message), throwable);
        }
    }
    
    public static void logDebug(String message) {
        if (ENABLE_LOGGING) {
            logger.fine(sanitizeLogMessage(message));
        }
    }
    
    public static void logTrace(String message) {
        if (ENABLE_LOGGING) {
            logger.finest(sanitizeLogMessage(message));
        }
    }
    
    public static String sanitizeLogMessage(String message) {
        if (message == null) {
            return "";
        }
        
        // Sanitize sensitive data
        String sanitized = SecurityUtils.sanitizeForLogging(message);
        
        // Remove control characters that could break log parsing
        sanitized = sanitized.replaceAll("[\\r\\n\\t\\u0000-\\u001f\\u007f-\\u009f]", " ");
        
        // Limit message length to prevent log flooding
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 997) + "...";
        }
        
        return sanitized;
    }
    
    public static void logStatistics(String operation, long durationMs, boolean success) {
        if (ENABLE_LOGGING) {
            String status = success ? "SUCCESS" : "FAILURE";
            logInfo(String.format("STATS: %s completed in %dms - %s", operation, durationMs, status));
        }
    }
    
    public static void logRateLimit(String userId, String operation) {
        logWarning(String.format("Rate limit exceeded for user %s on operation %s", 
            SecurityUtils.maskSensitiveValue(userId), operation));
    }
    
    public static void logCircuitBreaker(String state, String operation) {
        logWarning(String.format("Circuit breaker %s for operation %s", state, operation));
    }
    
    public static void logSecurityEvent(String event, String details) {
        logError(String.format("SECURITY: %s - %s", event, sanitizeLogMessage(details)));
    }
    
    public static void logApiCall(String method, String url, int responseCode, long durationMs) {
        if (ENABLE_LOGGING) {
            logDebug(String.format("API: %s %s -> %d (%dms)", 
                method, SecurityUtils.sanitizeForLogging(url), responseCode, durationMs));
        }
    }
    
    public static void logFlagEvaluation(String flagName, String userId, Object result, long durationMs) {
        if (ENABLE_LOGGING) {
            logTrace(String.format("FLAG: %s for user %s -> %s (%dms)", 
                flagName, 
                SecurityUtils.maskSensitiveValue(userId), 
                String.valueOf(result), 
                durationMs));
        }
    }
    
    public static boolean isLoggingEnabled() {
        return ENABLE_LOGGING;
    }
    
    public static Logger getLogger() {
        return logger;
    }
}