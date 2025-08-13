package com.featureflagshq.sdk.utils;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public final class TestUtils {
    
    public static final String TEST_CLIENT_ID = "test-client-id-12345";
    public static final String TEST_CLIENT_SECRET = "test-client-secret-67890-abcdef";
    public static final String TEST_USER_ID = "test-user-123";
    public static final String TEST_ENVIRONMENT = "test";
    
    private TestUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates an SDK instance configured for testing (offline mode, no metrics)
     */
    public static FeatureFlagsHQSDK createTestSDK() {
        return new FeatureFlagsHQSDK.Builder()
                .clientId(TEST_CLIENT_ID)
                .clientSecret(TEST_CLIENT_SECRET)
                .environment(TEST_ENVIRONMENT)
                .offlineMode(true)
                .enableMetrics(false)
                .build();
    }
    
    /**
     * Creates an SDK instance with custom configuration for testing
     */
    public static FeatureFlagsHQSDK createTestSDK(Map<String, Object> config) {
        FeatureFlagsHQSDK.Builder builder = new FeatureFlagsHQSDK.Builder()
                .clientId((String) config.getOrDefault("clientId", TEST_CLIENT_ID))
                .clientSecret((String) config.getOrDefault("clientSecret", TEST_CLIENT_SECRET))
                .environment((String) config.getOrDefault("environment", TEST_ENVIRONMENT))
                .offlineMode((Boolean) config.getOrDefault("offlineMode", true))
                .enableMetrics((Boolean) config.getOrDefault("enableMetrics", false));
        
        if (config.containsKey("apiBaseUrl")) {
            builder.apiBaseUrl((String) config.get("apiBaseUrl"));
        }
        
        if (config.containsKey("timeout")) {
            builder.timeout((Integer) config.get("timeout"));
        }
        
        if (config.containsKey("maxRetries")) {
            builder.maxRetries((Integer) config.get("maxRetries"));
        }
        
        return builder.build();
    }
    
    /**
     * Creates sample user segments for testing
     */
    public static Map<String, Object> createTestSegments() {
        Map<String, Object> segments = new HashMap<>();
        segments.put("plan", "premium");
        segments.put("age", 25);
        segments.put("region", "US");
        segments.put("beta_user", true);
        segments.put("account_type", "business");
        return segments;
    }
    
    /**
     * Creates sample user segments with custom values
     */
    public static Map<String, Object> createTestSegments(Map<String, Object> customSegments) {
        Map<String, Object> segments = createTestSegments();
        if (customSegments != null) {
            segments.putAll(customSegments);
        }
        return segments;
    }
    
    /**
     * Creates a list of test flag names
     */
    public static List<String> createTestFlagNames() {
        return Arrays.asList(
            "feature_flag_1",
            "feature_flag_2",
            "premium_feature",
            "beta_feature",
            "experimental_feature"
        );
    }
    
    /**
     * Creates a list of test user IDs
     */
    public static List<String> createTestUserIds() {
        return Arrays.asList(
            "user_123",
            "premium_user_456",
            "beta_user_789",
            "test_user_abc",
            "demo_user_xyz"
        );
    }
    
    /**
     * Waits for SDK initialization to complete
     */
    public static void waitForSDKInitialization(FeatureFlagsHQSDK sdk) {
        waitForSDKInitialization(sdk, 5000);
    }
    
    /**
     * Waits for SDK initialization to complete with custom timeout
     */
    public static void waitForSDKInitialization(FeatureFlagsHQSDK sdk, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            Map<String, Object> health = sdk.getHealthCheck();
            Boolean initialized = (Boolean) health.get("initialization_complete");
            if (initialized != null && initialized) {
                return;
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Generates test data for load testing
     */
    public static void generateTestLoad(FeatureFlagsHQSDK sdk, int operations) {
        List<String> userIds = createTestUserIds();
        List<String> flagNames = createTestFlagNames();
        
        for (int i = 0; i < operations; i++) {
            String userId = userIds.get(i % userIds.size());
            String flagName = flagNames.get(i % flagNames.size());
            
            // Mix different types of flag evaluations
            switch (i % 5) {
                case 0:
                    sdk.getBool(userId, flagName, false);
                    break;
                case 1:
                    sdk.getString(userId, flagName, "default");
                    break;
                case 2:
                    sdk.getInt(userId, flagName, 0);
                    break;
                case 3:
                    sdk.getFloat(userId, flagName, 0.0);
                    break;
                case 4:
                    sdk.getJson(userId, flagName, new HashMap<>());
                    break;
            }
        }
    }
    
    /**
     * Measures execution time of a runnable
     */
    public static long measureExecutionTime(Runnable operation) {
        long startTime = System.currentTimeMillis();
        operation.run();
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Validates that SDK statistics are reasonable
     */
    public static void validateSDKStatistics(Map<String, Object> stats, int expectedMinAccesses) {
        if (stats == null) {
            throw new AssertionError("Statistics should not be null");
        }
        
        // Check required fields
        String[] requiredFields = {
            "total_user_accesses", "unique_users_count", "unique_flags_count",
            "session_id", "cached_flags_count", "api_calls", "evaluation_times"
        };
        
        for (String field : requiredFields) {
            if (!stats.containsKey(field)) {
                throw new AssertionError("Statistics missing required field: " + field);
            }
        }
        
        // Validate access count
        Integer totalAccesses = (Integer) stats.get("total_user_accesses");
        if (totalAccesses < expectedMinAccesses) {
            throw new AssertionError("Expected at least " + expectedMinAccesses + 
                " user accesses, got " + totalAccesses);
        }
        
        // Validate positive counts
        Integer uniqueUsers = (Integer) stats.get("unique_users_count");
        Integer uniqueFlags = (Integer) stats.get("unique_flags_count");
        Integer cachedFlags = (Integer) stats.get("cached_flags_count");
        
        if (uniqueUsers < 0 || uniqueFlags < 0 || cachedFlags < 0) {
            throw new AssertionError("Statistics counts should not be negative");
        }
        
        // Validate session ID
        String sessionId = (String) stats.get("session_id");
        if (sessionId == null || sessionId.isEmpty()) {
            throw new AssertionError("Session ID should not be null or empty");
        }
    }
    
    /**
     * Validates that health check response is valid
     */
    public static void validateHealthCheck(Map<String, Object> health) {
        if (health == null) {
            throw new AssertionError("Health check should not be null");
        }
        
        // Check required fields
        String[] requiredFields = {
            "status", "sdk_version", "environment", "offline_mode",
            "session_id", "initialization_complete"
        };
        
        for (String field : requiredFields) {
            if (!health.containsKey(field)) {
                throw new AssertionError("Health check missing required field: " + field);
            }
        }
        
        // Validate status
        String status = (String) health.get("status");
        if (!"healthy".equals(status) && !"degraded".equals(status)) {
            throw new AssertionError("Health status should be 'healthy' or 'degraded', got: " + status);
        }
        
        // Validate session ID
        String sessionId = (String) health.get("session_id");
        if (sessionId == null || sessionId.isEmpty()) {
            throw new AssertionError("Session ID should not be null or empty");
        }
    }
    
    /**
     * Creates test data for concurrent testing
     */
    public static void runConcurrentTest(int threadCount, int operationsPerThread, Runnable operation) 
            throws InterruptedException {
        Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        operation.run();
                    }
                } catch (Exception e) {
                    exceptions[threadIndex] = e;
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Check for exceptions
        for (int i = 0; i < threadCount; i++) {
            if (exceptions[i] != null) {
                throw new RuntimeException("Thread " + i + " failed", exceptions[i]);
            }
        }
    }
    
    /**
     * Safely closes SDK instance, ignoring any exceptions
     */
    public static void safeCloseSDK(FeatureFlagsHQSDK sdk) {
        if (sdk != null) {
            try {
                sdk.close();
            } catch (Exception e) {
                // Ignore cleanup exceptions in tests
            }
        }
    }
    
    /**
     * Creates a delay for testing timing-sensitive operations
     */
    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Validates that a string is a valid UUID format
     */
    public static boolean isValidUUID(String uuid) {
        if (uuid == null) {
            return false;
        }
        
        // Basic UUID format validation
        return uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
    
    /**
     * Creates invalid input data for negative testing
     */
    public static String[] getInvalidInputs() {
        return new String[] {
            null,
            "",
            "   ",
            "invalid\ndata",
            "invalid\rdata",
            "invalid\0data",
            "'; DROP TABLE users; --",
            "SELECT * FROM flags",
            "a".repeat(300) // Too long
        };
    }
    
    /**
     * Validates that an operation completes within expected time
     */
    public static void assertCompletesWithinTime(Runnable operation, long maxTimeMs) {
        long duration = measureExecutionTime(operation);
        if (duration > maxTimeMs) {
            throw new AssertionError("Operation took " + duration + "ms, expected under " + maxTimeMs + "ms");
        }
    }
}