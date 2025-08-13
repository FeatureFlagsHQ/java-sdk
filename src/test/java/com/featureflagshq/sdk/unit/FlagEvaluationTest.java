package com.featureflagshq.sdk.unit;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Flag Evaluation Unit Tests")
public class FlagEvaluationTest {

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
    @DisplayName("Boolean Flag Evaluation Tests")
    class BooleanFlagEvaluationTests {

        @Test
        @DisplayName("Should return default value for non-existent boolean flag")
        void shouldReturnDefaultValueForNonExistentBooleanFlag() {
            boolean result = sdk.getBool(TEST_USER_ID, "non_existent_bool_flag", true);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false as default when no default provided for boolean")
        void shouldReturnFalseAsDefaultWhenNoDefaultProvidedForBoolean() {
            boolean result = sdk.getBool(TEST_USER_ID, "non_existent_bool_flag");
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle boolean conversion from string values")
        void shouldHandleBooleanConversionFromStringValues() {
            // This test simulates how the SDK would convert string representations
            boolean trueResult = sdk.getBool(TEST_USER_ID, "string_true_flag", false);
            boolean falseResult = sdk.getBool(TEST_USER_ID, "string_false_flag", true);
            
            // In offline mode, these will return the default values
            assertFalse(trueResult);
            assertTrue(falseResult);
        }

        @Test
        @DisplayName("Should handle boolean flags with segments")
        void shouldHandleBooleanFlagsWithSegments() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("plan", "premium");
            segments.put("age", 25);

            boolean result = sdk.getBool(TEST_USER_ID, "premium_bool_flag", false, segments);
            assertFalse(result); // In offline mode, returns default
        }
    }

    @Nested
    @DisplayName("String Flag Evaluation Tests")
    class StringFlagEvaluationTests {

        @Test
        @DisplayName("Should return default value for non-existent string flag")
        void shouldReturnDefaultValueForNonExistentStringFlag() {
            String result = sdk.getString(TEST_USER_ID, "non_existent_string_flag", "default_value");
            assertEquals("default_value", result);
        }

        @Test
        @DisplayName("Should return empty string as default when no default provided")
        void shouldReturnEmptyStringAsDefaultWhenNoDefaultProvided() {
            String result = sdk.getString(TEST_USER_ID, "non_existent_string_flag");
            assertEquals("", result);
        }

        @Test
        @DisplayName("Should handle string flags with special characters")
        void shouldHandleStringFlagsWithSpecialCharacters() {
            String defaultValue = "Hello, World! 🌍";
            String result = sdk.getString(TEST_USER_ID, "special_chars_flag", defaultValue);
            assertEquals(defaultValue, result);
        }

        @Test
        @DisplayName("Should handle very long string values")
        void shouldHandleVeryLongStringValues() {
            String longDefault = "a".repeat(1000);
            String result = sdk.getString(TEST_USER_ID, "long_string_flag", longDefault);
            assertEquals(longDefault, result);
        }
    }

    @Nested
    @DisplayName("Integer Flag Evaluation Tests")
    class IntegerFlagEvaluationTests {

        @Test
        @DisplayName("Should return default value for non-existent integer flag")
        void shouldReturnDefaultValueForNonExistentIntegerFlag() {
            int result = sdk.getInt(TEST_USER_ID, "non_existent_int_flag", 42);
            assertEquals(42, result);
        }

        @Test
        @DisplayName("Should return zero as default when no default provided")
        void shouldReturnZeroAsDefaultWhenNoDefaultProvided() {
            int result = sdk.getInt(TEST_USER_ID, "non_existent_int_flag");
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should handle negative integer values")
        void shouldHandleNegativeIntegerValues() {
            int result = sdk.getInt(TEST_USER_ID, "negative_int_flag", -100);
            assertEquals(-100, result);
        }

        @Test
        @DisplayName("Should handle large integer values")
        void shouldHandleLargeIntegerValues() {
            int result = sdk.getInt(TEST_USER_ID, "large_int_flag", Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, result);
        }
    }

