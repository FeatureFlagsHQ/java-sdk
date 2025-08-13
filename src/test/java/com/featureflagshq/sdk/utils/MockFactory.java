package com.featureflagshq.sdk.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Factory class for creating mock data and responses for testing
 */
public final class MockFactory {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private MockFactory() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a mock flag response JSON string
     */
    public static String createMockFlagsResponse() {
        try {
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> flags = new ArrayList<>();
            
            // Boolean flag
            flags.add(createMockFlag("test_boolean_flag", "bool", true, true, 100, Collections.emptyList()));
            
            // String flag
            flags.add(createMockFlag("test_string_flag", "string", "hello world", true, 50, 
                Arrays.asList(createMockSegment("plan", "string", "==", "premium"))));
            
            // Integer flag
            flags.add(createMockFlag("test_integer_flag", "int", 42, true, 100, Collections.emptyList()));
            
            // Float flag
            flags.add(createMockFlag("test_float_flag", "float", 3.14159, true, 75, 
                Arrays.asList(createMockSegment("age", "int", ">=", 18))));
            
            // JSON flag
            Map<String, Object> jsonValue = new HashMap<>();
            jsonValue.put("theme", "dark");
            jsonValue.put("timeout", 30000);
            jsonValue.put("features", Arrays.asList("feature1", "feature2"));
            flags.add(createMockFlag("test_json_flag", "json", jsonValue, true, 100, Collections.emptyList()));
            
            // Inactive flag
            flags.add(createMockFlag("inactive_flag", "bool", true, false, 100, Collections.emptyList()));
            
            response.put("data", flags);
            response.put("metadata", createMockMetadata());
            
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create mock flags response", e);
        }
    }
    
    /**
     * Creates a mock flag object
     */
    public static Map<String, Object> createMockFlag(String name, String type, Object value, 
                                                   boolean isActive, int rolloutPercentage, 
                                                   List<Map<String, Object>> segments) {
        Map<String, Object> flag = new HashMap<>();
        flag.put("name", name);
        flag.put("type", type);
        flag.put("value", value);
        flag.put("is_active", isActive);
        flag.put("description", "Test " + type + " flag");
        
        Map<String, Object> rollout = new HashMap<>();
        rollout.put("percentage", rolloutPercentage);
        flag.put("rollout", rollout);
        
        flag.put("segments", segments);
        
        return flag;
    }
    
    /**
     * Creates a mock segment object
     */
    public static Map<String, Object> createMockSegment(String name, String type, String comparator, Object value) {
        Map<String, Object> segment = new HashMap<>();
        segment.put("name", name);
        segment.put("type", type);
        segment.put("comparator", comparator);
        segment.put("value", value);
        segment.put("is_active", true);
        return segment;
    }
    
