package com.featureflagshq.sdk.utils;

import com.featureflagshq.sdk.config.Constants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

public final class SecurityUtils {
    
    private static final Pattern[] SENSITIVE_PATTERNS = {
        Pattern.compile("secret[\"']?\\s*[:=]\\s*[\"']?([^\"'\\s]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("signature[\"']?\\s*[:=]\\s*[\"']?([^\"'\\s]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("token[\"']?\\s*[:=]\\s*[\"']?([^\"'\\s]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("password[\"']?\\s*[:=]\\s*[\"']?([^\"'\\s]+)", Pattern.CASE_INSENSITIVE)
    };
    
    private SecurityUtils() {
        // Utility class - prevent instantiation
    }
    
    public static String generateSignature(String payload, String timestamp, String clientId, String clientSecret) {
        try {
            String message = clientId + ":" + timestamp + ":" + payload;
            Mac mac = Mac.getInstance(Constants.HASH_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), Constants.HASH_ALGORITHM);
            mac.init(secretKey);
            byte[] signature = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    public static boolean containsSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        
        return false;
    }
    
    public static String sanitizeForLogging(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove or mask potentially sensitive data
        String sanitized = input;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("$1=***");
        }
        
        return sanitized;
    }
    
    public static boolean hasDangerousCharacters(String input) {
        if (input == null) {
            return false;
        }
        
        for (String dangerousChar : Constants.DANGEROUS_CHARS) {
            if (input.contains(dangerousChar)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean hasSqlInjectionPatterns(String input) {
        if (input == null) {
            return false;
        }
        
        String inputLower = input.toLowerCase();
        for (String pattern : Constants.SQL_INJECTION_PATTERNS) {
            if (inputLower.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static String generateUserHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(input.hashCode());
        }
    }
    
    public static int calculateRolloutPercentage(String flagName, String userId) {
        try {
            String hashInput = flagName + ":" + userId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8));
            return (hash[0] & 0xFF) % 100;
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return Math.abs((userId + flagName).hashCode()) % 100;
        }
    }
    
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            java.net.URL parsed = new java.net.URL(url);
            String protocol = parsed.getProtocol();
            return "http".equals(protocol) || "https".equals(protocol);
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }
    
    public static String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 8) {
            return "***";
        }
        
        int visibleChars = Math.min(4, value.length() / 4);
        return value.substring(0, visibleChars) + "***";
    }
}