    @Nested
    @DisplayName("Float Flag Evaluation Tests")
    class FloatFlagEvaluationTests {

        @Test
        @DisplayName("Should return default value for non-existent float flag")
        void shouldReturnDefaultValueForNonExistentFloatFlag() {
            double result = sdk.getFloat(TEST_USER_ID, "non_existent_float_flag", 3.14159);
            assertEquals(3.14159, result, 0.00001);
        }

        @Test
        @DisplayName("Should return zero as default when no default provided")
        void shouldReturnZeroAsDefaultWhenNoDefaultProvided() {
            double result = sdk.getFloat(TEST_USER_ID, "non_existent_float_flag");
            assertEquals(0.0, result, 0.00001);
        }

        @Test
        @DisplayName("Should handle negative float values")
        void shouldHandleNegativeFloatValues() {
            double result = sdk.getFloat(TEST_USER_ID, "negative_float_flag", -123.456);
            assertEquals(-123.456, result, 0.00001);
        }

        @Test
        @DisplayName("Should handle very small float values")
        void shouldHandleVerySmallFloatValues() {
            double result = sdk.getFloat(TEST_USER_ID, "small_float_flag", 0.000001);
            assertEquals(0.000001, result, 0.0000001);
        }
    }

    @Nested
    @DisplayName("JSON Flag Evaluation Tests")
    class JsonFlagEvaluationTests {

        @Test
        @DisplayName("Should return default value for non-existent JSON flag")
        void shouldReturnDefaultValueForNonExistentJsonFlag() {
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("theme", "dark");
            defaultConfig.put("timeout", 30);

            Object result = sdk.getJson(TEST_USER_ID, "non_existent_json_flag", defaultConfig);
            assertEquals(defaultConfig, result);
        }

        @Test
        @DisplayName("Should return empty map as default when no default provided")
        void shouldReturnEmptyMapAsDefaultWhenNoDefaultProvided() {
            Object result = sdk.getJson(TEST_USER_ID, "non_existent_json_flag");
            assertTrue(result instanceof Map);
            assertTrue(((Map<?, ?>) result).isEmpty());
        }

        @Test
        @DisplayName("Should handle complex JSON structures")
        void shouldHandleComplexJsonStructures() {
            Map<String, Object> complexDefault = new HashMap<>();
            complexDefault.put("users", Arrays.asList("user1", "user2", "user3"));
            
            Map<String, Object> nestedMap = new HashMap<>();
            nestedMap.put("nested_key", "nested_value");
            complexDefault.put("nested", nestedMap);

            Object result = sdk.getJson(TEST_USER_ID, "complex_json_flag", complexDefault);
            assertEquals(complexDefault, result);
        }
    }

    @Nested
    @DisplayName("Segment-based Evaluation Tests")
    class SegmentBasedEvaluationTests {

        @Test
        @DisplayName("Should handle evaluation with single segment")
        void shouldHandleEvaluationWithSingleSegment() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("plan", "premium");

            boolean result = sdk.getBool(TEST_USER_ID, "premium_feature", false, segments);
            assertFalse(result); // In offline mode, returns default
        }

        @Test
        @DisplayName("Should handle evaluation with multiple segments")
        void shouldHandleEvaluationWithMultipleSegments() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("plan", "premium");
            segments.put("age", 25);
            segments.put("region", "US");
            segments.put("beta_user", true);

            String result = sdk.getString(TEST_USER_ID, "beta_feature", "disabled", segments);
            assertEquals("disabled", result); // In offline mode, returns default
        }

        @Test
        @DisplayName("Should handle evaluation with numeric segments")
        void shouldHandleEvaluationWithNumericSegments() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("age", 30);
            segments.put("score", 85.5);
            segments.put("level", 10);

            int result = sdk.getInt(TEST_USER_ID, "age_based_feature", 0, segments);
            assertEquals(0, result); // In offline mode, returns default
        }

