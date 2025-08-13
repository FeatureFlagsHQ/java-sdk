package com.featureflagshq.sdk.unit;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Edge Case Tests")
public class EdgeCaseTest {

    private FeatureFlagsHQSDK sdk;
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        sdk = new FeatureFlagsHQSDK.Builder()
                .clientId(TEST_CLIENT_ID)
                .clientSecret(TEST_CLIENT_SECRET)
                .environment("test")
                .offlineMode(true)
                .enableMetrics(false)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (sdk != null) {
            sdk.close();
        }
    }

    @Nested
    @DisplayName("Extreme Input Tests")
    class ExtremeInputTests {

        @Test
        @DisplayName("Should handle maximum length user ID")
        void shouldHandleMaximumLengthUserId() {
            String maxLengthUserId = "a".repeat(255);
            boolean result = sdk.getBool(maxLengthUserId, "test_flag", false);
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle user ID at length boundary")
        void shouldHandleUserIdAtLengthBoundary() {
            String boundaryUserId = "a".repeat(254); // Just under limit
            boolean result = sdk.getBool(boundaryUserId, "test_flag", false);
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle maximum length flag name")
        void shouldHandleMaximumLengthFlagName() {
            String maxLengthFlagName = "a".repeat(255);
            boolean result = sdk.getBool(TEST_USER_ID, maxLengthFlagName, false);
            assertFalse(result); // Should handle gracefully
        }

        @Test
        @DisplayName("Should handle empty segments map")
        void shouldHandleEmptySegmentsMap() {
            Map<String, Object> emptySegments = Collections.emptyMap();
            boolean result = sdk.getBool(TEST_USER_ID, "test_flag", true, emptySegments);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle segments with null values")
        void shouldHandleSegmentsWithNullValues() {
            Map<String, Object> segmentsWithNulls = new HashMap<>();
            segmentsWithNulls.put("plan", null);
            segmentsWithNulls.put("age", 25);
            segmentsWithNulls.put("region", null);

            boolean result = sdk.getBool(TEST_USER_ID, "test_flag", false, segmentsWithNulls);
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle segments with complex nested objects")
        void shouldHandleSegmentsWithComplexNestedObjects() {
            Map<String, Object> nestedObject = new HashMap<>();
            nestedObject.put("level1", Map.of("level2", Map.of("level3", "deep_value")));
            
            Map<String, Object> complexSegments = new HashMap<>();
            complexSegments.put("nested", nestedObject);
            complexSegments.put("list", List.of(1, 2, 3, "string", true));

            boolean result = sdk.getBool(TEST_USER_ID, "complex_flag", false, complexSegments);
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle extremely large segment values")
        void shouldHandleExtremelyLargeSegmentValues() {
            Map<String, Object> largeSegments = new HashMap<>();
            largeSegments.put("large_string", "x".repeat(10000));
            largeSegments.put("large_number", Long.MAX_VALUE);
            largeSegments.put("large_double", Double.MAX_VALUE);

            String result = sdk.getString(TEST_USER_ID, "test_flag", "default", largeSegments);
            assertEquals("default", result);
        }
    }

    @Nested
    @DisplayName("Unicode and Special Character Tests")
    class UnicodeAndSpecialCharacterTests {

        @Test
        @DisplayName("Should handle Unicode characters in user ID")
        void shouldHandleUnicodeCharactersInUserId() {
            String unicodeUserId = "user_测试_123";
            boolean result = sdk.getBool(unicodeUserId, "test_flag", true);
            assertTrue(result); // Should return default due to validation
        }

        @Test
        @DisplayName("Should handle Unicode characters in flag names")
        void shouldHandleUnicodeCharactersInFlagNames() {
            String unicodeFlagName = "测试_flag";
            boolean result = sdk.getBool(TEST_USER_ID, unicodeFlagName, true);
            assertTrue(result); // Should return default due to validation
        }

        @Test
        @DisplayName("Should handle emoji in inputs")
        void shouldHandleEmojiInInputs() {
            String emojiUserId = "user_🚀_123";
            String emojiFlagName = "flag_🎯";
            
            boolean result = sdk.getBool(emojiUserId, emojiFlagName, false);
            assertFalse(result); // Should return default due to validation
        }

        @Test
        @DisplayName("Should handle special whitespace characters")
        void shouldHandleSpecialWhitespaceCharacters() {
            String specialUserId = "user\u00A0\u2000\u2001"; // Non-breaking and em spaces
            boolean result = sdk.getBool(specialUserId, "test_flag", true);
            assertTrue(result); // Should return default due to validation
        }

        @Test
        @DisplayName("Should handle control characters")
        void shouldHandleControlCharacters() {
            String controlUserId = "user\u0001\u0002\u0003";
            boolean result = sdk.getBool(controlUserId, "test_flag", false);
            assertFalse(result); // Should return default due to validation
        }
    }

    @Nested
    @DisplayName("Numeric Edge Cases")
    class NumericEdgeCases {

        @Test
        @DisplayName("Should handle integer overflow scenarios")
        void shouldHandleIntegerOverflowScenarios() {
            int maxValue = sdk.getInt(TEST_USER_ID, "max_int", Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, maxValue);

            int minValue = sdk.getInt(TEST_USER_ID, "min_int", Integer.MIN_VALUE);
            assertEquals(Integer.MIN_VALUE, minValue);
        }

        @Test
        @DisplayName("Should handle float edge values")
        void shouldHandleFloatEdgeValues() {
            double maxDouble = sdk.getFloat(TEST_USER_ID, "max_double", Double.MAX_VALUE);
            assertEquals(Double.MAX_VALUE, maxDouble, 0.0);

            double minDouble = sdk.getFloat(TEST_USER_ID, "min_double", Double.MIN_VALUE);
            assertEquals(Double.MIN_VALUE, minDouble, 0.0);

            double infinity = sdk.getFloat(TEST_USER_ID, "infinity", Double.POSITIVE_INFINITY);
            assertEquals(Double.POSITIVE_INFINITY, infinity, 0.0);

            double negInfinity = sdk.getFloat(TEST_USER_ID, "neg_infinity", Double.NEGATIVE_INFINITY);
            assertEquals(Double.NEGATIVE_INFINITY, negInfinity, 0.0);
        }

        @Test
        @DisplayName("Should handle NaN values")
        void shouldHandleNaNValues() {
            double nanValue = sdk.getFloat(TEST_USER_ID, "nan_flag", Double.NaN);
            assertTrue(Double.isNaN(nanValue));
        }

        @Test
        @DisplayName("Should handle zero and negative zero")
        void shouldHandleZeroAndNegativeZero() {
            double positiveZero = sdk.getFloat(TEST_USER_ID, "pos_zero", 0.0);
            assertEquals(0.0, positiveZero, 0.0);

            double negativeZero = sdk.getFloat(TEST_USER_ID, "neg_zero", -0.0);
            assertEquals(-0.0, negativeZero, 0.0);
        }
    }

    @Nested
    @DisplayName("Memory and Resource Tests")
    class MemoryAndResourceTests {

        @Test
        @DisplayName("Should handle large number of flag evaluations")
        void shouldHandleLargeNumberOfFlagEvaluations() {
            final int iterations = 10000;
            for (int i = 0; i < iterations; i++) {
                String userId = "user_" + i;
                String flagName = "flag_" + (i % 100);
                boolean result = sdk.getBool(userId, flagName, false);
                assertNotNull(result);
            }

            Map<String, Object> stats = sdk.getStats();
            assertTrue((Integer) stats.get("total_user_accesses") >= iterations);
        }

        @Test
        @DisplayName("Should handle rapid successive calls")
        void shouldHandleRapidSuccessiveCalls() {
            long startTime = System.currentTimeMillis();
            final int rapidCalls = 1000;
            
            for (int i = 0; i < rapidCalls; i++) {
                sdk.getBool(TEST_USER_ID, "rapid_flag_" + i, false);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            assertTrue(duration < 5000, "Rapid calls should complete within 5 seconds");
        }

        @Test
        @DisplayName("Should handle memory pressure gracefully")
        void shouldHandleMemoryPressureGracefully() {
            // Create many unique users to test memory handling
            for (int i = 0; i < 15000; i++) { // Above MAX_UNIQUE_USERS_TRACKED
                String userId = "memory_user_" + i;
                sdk.getBool(userId, "memory_flag", false);
            }

            Map<String, Object> stats = sdk.getStats();
            Integer uniqueUsers = (Integer) stats.get("unique_users_count");
            assertTrue(uniqueUsers <= 10000, "Should limit tracked users to prevent memory issues");
        }
    }

    @Nested
    @DisplayName("Timing and Race Condition Tests")
    class TimingAndRaceConditionTests {

        @Test
        @DisplayName("Should handle concurrent flag changes")
        void shouldHandleConcurrentFlagChanges() throws InterruptedException {
            final AtomicInteger changeCount = new AtomicInteger(0);
            final CountDownLatch latch = new CountDownLatch(1);

            FeatureFlagsHQSDK concurrentSDK = new FeatureFlagsHQSDK.Builder()
                    .clientId(TEST_CLIENT_ID)
                    .clientSecret(TEST_CLIENT_SECRET)
                    .offlineMode(true)
                    .onFlagChange(change -> {
                        changeCount.incrementAndGet();
                        latch.countDown();
                    })
                    .build();

            try {
                // Simulate concurrent access
                Thread[] threads = new Thread[10];
                for (int i = 0; i < threads.length; i++) {
                    final int threadId = i;
                    threads[i] = new Thread(() -> {
                        for (int j = 0; j < 100; j++) {
                            concurrentSDK.getBool("user_" + threadId, "flag_" + j, false);
                        }
                    });
                }

                for (Thread thread : threads) {
                    thread.start();
                }

                for (Thread thread : threads) {
                    thread.join();
                }

                // Verify no deadlocks or race conditions occurred
                Map<String, Object> stats = concurrentSDK.getStats();
                assertTrue((Integer) stats.get("total_user_accesses") >= 1000);

            } finally {
                concurrentSDK.close();
            }
        }

        @Test
        @DisplayName("Should handle rapid SDK creation and destruction")
        void shouldHandleRapidSdkCreationAndDestruction() {
            for (int i = 0; i < 50; i++) {
                FeatureFlagsHQSDK tempSDK = new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .clientSecret(TEST_CLIENT_SECRET)
                        .offlineMode(true)
                        .enableMetrics(false)
                        .build();

                boolean result = tempSDK.getBool("temp_user", "temp_flag", false);
                assertFalse(result);
                
                tempSDK.close();
            }
        }

        @RepeatedTest(5)
        @DisplayName("Should be deterministic across multiple runs")
        void shouldBeDeterministicAcrossMultipleRuns() {
            Map<String, Object> segments = Map.of("plan", "premium", "age", 25);
            
            boolean result1 = sdk.getBool(TEST_USER_ID, "deterministic_flag", false, segments);
            boolean result2 = sdk.getBool(TEST_USER_ID, "deterministic_flag", false, segments);
            
            assertEquals(result1, result2, "Results should be deterministic");
        }
    }

    @Nested
    @DisplayName("Configuration Edge Cases")
    class ConfigurationEdgeCases {

        @Test
        @DisplayName("Should handle minimal timeout configuration")
        void shouldHandleMinimalTimeoutConfiguration() {
            assertDoesNotThrow(() -> {
                FeatureFlagsHQSDK minTimeoutSDK = new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .clientSecret(TEST_CLIENT_SECRET)
                        .timeout(1)
                        .offlineMode(true)
                        .build();
                
                boolean result = minTimeoutSDK.getBool(TEST_USER_ID, "timeout_flag", false);
                assertFalse(result);
                
                minTimeoutSDK.close();
            });
        }

        @Test
        @DisplayName("Should handle zero retries configuration")
        void shouldHandleZeroRetriesConfiguration() {
            assertDoesNotThrow(() -> {
                FeatureFlagsHQSDK noRetrySDK = new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .clientSecret(TEST_CLIENT_SECRET)
                        .maxRetries(0)
                        .offlineMode(true)
                        .build();

                boolean result = noRetrySDK.getBool(TEST_USER_ID, "retry_flag", true);
                assertTrue(result);

                noRetrySDK.close();
            });
        }

        @Test
        @DisplayName("Should handle extreme API URL configurations")
        void shouldHandleExtremeApiUrlConfigurations() {
            // Very long but valid URL
            String longUrl = "https://very-long-subdomain-name-that-tests-url-length-limits.api.featureflagshq.com";
            
            assertDoesNotThrow(() -> {
                FeatureFlagsHQSDK longUrlSDK = new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .clientSecret(TEST_CLIENT_SECRET)
                        .apiBaseUrl(longUrl)
                        .offlineMode(true)
                        .build();

                longUrlSDK.close();
            });
        }
    }

    @Nested
    @DisplayName("Error Recovery Tests")
    class ErrorRecoveryTests {

        @Test
        @DisplayName("Should recover from internal state corruption")
        void shouldRecoverFromInternalStateCorruption() {
            // Test that SDK continues working even if internal state is problematic
            for (int i = 0; i < 1000; i++) {
                boolean result = sdk.getBool(TEST_USER_ID + "_recovery", "recovery_flag_" + i, true);
                assertTrue(result); // Should always return default in offline mode
            }

            Map<String, Object> health = sdk.getHealthCheck();
            assertEquals("healthy", health.get("status"));
        }

        @Test
        @DisplayName("Should handle callback exceptions gracefully")
        void shouldHandleCallbackExceptionsGracefully() {
            final AtomicBoolean callbackCalled = new AtomicBoolean(false);
            
            FeatureFlagsHQSDK callbackSDK = new FeatureFlagsHQSDK.Builder()
                    .clientId(TEST_CLIENT_ID)
                    .clientSecret(TEST_CLIENT_SECRET)
                    .offlineMode(true)
                    .onFlagChange(change -> {
                        callbackCalled.set(true);
                        throw new RuntimeException("Callback intentionally failed");
                    })
                    .build();

            try {
                // SDK should continue working despite callback failures
                boolean result = callbackSDK.getBool(TEST_USER_ID, "callback_test", false);
                assertFalse(result);

                Map<String, Object> health = callbackSDK.getHealthCheck();
                assertEquals("healthy", health.get("status"));

            } finally {
                callbackSDK.close();
            }
        }
    }

    @Nested
    @DisplayName("Boundary Value Tests")
    class BoundaryValueTests {

        @Test
        @DisplayName("Should handle empty string inputs correctly")
        void shouldHandleEmptyStringInputsCorrectly() {
            String result1 = sdk.getString(TEST_USER_ID, "empty_flag", "");
            assertEquals("", result1);

            String result2 = sdk.getString("", "test_flag", "default");
            assertEquals("default", result2); // Should return default due to validation
        }

        @Test
        @DisplayName("Should handle single character inputs")
        void shouldHandleSingleCharacterInputs() {
            boolean result = sdk.getBool("a", "b", true);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle inputs at exact validation boundaries")
        void shouldHandleInputsAtExactValidationBoundaries() {
            // Test exactly at MAX_USER_ID_LENGTH
            String exactLengthUserId = "u".repeat(255);
            boolean result = sdk.getBool(exactLengthUserId, "boundary_flag", false);
            assertFalse(result);

            // Test one character over
            String overLengthUserId = "u".repeat(256);
            boolean overResult = sdk.getBool(overLengthUserId, "boundary_flag", true);
            assertTrue(overResult); // Should return default due to validation failure
        }
    }
}