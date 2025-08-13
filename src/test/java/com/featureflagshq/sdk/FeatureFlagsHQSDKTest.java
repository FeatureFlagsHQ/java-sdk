package com.featureflagshq.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for FeatureFlagsHQSDK
 */
public class FeatureFlagsHQSDKTest {

    private FeatureFlagsHQSDK sdk;
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        // Create SDK in offline mode for testing
        sdk = new FeatureFlagsHQSDK.Builder()
                .clientId(TEST_CLIENT_ID)
                .clientSecret(TEST_CLIENT_SECRET)
                .environment("test")
                .offlineMode(true)  // Prevent network calls in tests
                .enableMetrics(false)  // Disable metrics for testing
                .build();
    }

    @AfterEach
    void tearDown() {
        if (sdk != null) {
            sdk.close();
        }
    }

    @Nested
    @DisplayName("SDK Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should create SDK with builder pattern")
        void shouldCreateSDKWithBuilder() {
            assertNotNull(sdk);
            
            Map<String, Object> health = sdk.getHealthCheck();
            assertEquals("healthy", health.get("status"));
            assertEquals("test", health.get("environment"));
            assertTrue((Boolean) health.get("offline_mode"));
        }

        @Test
        @DisplayName("Should throw exception for missing client ID")
        void shouldThrowExceptionForMissingClientId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientSecret(TEST_CLIENT_SECRET)
                        .build();
            });
        }

        @Test
        @DisplayName("Should throw exception for missing client secret")
        void shouldThrowExceptionForMissingClientSecret() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .build();
            });
        }

        @Test
        @DisplayName("Should use environment variables when not provided")
        void shouldUseEnvironmentVariables() {
            try (MockedStatic<System> mockedSystem = Mockito.mockStatic(System.class)) {
                mockedSystem.when(() -> System.getenv("FEATUREFLAGSHQ_CLIENT_ID"))
                           .thenReturn(TEST_CLIENT_ID);
                mockedSystem.when(() -> System.getenv("FEATUREFLAGSHQ_CLIENT_SECRET"))
                           .thenReturn(TEST_CLIENT_SECRET);
                
                FeatureFlagsHQSDK envSdk = new FeatureFlagsHQSDK.Builder()
                        .offlineMode(true)
                        .build();
                
                assertNotNull(envSdk);
                envSdk.close();
            }
        }
    }

    @Nested
    @DisplayName("Boolean Flag Tests")
    class BooleanFlagTests {

        @Test
        @DisplayName("Should return default value for non-existent flag")
        void shouldReturnDefaultValueForNonExistentFlag() {
            boolean result = sdk.getBool(TEST_USER_ID, "non_existent_flag", true);
            assertTrue(result);
            
            boolean resultFalse = sdk.getBool(TEST_USER_ID, "non_existent_flag", false);
            assertFalse(resultFalse);
        }

        @Test
        @DisplayName("Should return false as default when no default provided")
        void shouldReturnFalseAsDefaultWhenNoDefaultProvided() {
            boolean result = sdk.getBool(TEST_USER_ID, "non_existent_flag");
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle segments parameter")
        void shouldHandleSegmentsParameter() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("age", 25);
            segments.put("plan", "premium");
            
            boolean result = sdk.getBool(TEST_USER_ID, "premium_feature", false, segments);
            assertFalse(result); // In offline mode, returns default
        }
    }

    @Nested
    @DisplayName("String Flag Tests")
    class StringFlagTests {

        @Test
        @DisplayName("Should return default string value")
        void shouldReturnDefaultStringValue() {
            String result = sdk.getString(TEST_USER_ID, "welcome_message", "Hello World!");
            assertEquals("Hello World!", result);
        }

        @Test
        @DisplayName("Should return empty string as default when no default provided")
        void shouldReturnEmptyStringAsDefaultWhenNoDefaultProvided() {
            String result = sdk.getString(TEST_USER_ID, "non_existent_flag");
            assertEquals("", result);
        }
    }

    @Nested
    @DisplayName("Integer Flag Tests")
    class IntegerFlagTests {

        @Test
        @DisplayName("Should return default integer value")
        void shouldReturnDefaultIntegerValue() {
            int result = sdk.getInt(TEST_USER_ID, "max_retries", 5);
            assertEquals(5, result);
        }

        @Test
        @DisplayName("Should return zero as default when no default provided")
        void shouldReturnZeroAsDefaultWhenNoDefaultProvided() {
            int result = sdk.getInt(TEST_USER_ID, "non_existent_flag");
            assertEquals(0, result);
        }
    }

    @Nested
    @DisplayName("Float Flag Tests")
    class FloatFlagTests {

        @Test
        @DisplayName("Should return default float value")
        void shouldReturnDefaultFloatValue() {
            double result = sdk.getFloat(TEST_USER_ID, "discount_rate", 0.15);
            assertEquals(0.15, result, 0.001);
        }

        @Test
        @DisplayName("Should return zero as default when no default provided")
        void shouldReturnZeroAsDefaultWhenNoDefaultProvided() {
            double result = sdk.getFloat(TEST_USER_ID, "non_existent_flag");
            assertEquals(0.0, result, 0.001);
        }
    }

    @Nested
    @DisplayName("JSON Flag Tests")
    class JsonFlagTests {

        @Test
        @DisplayName("Should return default JSON value")
        void shouldReturnDefaultJsonValue() {
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("theme", "dark");
            defaultConfig.put("timeout", 30);
            
            Object result = sdk.getJson(TEST_USER_ID, "ui_config", defaultConfig);
            assertEquals(defaultConfig, result);
        }

        @Test
        @DisplayName("Should return empty map as default when no default provided")
        void shouldReturnEmptyMapAsDefaultWhenNoDefaultProvided() {
            Object result = sdk.getJson(TEST_USER_ID, "non_existent_flag");
            assertTrue(result instanceof Map);
            assertTrue(((Map<?, ?>) result).isEmpty());
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {

        @Test
        @DisplayName("Should check if flag is enabled for user")
        void shouldCheckIfFlagIsEnabledForUser() {
            boolean result = sdk.isFlagEnabledForUser(TEST_USER_ID, "new_feature");
            assertFalse(result); // In offline mode, returns false default
        }

        @Test
        @DisplayName("Should get multiple user flags")
        void shouldGetMultipleUserFlags() {
            Map<String, Object> userFlags = sdk.getUserFlags(TEST_USER_ID);
            assertNotNull(userFlags);
            assertTrue(userFlags.isEmpty()); // In offline mode, no flags available
        }

        @Test
        @DisplayName("Should get user flags with segments")
        void shouldGetUserFlagsWithSegments() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("age", 30);
            
            Map<String, Object> userFlags = sdk.getUserFlags(TEST_USER_ID, segments);
            assertNotNull(userFlags);
        }

        @Test
        @DisplayName("Should get specific user flags")
        void shouldGetSpecificUserFlags() {
            List<String> flagKeys = Arrays.asList("feature_a", "feature_b", "feature_c");
            Map<String, Object> userFlags = sdk.getUserFlags(TEST_USER_ID, null, flagKeys);
            assertNotNull(userFlags);
        }

        @Test
        @DisplayName("Should get all cached flags")
        void shouldGetAllCachedFlags() {
            Map<String, Map<String, Object>> allFlags = sdk.getAllFlags();
            assertNotNull(allFlags);
            assertTrue(allFlags.isEmpty()); // In offline mode, no flags cached
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void shouldHandleNullUserIdGracefully() {
            boolean result = sdk.getBool(null, "test_flag", true);
            assertTrue(result); // Should return default value
        }

        @Test
        @DisplayName("Should handle null flag name gracefully")
        void shouldHandleNullFlagNameGracefully() {
            boolean result = sdk.getBool(TEST_USER_ID, null, true);
            assertTrue(result); // Should return default value
        }

        @Test
        @DisplayName("Should handle empty user ID gracefully")
        void shouldHandleEmptyUserIdGracefully() {
            boolean result = sdk.getBool("", "test_flag", true);
            assertTrue(result); // Should return default value
        }

        @Test
        @DisplayName("Should handle empty flag name gracefully")
        void shouldHandleEmptyFlagNameGracefully() {
            boolean result = sdk.getBool(TEST_USER_ID, "", true);
            assertTrue(result); // Should return default value
        }

        @Test
        @DisplayName("Should handle invalid characters in user ID")
        void shouldHandleInvalidCharactersInUserId() {
            boolean result = sdk.getBool("user\nwith\nnewlines", "test_flag", true);
            assertTrue(result); // Should return default value
        }

        @Test
        @DisplayName("Should handle very long user ID")
        void shouldHandleVeryLongUserId() {
            String longUserId = "a".repeat(300); // Longer than MAX_USER_ID_LENGTH
            boolean result = sdk.getBool(longUserId, "test_flag", true);
            assertTrue(result); // Should return default value
        }
    }

    @Nested
    @DisplayName("Statistics and Health Tests")
    class StatisticsAndHealthTests {

        @Test
        @DisplayName("Should return SDK statistics")
        void shouldReturnSDKStatistics() {
            // Make some flag calls to generate stats
            sdk.getBool(TEST_USER_ID, "test_flag1", false);
            sdk.getString(TEST_USER_ID, "test_flag2", "default");
            sdk.getInt(TEST_USER_ID, "test_flag3", 0);

            Map<String, Object> stats = sdk.getStats();
            assertNotNull(stats);
            assertTrue(stats.containsKey("total_user_accesses"));
            assertTrue(stats.containsKey("unique_users_count"));
            assertTrue(stats.containsKey("unique_flags_count"));
            assertTrue(stats.containsKey("session_id"));
            assertTrue(stats.containsKey("cached_flags_count"));

            // Check that accesses were recorded
            assertTrue((Integer) stats.get("total_user_accesses") >= 3);
        }

        @Test
        @DisplayName("Should return health check information")
        void shouldReturnHealthCheckInformation() {
            Map<String, Object> health = sdk.getHealthCheck();
            assertNotNull(health);
            assertTrue(health.containsKey("status"));
            assertTrue(health.containsKey("sdk_version"));
            assertTrue(health.containsKey("environment"));
            assertTrue(health.containsKey("offline_mode"));
            assertTrue(health.containsKey("session_id"));
            assertTrue(health.containsKey("initialization_complete"));

            assertEquals("healthy", health.get("status"));
            assertEquals("test", health.get("environment"));
            assertTrue((Boolean) health.get("offline_mode"));
        }
    }

    @Nested
    @DisplayName("Manual Operations Tests")
    class ManualOperationsTests {

        @Test
        @DisplayName("Should handle refresh flags in offline mode")
        void shouldHandleRefreshFlagsInOfflineMode() {
            boolean result = sdk.refreshFlags();
            assertFalse(result); // Should return false in offline mode
        }

        @Test
        @DisplayName("Should handle flush logs in offline mode")
        void shouldHandleFlushLogsInOfflineMode() {
            boolean result = sdk.flushLogs();
            assertFalse(result); // Should return false in offline mode or with metrics disabled
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent flag evaluations")
        void shouldHandleConcurrentFlagEvaluations() throws InterruptedException {
            final int threadCount = 10;
            final int iterationsPerThread = 100;
            Thread[] threads = new Thread[threadCount];
            final boolean[] results = new boolean[threadCount * iterationsPerThread];
            final int[] index = {0};

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        String userId = "user_" + threadIndex + "_" + j;
                        String flagName = "flag_" + (j % 10);
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

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Verify no exceptions occurred and we got all results
            assertEquals(threadCount * iterationsPerThread, index[0]);
            
            // Check statistics were updated correctly
            Map<String, Object> stats = sdk.getStats();
            assertTrue((Integer) stats.get("total_user_accesses") >= threadCount * iterationsPerThread);
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should properly close SDK resources")
        void shouldProperlyCloseSDKResources() {
            FeatureFlagsHQSDK testSDK = new FeatureFlagsHQSDK.Builder()
                    .clientId(TEST_CLIENT_ID)
                    .clientSecret(TEST_CLIENT_SECRET)
                    .offlineMode(true)
                    .build();

            // Use the SDK
            testSDK.getBool(TEST_USER_ID, "test_flag", false);
            
            // Close should not throw exception
            assertDoesNotThrow(() -> testSDK.close());
            
            // Operations after close should handle gracefully
            boolean result = testSDK.getBool(TEST_USER_ID, "test_flag", true);
            assertTrue(result); // Should return default
        }

        @Test
        @DisplayName("Should work with try-with-resources")
        void shouldWorkWithTryWithResources() {
            assertDoesNotThrow(() -> {
                try (FeatureFlagsHQSDK testSDK = new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .clientSecret(TEST_CLIENT_SECRET)
                        .offlineMode(true)
                        .build()) {
                    
                    boolean result = testSDK.getBool(TEST_USER_ID, "test_flag", false);
                    assertFalse(result);
                }
            });
        }
    }

    @Nested
    @DisplayName("Production Configuration Tests")
    class ProductionConfigurationTests {

        @Test
        @DisplayName("Should validate production configuration")
        void shouldValidateProductionConfiguration() {
            Map<String, Object> config = new HashMap<>();
            config.put("api_base_url", "http://insecure.com"); // HTTP instead of HTTPS
            config.put("timeout", 2); // Too low timeout
            config.put("client_secret", "short"); // Weak secret

            List<String> warnings = FeatureFlagsHQSDK.validateProductionConfig(config);
            assertFalse(warnings.isEmpty());
            assertTrue(warnings.stream().anyMatch(w -> w.contains("HTTP instead of HTTPS")));
            assertTrue(warnings.stream().anyMatch(w -> w.contains("Timeout too low")));
            assertTrue(warnings.stream().anyMatch(w -> w.contains("weak")));
        }

        @Test
        @DisplayName("Should create production client with secure config")
        void shouldCreateProductionClientWithSecureConfig() {
            Map<String, Object> options = new HashMap<>();
            options.put("timeout", 30);
            options.put("enable_metrics", true);

            assertDoesNotThrow(() -> {
                FeatureFlagsHQSDK prodSDK = FeatureFlagsHQSDK.createProductionClient(
                        TEST_CLIENT_ID,
                        TEST_CLIENT_SECRET,
                        "production",
                        options
                );
                prodSDK.close();
            });
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() {
            // This test would be more meaningful with actual flag data
            // In offline mode, we're testing the default behavior
            Object result = sdk.getJson(TEST_USER_ID, "malformed_json_flag", Map.of("default", "value"));
            assertNotNull(result);
            assertTrue(result instanceof Map);
        }

        @Test
        @DisplayName("Should handle type conversion errors gracefully")
        void shouldHandleTypeConversionErrorsGracefully() {
            // Test integer conversion with invalid data
            int intResult = sdk.getInt(TEST_USER_ID, "invalid_int_flag", 42);
            assertEquals(42, intResult);

            // Test float conversion with invalid data
            double floatResult = sdk.getFloat(TEST_USER_ID, "invalid_float_flag", 3.14);
            assertEquals(3.14, floatResult, 0.001);

            // Test boolean conversion with invalid data
            boolean boolResult = sdk.getBool(TEST_USER_ID, "invalid_bool_flag", true);
            assertTrue(boolResult);
        }
    }

    @Nested
    @DisplayName("Callback Tests")
    class CallbackTests {

        @Test
        @DisplayName("Should handle flag change callbacks")
        void shouldHandleFlagChangeCallbacks() {
            final boolean[] callbackInvoked = {false};
            final String[] callbackFlagName = {null};
            final Object[] callbackOldValue = {null};
            final Object[] callbackNewValue = {null};

            FeatureFlagsHQSDK callbackSDK = new FeatureFlagsHQSDK.Builder()
                    .clientId(TEST_CLIENT_ID)
                    .clientSecret(TEST_CLIENT_SECRET)
                    .offlineMode(true)
                    .onFlagChange(change -> {
                        callbackInvoked[0] = true;
                        callbackFlagName[0] = change.flagName;
                        callbackOldValue[0] = change.oldValue;
                        callbackNewValue[0] = change.newValue;
                    })
                    .build();

            try {
                // In offline mode, callbacks won't be triggered by actual flag changes
                // This test verifies the callback can be set without errors
                assertNotNull(callbackSDK);
                
                // Use the SDK normally
                boolean result = callbackSDK.getBool(TEST_USER_ID, "test_flag", false);
                assertFalse(result);
                
            } finally {
                callbackSDK.close();
            }
        }

        @Test
        @DisplayName("Should handle callback exceptions gracefully")
        void shouldHandleCallbackExceptionsGracefully() {
            FeatureFlagsHQSDK callbackSDK = new FeatureFlagsHQSDK.Builder()
                    .clientId(TEST_CLIENT_ID)
                    .clientSecret(TEST_CLIENT_SECRET)
                    .offlineMode(true)
                    .onFlagChange(change -> {
                        throw new RuntimeException("Callback error");
                    })
                    .build();

            try {
                // SDK should continue working even if callback throws exception
                assertDoesNotThrow(() -> {
                    boolean result = callbackSDK.getBool(TEST_USER_ID, "test_flag", false);
                    assertFalse(result);
                });
            } finally {
                callbackSDK.close();
            }
        }
    }
}
            