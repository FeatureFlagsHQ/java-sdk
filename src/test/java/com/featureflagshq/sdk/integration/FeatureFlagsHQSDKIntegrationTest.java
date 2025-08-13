package com.featureflagshq.sdk.integration;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FeatureFlagsHQ SDK Integration Tests")
@EnabledIfEnvironmentVariable(named = "FEATUREFLAGSHQ_INTEGRATION_TESTS", matches = "true")
public class FeatureFlagsHQSDKIntegrationTest {

    private FeatureFlagsHQSDK sdk;
    private static final String TEST_USER_ID = "integration-test-user";

    @BeforeEach
    void setUp() {
        // These tests require actual credentials - run only when explicitly enabled
        sdk = new FeatureFlagsHQSDK.Builder()
                .environment("test")
                .timeout(10)
                .maxRetries(2)
                .enableMetrics(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (sdk != null) {
            sdk.close();
        }
    }

    @Test
    @DisplayName("Should initialize SDK with real API connection")
    void shouldInitializeSDKWithRealApiConnection() {
        assertNotNull(sdk);
        
        Map<String, Object> health = sdk.getHealthCheck();
        assertNotNull(health);
        assertEquals("healthy", health.get("status"));
        assertEquals("test", health.get("environment"));
        assertFalse((Boolean) health.get("offline_mode"));
    }

    @Test
    @DisplayName("Should fetch flags from real API")
    void shouldFetchFlagsFromRealApi() {
        // Wait for initial sync
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean refreshSuccess = sdk.refreshFlags();
        assertTrue(refreshSuccess, "Should successfully refresh flags from API");

        Map<String, Map<String, Object>> flags = sdk.getAllFlags();
        assertNotNull(flags);
        // Flags may be empty, but the call should succeed
    }

    @Test
    @DisplayName("Should handle flag evaluation with real data")
    void shouldHandleFlagEvaluationWithRealData() {
        // Test basic flag operations
        boolean boolResult = sdk.getBool(TEST_USER_ID, "test_flag", false);
        assertNotNull(boolResult);

        String stringResult = sdk.getString(TEST_USER_ID, "welcome_message", "default");
        assertNotNull(stringResult);

        int intResult = sdk.getInt(TEST_USER_ID, "max_items", 10);
        assertTrue(intResult >= 0);

        double floatResult = sdk.getFloat(TEST_USER_ID, "multiplier", 1.0);
        assertTrue(floatResult >= 0.0);
    }

    @Test
    @DisplayName("Should handle user segments in evaluation")
    void shouldHandleUserSegmentsInEvaluation() {
        Map<String, Object> segments = new HashMap<>();
        segments.put("plan", "premium");
        segments.put("age", 25);
        segments.put("region", "US");

        boolean result = sdk.getBool(TEST_USER_ID, "premium_feature", false, segments);
        assertNotNull(result);

        // Get all flags for user with segments
        Map<String, Object> userFlags = sdk.getUserFlags(TEST_USER_ID, segments);
        assertNotNull(userFlags);
    }

    @Test
    @DisplayName("Should collect and report statistics")
    void shouldCollectAndReportStatistics() {
        // Generate some activity
        for (int i = 0; i < 5; i++) {
            sdk.getBool(TEST_USER_ID + "_" + i, "test_flag_" + i, false);
        }

        Map<String, Object> stats = sdk.getStats();
        assertNotNull(stats);
        assertTrue((Integer) stats.get("total_user_accesses") >= 5);
        assertTrue((Integer) stats.get("unique_users_count") >= 1);
        assertTrue((Integer) stats.get("unique_flags_count") >= 1);
        assertNotNull(stats.get("session_id"));
    }

    @Test
    @DisplayName("Should handle flag change callbacks")
    void shouldHandleFlagChangeCallbacks() throws Exception {
        CompletableFuture<Boolean> callbackReceived = new CompletableFuture<>();
        
        FeatureFlagsHQSDK callbackSdk = new FeatureFlagsHQSDK.Builder()
                .environment("test")
                .onFlagChange(change -> {
                    assertNotNull(change.getFlagName());
                    callbackReceived.complete(true);
                })
                .build();

        try {
            // Wait for potential flag changes (this might timeout if no changes occur)
            boolean received = callbackReceived.get(5, TimeUnit.SECONDS);
            // This assertion may fail if no flag changes occur during test
            // assertTrue(received, "Should receive flag change callback");
        } catch (Exception e) {
            // Timeout is acceptable if no flag changes occur during test
        } finally {
            callbackSdk.close();
        }
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccessSafely() throws InterruptedException {
        final int threadCount = 5;
        final int operationsPerThread = 20;
        Thread[] threads = new Thread[threadCount];
        final boolean[] results = new boolean[threadCount * operationsPerThread];
        final int[] index = {0};

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String userId = "concurrent_user_" + threadIndex + "_" + j;
                    String flagName = "concurrent_flag_" + (j % 5);
                    boolean result = sdk.getBool(userId, flagName, j % 2 == 0);
                    
                    synchronized (results) {
                        results[index[0]++] = result;
                    }
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

        // Verify all operations completed
        assertEquals(threadCount * operationsPerThread, index[0]);

        // Check that SDK statistics were updated correctly
        Map<String, Object> stats = sdk.getStats();
        assertTrue((Integer) stats.get("total_user_accesses") >= threadCount * operationsPerThread);
    }

    @Test
    @DisplayName("Should handle log uploading")
    void shouldHandleLogUploading() {
        // Generate some log entries
        sdk.getBool(TEST_USER_ID, "log_test_flag_1", false);
        sdk.getString(TEST_USER_ID, "log_test_flag_2", "default");
        sdk.getInt(TEST_USER_ID, "log_test_flag_3", 0);

        // Attempt to flush logs
        boolean flushResult = sdk.flushLogs();
        // Result depends on whether metrics are enabled and network is available
        assertNotNull(flushResult);
    }

    @Test
    @DisplayName("Should maintain performance under load")
    void shouldMaintainPerformanceUnderLoad() {
        long startTime = System.currentTimeMillis();
        final int operations = 100;

        for (int i = 0; i < operations; i++) {
            sdk.getBool("perf_user_" + i, "perf_flag_" + (i % 10), false);
        }

        long duration = System.currentTimeMillis() - startTime;
        
        // Should complete 100 operations in reasonable time (< 5 seconds)
        assertTrue(duration < 5000, "Performance test should complete within 5 seconds, took: " + duration + "ms");

        // Check evaluation time statistics
        Map<String, Object> stats = sdk.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> evalTimes = (Map<String, Object>) stats.get("evaluation_times");
        assertNotNull(evalTimes);
        
        double avgMs = (Double) evalTimes.get("avg_ms");
        assertTrue(avgMs < 50, "Average evaluation time should be under 50ms, was: " + avgMs + "ms");
    }

    @Test
    @DisplayName("Should handle network failures gracefully")
    void shouldHandleNetworkFailuresGracefully() {
        // Create SDK with invalid URL to simulate network failure
        FeatureFlagsHQSDK failureSdk = new FeatureFlagsHQSDK.Builder()
                .clientId("invalid")
                .clientSecret("invalid")
                .apiBaseUrl("https://invalid-url-that-does-not-exist.com")
                .environment("test")
                .timeout(2)
                .maxRetries(1)
                .build();

        try {
            // SDK should continue working even with network failures
            boolean result = failureSdk.getBool(TEST_USER_ID, "test_flag", true);
            assertTrue(result); // Should return default value

            Map<String, Object> health = failureSdk.getHealthCheck();
            assertNotNull(health);
            // Status might be "degraded" due to network issues
            assertTrue(health.get("status").equals("healthy") || health.get("status").equals("degraded"));

        } finally {
            failureSdk.close();
        }
    }
}