    /**
     * Creates mock metadata
     */
    public static Map<String, Object> createMockMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("total_count", 6);
        metadata.put("environment", "test");
        metadata.put("last_updated", "2024-01-01T00:00:00Z");
        return metadata;
    }
    
    /**
     * Creates an empty flags response
     */
    public static String createEmptyFlagsResponse() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("data", Collections.emptyList());
            response.put("metadata", Map.of("total_count", 0, "environment", "test"));
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create empty flags response", e);
        }
    }
    
    /**
     * Creates a mock error response
     */
    public static String createErrorResponse(int statusCode, String message) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("error", message);
            response.put("status_code", statusCode);
            response.put("timestamp", System.currentTimeMillis());
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create error response", e);
        }
    }
    
    /**
     * Creates a mock log batch request
     */
    public static String createMockLogBatchRequest() {
        try {
            Map<String, Object> request = new HashMap<>();
            List<Map<String, Object>> logs = new ArrayList<>();
            
            // Sample log entries
            logs.add(createMockLogEntry("user_123", "test_flag", true));
            logs.add(createMockLogEntry("user_456", "premium_feature", false));
            
            request.put("logs", logs);
            request.put("session_metadata", createMockSessionMetadata());
            
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create mock log batch request", e);
        }
    }
    
    /**
     * Creates a mock log entry
     */
    public static Map<String, Object> createMockLogEntry(String userId, String flagName, Object flagValue) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("userId", userId);
        logEntry.put("flagName", flagName);
        logEntry.put("flagValue", flagValue);
        logEntry.put("timestamp", "2024-01-01T12:00:00Z");
        logEntry.put("sessionId", UUID.randomUUID().toString());
        logEntry.put("evaluationTimeMs", 5);
        logEntry.put("segments", createMockUserSegments());
        logEntry.put("metadata", createMockLogMetadata());
        
        // Mock evaluation context
        Map<String, Object> evaluationContext = new HashMap<>();
        evaluationContext.put("flagActive", true);
        evaluationContext.put("flagFound", true);
        evaluationContext.put("defaultValueUsed", false);
        evaluationContext.put("reason", "active_flag");
        evaluationContext.put("totalSdkTimeMs", 5);
        logEntry.put("evaluationContext", evaluationContext);
        
        return logEntry;
    }
    
    /**
     * Creates mock user segments
     */
    public static Map<String, Object> createMockUserSegments() {
        Map<String, Object> segments = new HashMap<>();
        segments.put("plan", "premium");
        segments.put("age", 25);
        segments.put("region", "US");
        segments.put("beta_user", true);
        return segments;
    }
    
    /**
     * Creates mock session metadata
     */
    public static Map<String, Object> createMockSessionMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("session_id", UUID.randomUUID().toString());
        
        Map<String, Object> environment = new HashMap<>();
        environment.put("name", "test");
        metadata.put("environment", environment);
        
        metadata.put("system_info", createMockSystemInfo());
        metadata.put("stats", createMockStats());
        
        return metadata;
    }
    
    /**
     * Creates mock system info
     */
    public static Map<String, Object> createMockSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("platform", "Test Platform");
        systemInfo.put("java_version", "11.0.0");
        systemInfo.put("hostname", "test-host");
        systemInfo.put("process_id", 12345);
        systemInfo.put("cpu_count", 4);
        systemInfo.put("memory_total", 1073741824L); // 1GB
        return systemInfo;
    }
    
    /**
     * Creates mock statistics
     */
    public static Map<String, Object> createMockStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_user_accesses", 100);
        stats.put("unique_users_count", 10);
        stats.put("unique_flags_count", 5);
        stats.put("segment_matches", 25);
        stats.put("rollout_evaluations", 50);
        
        Map<String, Object> evalTimes = new HashMap<>();
        evalTimes.put("avg_ms", 5.5);
        evalTimes.put("min_ms", 1);
        evalTimes.put("max_ms", 15);
        evalTimes.put("total_ms", 550);
        evalTimes.put("count", 100);
        stats.put("evaluation_times", evalTimes);
        
        return stats;
    }
    
    /**
     * Creates mock log metadata
     */
    public static Map<String, Object> createMockLogMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sdk_version", "1.0.0");
        metadata.put("environment", "test");
        metadata.put("client_version", "test-client");
        return metadata;
    }
    
    /**
     * Creates a malformed JSON response for testing error handling
     */
    public static String createMalformedJsonResponse() {
        return "{ \"data\": [ { \"name\": \"test_flag\", \"value\": }"; // Missing value and closing braces
    }
    
    /**
     * Creates a mock successful response for any endpoint
     */
    public static String createSuccessResponse() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("timestamp", System.currentTimeMillis());
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create success response", e);
        }
    }
    
    /**
     * Creates mock flag data with complex segments
     */
    public static Map<String, Object> createComplexMockFlag() {
        List<Map<String, Object>> segments = Arrays.asList(
            createMockSegment("plan", "string", "==", "premium"),
            createMockSegment("age", "int", ">=", 18),
            createMockSegment("region", "string", "contains", "US"),
            createMockSegment("beta_user", "bool", "==", true)
        );
        
        return createMockFlag("complex_flag", "string", "premium_feature", true, 75, segments);
    }
    
    /**
     * Creates mock segments with different comparators for testing
     */
    public static List<Map<String, Object>> createTestSegments() {
        return Arrays.asList(
            createMockSegment("plan", "string", "==", "premium"),
            createMockSegment("age", "int", ">=", 18),
            createMockSegment("age_max", "int", "<=", 65),
            createMockSegment("score", "float", ">", 85.5),
            createMockSegment("region", "string", "contains", "US"),
            createMockSegment("email", "string", "!=", ""),
            createMockSegment("active", "bool", "==", true)
        );
    }
    
    /**
     * Creates mock user data with various segment values
     */
    public static Map<String, Object> createMockUser(String plan, int age, String region, boolean betaUser) {
        Map<String, Object> user = new HashMap<>();
        user.put("plan", plan);
        user.put("age", age);
        user.put("region", region);
        user.put("beta_user", betaUser);
        user.put("score", 90.5);
        user.put("email", "user@example.com");
        user.put("active", true);
        return user;
    }
    
    /**
     * Creates a variety of mock users for testing different scenarios
     */
    public static List<Map<String, Object>> createMockUsers() {
        return Arrays.asList(
            createMockUser("premium", 25, "US-West", true),
            createMockUser("basic", 19, "EU-Central", false),
            createMockUser("trial", 30, "US-East", false),
            createMockUser("enterprise", 35, "US-Central", true),
            createMockUser("premium", 45, "APAC", true)
        );
    }
}