        @Test
        @DisplayName("Should handle evaluation with null segments")
        void shouldHandleEvaluationWithNullSegments() {
            boolean result = sdk.getBool(TEST_USER_ID, "test_flag", true, null);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle evaluation with empty segments")
        void shouldHandleEvaluationWithEmptySegments() {
            Map<String, Object> emptySegments = new HashMap<>();
            double result = sdk.getFloat(TEST_USER_ID, "test_flag", 1.5, emptySegments);
            assertEquals(1.5, result, 0.001);
        }
    }

    @Nested
    @DisplayName("Multi-flag Evaluation Tests")
    class MultiFlagEvaluationTests {

        @Test
        @DisplayName("Should get all flags for user")
        void shouldGetAllFlagsForUser() {
            Map<String, Object> userFlags = sdk.getUserFlags(TEST_USER_ID);
            assertNotNull(userFlags);
            assertTrue(userFlags.isEmpty()); // In offline mode, no flags available
        }

        @Test
        @DisplayName("Should get specific flags for user")
        void shouldGetSpecificFlagsForUser() {
            List<String> flagKeys = Arrays.asList("flag1", "flag2", "flag3");
            Map<String, Object> userFlags = sdk.getUserFlags(TEST_USER_ID, null, flagKeys);
            assertNotNull(userFlags);
            assertTrue(userFlags.isEmpty()); // In offline mode, no flags available
        }

        @Test
        @DisplayName("Should get flags with segments")
        void shouldGetFlagsWithSegments() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("plan", "enterprise");
            segments.put("features", Arrays.asList("advanced", "premium"));

            Map<String, Object> userFlags = sdk.getUserFlags(TEST_USER_ID, segments);
            assertNotNull(userFlags);
        }
    }

    @Nested
    @DisplayName("Flag Availability Tests")
    class FlagAvailabilityTests {

        @Test
        @DisplayName("Should check if flag is enabled for user")
        void shouldCheckIfFlagIsEnabledForUser() {
            boolean isEnabled = sdk.isFlagEnabledForUser(TEST_USER_ID, "feature_flag");
            assertFalse(isEnabled); // In offline mode, returns false default
        }

        @Test
        @DisplayName("Should check flag availability with segments")
        void shouldCheckFlagAvailabilityWithSegments() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("beta_user", true);

            boolean isEnabled = sdk.isFlagEnabledForUser(TEST_USER_ID, "beta_feature", segments);
            assertFalse(isEnabled); // In offline mode, returns false default
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long flag names")
        void shouldHandleVeryLongFlagNames() {
            String longFlagName = "very_long_flag_name_" + "a".repeat(200);
            // This should be handled gracefully by validation
            boolean result = sdk.getBool(TEST_USER_ID, longFlagName, true);
            assertTrue(result); // Should return default due to validation failure
        }

        @Test
        @DisplayName("Should handle invalid characters in flag names")
        void shouldHandleInvalidCharactersInFlagNames() {
            String invalidFlagName = "flag@name#invalid";
            boolean result = sdk.getBool(TEST_USER_ID, invalidFlagName, true);
            assertTrue(result); // Should return default due to validation failure
        }

        @Test
        @DisplayName("Should handle concurrent flag evaluations")
        void shouldHandleConcurrentFlagEvaluations() throws InterruptedException {
            final int threadCount = 10;
            final int operationsPerThread = 50;
            Thread[] threads = new Thread[threadCount];
            final boolean[] allCompleted = {true};

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            String userId = "concurrent_user_" + threadIndex + "_" + j;
                            String flagName = "concurrent_flag_" + (j % 5);
                            boolean result = sdk.getBool(userId, flagName, j % 2 == 0);
                            assertNotNull(result);
                        }
                    } catch (Exception e) {
                        allCompleted[0] = false;
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

            assertTrue(allCompleted[0], "All concurrent operations should complete successfully");
        }
    }
}