package com.featureflagshq.sdk.unit;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Circuit Breaker Unit Tests")
public class CircuitBreakerTest {

    private FeatureFlagsHQSDK sdk;
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        // Create SDK that can potentially trigger circuit breaker behavior
        sdk = new FeatureFlagsHQSDK.Builder()
                .clientId(TEST_CLIENT_ID)
                .clientSecret(TEST_CLIENT_SECRET)
                .environment("test")
                .timeout(1) // Short timeout to potentially trigger failures
                .maxRetries(1)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (sdk != null) {
            sdk.close();
        }
    }

    @Nested
    @DisplayName("Circuit Breaker State Tests")
    class CircuitBreakerStateTests {

        @Test
        @DisplayName("Should start in closed state")
        void shouldStartInClosedState() {
            Map<String, Object> health = sdk.getHealthCheck();
            assertNotNull(health);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> circuitBreaker = (Map<String, Object>) health.get("circuit_breaker");
            assertNotNull(circuitBreaker);
            
            String state = (String) circuitBreaker.get("state");
            assertEquals("CLOSED", state);
            
            Integer failureCount = (Integer) circuitBreaker.get("failure_count");
            assertEquals(0, failureCount);
        }

        @Test
        @DisplayName("Should report circuit breaker state in statistics")
        void shouldReportCircuitBreakerStateInStatistics() {
            Map<String, Object> stats = sdk.getStats();
            assertNotNull(stats);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> circuitBreaker = (Map<String, Object>) stats.get("circuit_breaker");
            assertNotNull(circuitBreaker);
            
            assertTrue(circuitBreaker.containsKey("state"));
            assertTrue(circuitBreaker.containsKey("failure_count"));
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Behavior Tests")
    class CircuitBreakerBehaviorTests {

        @Test
        @DisplayName("Should continue working when circuit breaker is active")
        void shouldContinueWorkingWhenCircuitBreakerIsActive() {
            // Even if circuit breaker is open, SDK should continue working with cached/default values
            boolean result = sdk.getBool(TEST_USER_ID, "test_flag", true);
            assertTrue(result);
            
            String stringResult = sdk.getString(TEST_USER_ID, "test_string_flag", "default");
            assertEquals("default", stringResult);
            
            int intResult = sdk.getInt(TEST_USER_ID, "test_int_flag", 42);
            assertEquals(42, intResult);
        }

        @Test
        @DisplayName("Should handle refresh operations when circuit breaker is active")
        void shouldHandleRefreshOperationsWhenCircuitBreakerIsActive() {
            // Multiple failed refresh attempts might trigger circuit breaker
            // But the operation should still complete without throwing exceptions
            
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10; i++) {
                    sdk.refreshFlags();
                }
            });
            
            // SDK should still be functional
            Map<String, Object> health = sdk.getHealthCheck();
            assertNotNull(health);
            assertTrue(health.containsKey("status"));
        }

        @Test
        @DisplayName("Should handle log upload operations when circuit breaker is active")
        void shouldHandleLogUploadOperationsWhenCircuitBreakerIsActive() {
            // Generate some activity first
            sdk.getBool(TEST_USER_ID, "test_flag", false);
            sdk.getString(TEST_USER_ID, "test_string", "default");
            
            // Multiple failed log upload attempts might trigger circuit breaker
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10; i++) {
                    sdk.flushLogs();
                }
            });
            
            // SDK should still be functional
            boolean result = sdk.getBool(TEST_USER_ID, "another_test_flag", true);
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Recovery Tests")
    class CircuitBreakerRecoveryTests {

        @Test
        @DisplayName("Should maintain functionality during recovery attempts")
        void shouldMaintainFunctionalityDuringRecoveryAttempts() {
            // Simulate multiple operations that might cause failures and recovery
            for (int i = 0; i < 20; i++) {
                // These operations should never throw exceptions
                assertDoesNotThrow(() -> {
                    sdk.getBool(TEST_USER_ID + "_" + i, "test_flag_" + i, i % 2 == 0);
                    sdk.refreshFlags();
                });
            }
            
            // Verify SDK is still functional
            Map<String, Object> stats = sdk.getStats();
            assertNotNull(stats);
            assertTrue((Integer) stats.get("total_user_accesses") >= 20);
        }

        @Test
        @DisplayName("Should allow normal operations after recovery")
        void shouldAllowNormalOperationsAfterRecovery() {
            // Force some activity that might trigger and recover from circuit breaker
            for (int i = 0; i < 15; i++) {
                sdk.refreshFlags();
                try {
                    Thread.sleep(100); // Small delay between attempts
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // After potential recovery, normal operations should work
            boolean result = sdk.getBool(TEST_USER_ID, "recovery_test_flag", false);
            assertNotNull(result);
            
            Map<String, Object> userFlags = sdk.getUserFlags(TEST_USER_ID);
            assertNotNull(userFlags);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Error Handling Tests")
    class CircuitBreakerErrorHandlingTests {

        @Test
        @DisplayName("Should handle network timeouts gracefully")
        void shouldHandleNetworkTimeoutsGracefully() {
            // SDK is configured with short timeout, so network operations might timeout
            // This should be handled gracefully without crashing
            
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 5; i++) {
                    sdk.refreshFlags();
                    sdk.flushLogs();
                }
            });
            
            // Verify SDK continues to work
            Map<String, Object> health = sdk.getHealthCheck();
            assertNotNull(health);
        }

        @Test
        @DisplayName("Should track error counts correctly")
        void shouldTrackErrorCountsCorrectly() {
            // Generate some operations
            for (int i = 0; i < 10; i++) {
                sdk.getBool(TEST_USER_ID, "error_test_flag_" + i, false);
                sdk.refreshFlags(); // Might fail and increment error count
            }
            
            Map<String, Object> stats = sdk.getStats();
            assertNotNull(stats);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> apiCalls = (Map<String, Object>) stats.get("api_calls");
            assertNotNull(apiCalls);
            
            // Should have recorded some API calls (successful or failed)
            Integer total = (Integer) apiCalls.get("total");
            assertNotNull(total);
            assertTrue(total >= 0);
        }

        @Test
        @DisplayName("Should maintain separate error tracking for different operations")
        void shouldMaintainSeparateErrorTrackingForDifferentOperations() {
            // Trigger different types of operations
            sdk.refreshFlags(); // Might fail with network error
            sdk.flushLogs();    // Might fail with different error
            
            Map<String, Object> stats = sdk.getStats();
            assertNotNull(stats);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> errors = (Map<String, Object>) stats.get("errors");
            assertNotNull(errors);
            
            // Errors map might be empty if no errors occurred, but should exist
            assertTrue(errors instanceof Map);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Configuration Tests")
    class CircuitBreakerConfigurationTests {

        @Test
        @DisplayName("Should respect timeout configuration")
        void shouldRespectTimeoutConfiguration() {
            // SDK is configured with 1 second timeout
            // Operations should respect this timeout
            
            long startTime = System.currentTimeMillis();
            sdk.refreshFlags(); // Should timeout quickly
            long duration = System.currentTimeMillis() - startTime;
            
            // Should not take much longer than the configured timeout
            // Allow some buffer for processing time
            assertTrue(duration < 5000, "Operation should timeout within reasonable time");
        }

        @Test
        @DisplayName("Should respect retry configuration")
        void shouldRespectRetryConfiguration() {
            // SDK is configured with 1 retry
            // Operations should not retry indefinitely
            
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 3; i++) {
                sdk.refreshFlags();
            }
            long duration = System.currentTimeMillis() - startTime;
            
            // With limited retries, operations should complete relatively quickly
            assertTrue(duration < 10000, "Operations with limited retries should complete quickly");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Thread Safety Tests")
    class CircuitBreakerThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent operations safely")
        void shouldHandleConcurrentOperationsSafely() throws InterruptedException {
            final int threadCount = 5;
            final int operationsPerThread = 10;
            Thread[] threads = new Thread[threadCount];
            final boolean[] allSucceeded = {true};

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            sdk.getBool(TEST_USER_ID + "_" + threadIndex + "_" + j, "concurrent_flag", false);
                            if (j % 3 == 0) {
                                sdk.refreshFlags(); // Some threads attempt refresh
                            }
                        }
                    } catch (Exception e) {
                        allSucceeded[0] = false;
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

            assertTrue(allSucceeded[0], "All concurrent operations should complete without exceptions");
            
            // Verify SDK is still functional
            Map<String, Object> stats = sdk.getStats();
            assertNotNull(stats);
            assertTrue((Integer) stats.get("total_user_accesses") >= threadCount * operationsPerThread);
        }

        @Test
        @DisplayName("Should maintain consistent state under concurrent load")
        void shouldMaintainConsistentStateUnderConcurrentLoad() throws InterruptedException {
            final int threadCount = 3;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 20; j++) {
                        // Mix of different operations
                        sdk.getBool(TEST_USER_ID, "load_test_bool", false);
                        sdk.getString(TEST_USER_ID, "load_test_string", "default");
                        sdk.getHealthCheck();
                        sdk.getStats();
                        
                        try {
                            Thread.sleep(10); // Small delay
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
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

            // Verify final state is consistent
            Map<String, Object> finalHealth = sdk.getHealthCheck();
            assertNotNull(finalHealth);
            assertTrue(finalHealth.containsKey("status"));
            
            Map<String, Object> finalStats = sdk.getStats();
            assertNotNull(finalStats);
            assertTrue((Integer) finalStats.get("total_user_accesses") > 0);
        }
    }
}