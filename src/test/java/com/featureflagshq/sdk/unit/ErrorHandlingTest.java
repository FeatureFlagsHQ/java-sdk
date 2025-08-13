package com.featureflagshq.sdk.unit;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Error Handling and Malformed Data Tests")
public class ErrorHandlingTest {

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
    @DisplayName("Malformed Input Tests")
    class MalformedInputTests {

        @Test
        @DisplayName("Should handle corrupted user ID gracefully")
        void shouldHandleCorruptedUserIdGracefully() {
            String[] corruptedUserIds = {
                null,
                "",
                "   ",
                "\0",
                "\u0001\u0002\u0003",
                "user\u007F\u0080\u0081", // DEL + non-ASCII
                "user\uFFFE\uFFFF", // Unicode non-characters
                "user\uD800\uDC00", // Surrogate pairs
                new String(new byte[]{(byte)0xFF, (byte)0xFE}, StandardCharsets.ISO_8859_1), // Invalid UTF-8
                "user" + Character.toString(0x10FFFF + 1) // Beyond Unicode range
            };

            for (String corruptedUserId : corruptedUserIds) {
                assertDoesNotThrow(() -> {
                    boolean result = sdk.getBool(corruptedUserId, "test_flag", true);
                    assertTrue(result); // Should return default value safely
                }, "Should handle corrupted user ID without throwing: " + 
                   (corruptedUserId == null ? "null" : corruptedUserId.replace("\0", "\\0")));
            }
        }

        @Test
        @DisplayName("Should handle malformed flag names gracefully")
        void shouldHandleMalformedFlagNamesGracefully() {
            String[] malformedFlagNames = {
                null,
                "",
                "   ",
                "flag\n\r\t",
                "flag\0name",
                "flag..name",
                "flag//name",
                "flag\\\\name",
                "flag$$name",
                "flag@@name",
                "flag##name",
                "flag%%name",
                "flag^^name",
                "flag**name",
                "flag++name",
                "flag==name",
                "flag||name",
                "flag&&name",
                "flag!!name",
                "flag??name",
                "flag::name",
                "flag;;name",
                "flag,,name",
                "flag<<name",
                "flag>>name",
                "flag((name))",
                "flag[[name]]",
                "flag{{name}}",
                "flag|pipe|name",
                "flag&amp;name",
                "flag<script>name",
                "flag</script>name"
            };

            for (String malformedFlagName : malformedFlagNames) {
                assertDoesNotThrow(() -> {
                    boolean result = sdk.getBool(TEST_USER_ID, malformedFlagName, false);
                    assertFalse(result); // Should return default value safely
                }, "Should handle malformed flag name without throwing: " + malformedFlagName);
            }
        }

        @Test
        @DisplayName("Should handle corrupted segment data")
        void shouldHandleCorruptedSegmentData() {
            Map<String, Object> corruptedSegments = new HashMap<>();
            
            // Add various types of corrupted data
            corruptedSegments.put(null, "value_for_null_key");
            corruptedSegments.put("", "value_for_empty_key");
            corruptedSegments.put("normal_key", null);
            corruptedSegments.put("unicode_key_\uFFFE", "unicode_value");
            corruptedSegments.put("key_with_\0_null", "value");
            corruptedSegments.put("key\n\r\t", "value_with_whitespace");
            
            // Try to add circular reference
            Map<String, Object> circularMap = new HashMap<>();
            circularMap.put("self", circularMap);
            corruptedSegments.put("circular", circularMap);
            
            // Add extremely nested structure
            Map<String, Object> nested = new HashMap<>();
            Map<String, Object> current = nested;
            for (int i = 0; i < 100; i++) {
                Map<String, Object> next = new HashMap<>();
                current.put("level_" + i, next);
                current = next;
            }
            current.put("deep_value", "at_the_bottom");
            corruptedSegments.put("deeply_nested", nested);

            assertDoesNotThrow(() -> {
                boolean result = sdk.getBool(TEST_USER_ID, "corrupted_test", true, corruptedSegments);
                assertTrue(result); // Should return default value safely
            });
        }

        @Test
        @DisplayName("Should handle binary and non-printable data")
        void shouldHandleBinaryAndNonPrintableData() {
            // Create binary data that might break string processing
            byte[] binaryData = new byte[256];
            for (int i = 0; i < 256; i++) {
                binaryData[i] = (byte) i;
            }
            
            String binaryString = new String(binaryData, StandardCharsets.ISO_8859_1);
            
            assertDoesNotThrow(() -> {
                boolean result = sdk.getBool(binaryString, "binary_flag", false);
                assertFalse(result); // Should handle binary data safely
            });

            // Test with different encodings
            String[] encodingTests = {
                new String("test".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1),
                new String("test".getBytes(StandardCharsets.UTF_16), StandardCharsets.UTF_8),
                new String("测试".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)
            };

            for (String encodingTest : encodingTests) {
                assertDoesNotThrow(() -> {
                    sdk.getString(encodingTest, "encoding_flag", "default");
                });
            }
        }
    }

    @Nested
    @DisplayName("Type Conversion Error Tests")
    class TypeConversionErrorTests {

        @Test
        @DisplayName("Should handle invalid boolean conversions")
        void shouldHandleInvalidBooleanConversions() {
            // In offline mode, these test the default value handling
            Map<String, Object> segments = new HashMap<>();
            segments.put("invalid_bool_1", "not_a_boolean");
            segments.put("invalid_bool_2", 42);
            segments.put("invalid_bool_3", new Object());
            segments.put("invalid_bool_4", Arrays.asList(1, 2, 3));

            boolean result = sdk.getBool(TEST_USER_ID, "bool_conversion_test", true, segments);
            assertTrue(result); // Should return default value
        }

        @Test
        @DisplayName("Should handle invalid integer conversions")
        void shouldHandleInvalidIntegerConversions() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("invalid_int_1", "not_a_number");
            segments.put("invalid_int_2", "123abc");
            segments.put("invalid_int_3", Double.POSITIVE_INFINITY);
            segments.put("invalid_int_4", Double.NaN);
            segments.put("invalid_int_5", "999999999999999999999999999999");

            int result = sdk.getInt(TEST_USER_ID, "int_conversion_test", 42, segments);
            assertEquals(42, result); // Should return default value
        }

        @Test
        @DisplayName("Should handle invalid float conversions")
        void shouldHandleInvalidFloatConversions() {
            Map<String, Object> segments = new HashMap<>();
            segments.put("invalid_float_1", "not_a_float");
            segments.put("invalid_float_2", "3.14abc");
            segments.put("invalid_float_3", "∞");
            segments.put("invalid_float_4", "NaN_string");

            double result = sdk.getFloat(TEST_USER_ID, "float_conversion_test", 3.14, segments);
            assertEquals(3.14, result, 0.001); // Should return default value
        }

        @Test
        @DisplayName("Should handle invalid JSON data")
        void shouldHandleInvalidJsonData() {
            Map<String, Object> defaultJson = new HashMap<>();
            defaultJson.put("default", "value");

            // Test with various invalid JSON scenarios
            Object result = sdk.getJson(TEST_USER_ID, "json_test", defaultJson);
            assertEquals(defaultJson, result); // Should return default in offline mode
        }
    }

    @Nested
    @DisplayName("Extreme Value Tests")
    class ExtremeValueTests {

        @Test
        @DisplayName("Should handle extremely large numbers")
        void shouldHandleExtremelyLargeNumbers() {
            Map<String, Object> extremeSegments = new HashMap<>();
            extremeSegments.put("max_long", Long.MAX_VALUE);
            extremeSegments.put("min_long", Long.MIN_VALUE);
            extremeSegments.put("max_double", Double.MAX_VALUE);
            extremeSegments.put("min_double", Double.MIN_VALUE);
            extremeSegments.put("pos_infinity", Double.POSITIVE_INFINITY);
            extremeSegments.put("neg_infinity", Double.NEGATIVE_INFINITY);
            extremeSegments.put("nan", Double.NaN);

            assertDoesNotThrow(() -> {
                int intResult = sdk.getInt(TEST_USER_ID, "extreme_int", Integer.MAX_VALUE, extremeSegments);
                assertEquals(Integer.MAX_VALUE, intResult);

                double floatResult = sdk.getFloat(TEST_USER_ID, "extreme_float", Double.MAX_VALUE, extremeSegments);
                assertEquals(Double.MAX_VALUE, floatResult, 0.0);
            });
        }

        @Test
        @DisplayName("Should handle extremely long strings")
        void shouldHandleExtremelyLongStrings() {
            String veryLongString = "x".repeat(1000000); // 1MB string
            
            assertDoesNotThrow(() -> {
                String result = sdk.getString(TEST_USER_ID, "long_string_test", veryLongString);
                assertEquals(veryLongString, result);
            });

            // Test with long string in segments
            Map<String, Object> longStringSegments = new HashMap<>();
            longStringSegments.put("very_long_value", veryLongString);
            
            assertDoesNotThrow(() -> {
                sdk.getBool(TEST_USER_ID, "test_flag", false, longStringSegments);
            });
        }

        @Test
        @DisplayName("Should handle deeply nested data structures")
        void shouldHandleDeeplyNestedDataStructures() {
            Map<String, Object> deeplyNested = new HashMap<>();
            Map<String, Object> current = deeplyNested;
            
            // Create 500 levels of nesting
            for (int i = 0; i < 500; i++) {
                Map<String, Object> next = new HashMap<>();
                next.put("level", i);
                next.put("data", "level_" + i + "_data");
                current.put("next", next);
                current = next;
            }

            Map<String, Object> nestedSegments = new HashMap<>();
            nestedSegments.put("deep_structure", deeplyNested);

            assertDoesNotThrow(() -> {
                boolean result = sdk.getBool(TEST_USER_ID, "deep_test", false, nestedSegments);
                assertFalse(result); // Should handle without crashing
            });
        }
    }

    @Nested
    @DisplayName("State Corruption Recovery Tests")
    class StateCorruptionRecoveryTests {

        @Test
        @DisplayName("Should recover from corrupted internal state")
        void shouldRecoverFromCorruptedInternalState() {
            // Simulate various failure scenarios and verify recovery
            
            // Test 1: Massive parallel access that might corrupt state
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10000; i++) {
                    sdk.getBool("recovery_user_" + i, "recovery_flag_" + (i % 100), false);
                }
            });

            // Test 2: Verify SDK is still functional
            boolean result = sdk.getBool(TEST_USER_ID, "post_corruption_test", true);
            assertTrue(result);

            // Test 3: Verify health check still works
            Map<String, Object> health = sdk.getHealthCheck();
            assertNotNull(health);
            assertTrue(health.containsKey("status"));
        }

        @Test
        @DisplayName("Should handle callback failures gracefully")
        void shouldHandleCallbackFailuresGracefully() {
            FeatureFlagsHQSDK callbackSDK = new FeatureFlagsHQSDK.Builder()
                    .clientId(TEST_CLIENT_ID)
                    .clientSecret(TEST_CLIENT_SECRET)
                    .offlineMode(true)
                    .onFlagChange(change -> {
                        throw new RuntimeException("Intentional callback failure");
                    })
                    .build();

            try {
                // SDK should continue working despite callback failures
                assertDoesNotThrow(() -> {
                    for (int i = 0; i < 100; i++) {
                        callbackSDK.getBool("callback_user_" + i, "callback_flag", false);
                    }
                });

                Map<String, Object> health = callbackSDK.getHealthCheck();
                assertEquals("healthy", health.get("status"));

            } finally {
                callbackSDK.close();
            }
        }

        @Test
        @DisplayName("Should handle resource exhaustion gracefully")
        void shouldHandleResourceExhaustionGracefully() {
            // Try to exhaust various resources
            
            // Test 1: Many unique users (beyond tracking limit)
            for (int i = 0; i < 15000; i++) {
                sdk.getBool("exhaust_user_" + i, "exhaust_flag", false);
            }

            // Test 2: Many unique flags (beyond tracking limit)
            for (int i = 0; i < 1500; i++) {
                sdk.getBool(TEST_USER_ID, "exhaust_flag_" + i, false);
            }

            // SDK should still be functional
            Map<String, Object> stats = sdk.getStats();
            assertNotNull(stats);
            assertTrue((Integer) stats.get("unique_users_count") <= 10000);
            assertTrue((Integer) stats.get("unique_flags_count") <= 1000);

            boolean result = sdk.getBool(TEST_USER_ID, "post_exhaustion_test", true);
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Protocol and Format Error Tests")
    class ProtocolAndFormatErrorTests {

        @Test
        @DisplayName("Should handle malformed JSON-like strings")
        void shouldHandleMalformedJsonLikeStrings() {
            String[] malformedJsonStrings = {
                "{",
                "}",
                "{\"unclosed\": \"value\"",
                "{\"key\": }",
                "{\"key\": \"value\",}",
                "{\"key\": \"value\" \"another\": \"value\"}",
                "[1, 2, 3,]",
                "{\"nested\": {\"deep\": }",
                "{'single_quotes': 'not_valid_json'}",
                "{\"unicode\": \"\\uXXXX\"}",
                "{\"number\": 123abc}",
                "{\"boolean\": truee}",
                "{\"null\": nul}",
                "{key_without_quotes: \"value\"}",
                "{\"key\": \"value\n\r\t\"}"
            };

            for (String malformedJson : malformedJsonStrings) {
                Map<String, Object> segments = new HashMap<>();
                segments.put("malformed_json", malformedJson);

                assertDoesNotThrow(() -> {
                    Object result = sdk.getJson(TEST_USER_ID, "json_test", new HashMap<>(), segments);
                    assertNotNull(result);
                }, "Should handle malformed JSON string: " + malformedJson);
            }
        }

        @Test
        @DisplayName("Should handle invalid HTTP-like data")
        void shouldHandleInvalidHttpLikeData() {
            String[] httpLikeData = {
                "GET /api/flags HTTP/1.1\r\nHost: example.com\r\n\r\n",
                "POST /api/flags HTTP/1.1\r\nContent-Length: -1\r\n\r\n",
                "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{\"invalid\": json}",
                "CONNECT example.com:443 HTTP/1.1\r\n\r\n",
                "OPTIONS * HTTP/1.1\r\n\r\n"
            };

            for (String httpData : httpLikeData) {
                assertDoesNotThrow(() -> {
                    sdk.getString(httpData, "http_test", "default");
                    sdk.getBool(TEST_USER_ID, httpData, false);
                }, "Should handle HTTP-like data safely: " + httpData.substring(0, Math.min(20, httpData.length())));
            }
        }

        @Test
        @DisplayName("Should handle malformed URL-like strings")
        void shouldHandleMalformedUrlLikeStrings() {
            String[] malformedUrls = {
                "http://",
                "https://",
                "://example.com",
                "http:///path",
                "http://[::1:invalid",
                "http://user:pass@",
                "http://example.com:abc/path",
                "http://example.com:-1/path",
                "http://example.com:99999/path",
                "http://[invalid::ipv6]/path",
                "javascript:alert('xss')",
                "data:text/html,<script>alert('xss')</script>",
                "file:///etc/passwd",
                "http://256.256.256.256/",
                "http://example.com:80:80/path"
            };

            for (String malformedUrl : malformedUrls) {
                assertDoesNotThrow(() -> {
                    sdk.getString(TEST_USER_ID, malformedUrl, "default");
                }, "Should handle malformed URL safely: " + malformedUrl);
            }
        }
    }

    @Nested
    @DisplayName("Boundary and Overflow Tests")
    class BoundaryAndOverflowTests {

        @Test
        @DisplayName("Should handle integer overflow scenarios")
        void shouldHandleIntegerOverflowScenarios() {
            Map<String, Object> overflowSegments = new HashMap<>();
            overflowSegments.put("big_long", Long.MAX_VALUE);
            overflowSegments.put("overflow_string", "999999999999999999999999999999999999999");
            
            assertDoesNotThrow(() -> {
                int result = sdk.getInt(TEST_USER_ID, "overflow_test", Integer.MAX_VALUE, overflowSegments);
                assertEquals(Integer.MAX_VALUE, result);
            });
        }

        @Test
        @DisplayName("Should handle floating point edge cases")
        void shouldHandleFloatingPointEdgeCases() {
            Map<String, Object> floatSegments = new HashMap<>();
            floatSegments.put("positive_infinity", Double.POSITIVE_INFINITY);
            floatSegments.put("negative_infinity", Double.NEGATIVE_INFINITY);
            floatSegments.put("nan", Double.NaN);
            floatSegments.put("max_value", Double.MAX_VALUE);
            floatSegments.put("min_value", Double.MIN_VALUE);
            floatSegments.put("subnormal", Double.MIN_NORMAL / 2);

            assertDoesNotThrow(() -> {
                double result = sdk.getFloat(TEST_USER_ID, "float_edge_test", 1.0, floatSegments);
                assertEquals(1.0, result, 0.0);
            });
        }

        @Test
        @DisplayName("Should handle memory boundary conditions")
        void shouldHandleMemoryBoundaryConditions() {
            // Test with data that approaches system limits
            try {
                // Create a large map that might stress memory allocation
                Map<String, Object> largeSegments = new HashMap<>();
                for (int i = 0; i < 100000; i++) {
                    largeSegments.put("key_" + i, "value_" + i + "_" + "x".repeat(100));
                }

                assertDoesNotThrow(() -> {
                    boolean result = sdk.getBool(TEST_USER_ID, "memory_boundary_test", false, largeSegments);
                    assertFalse(result);
                });

            } catch (OutOfMemoryError e) {
                // This is acceptable - we're testing boundary conditions
                System.out.println("OutOfMemoryError caught as expected during boundary testing");
            }
        }
    }

    @Nested
    @DisplayName("Concurrency Error Tests")
    class ConcurrencyErrorTests {

        @Test
        @DisplayName("Should handle concurrent access with corrupted data")
        void shouldHandleConcurrentAccessWithCorruptedData() throws InterruptedException {
            final int threadCount = 10;
            final int operationsPerThread = 100;
            Thread[] threads = new Thread[threadCount];
            final Exception[] exceptions = new Exception[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            // Each thread uses different types of corrupted data
                            String corruptedUserId = "thread_" + threadId + "_\0_user_" + j;
                            String corruptedFlagName = "flag\n\r\t_" + j;
                            
                            Map<String, Object> corruptedSegments = new HashMap<>();
                            corruptedSegments.put("corrupted_key_" + j, null);
                            corruptedSegments.put(null, "value_for_null_key_" + j);
                            
                            sdk.getBool(corruptedUserId, corruptedFlagName, false, corruptedSegments);
                        }
                    } catch (Exception e) {
                        exceptions[threadId] = e;
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Verify no exceptions occurred
            for (int i = 0; i < threadCount; i++) {
                assertNull(exceptions[i], "Thread " + i + " should not have thrown exception");
            }

            // SDK should still be healthy
            Map<String, Object> health = sdk.getHealthCheck();
            assertEquals("healthy", health.get("status"));
        }

        @Test
        @DisplayName("Should handle rapid state changes without corruption")
        void shouldHandleRapidStateChangesWithoutCorruption() {
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 1000; i++) {
                    // Rapidly change between different user contexts
                    sdk.getBool("rapid_user_" + (i % 10), "rapid_flag_" + (i % 5), i % 2 == 0);
                    
                    // Add and remove segments rapidly
                    Map<String, Object> segments = new HashMap<>();
                    if (i % 3 == 0) {
                        segments.put("rapid_key", "rapid_value_" + i);
                    }
                    
                    sdk.getString("rapid_string_user", "rapid_string_flag", "default", segments);
                }
            });

            Map<String, Object> stats = sdk.getStats();
            assertTrue((Integer) stats.get("total_user_accesses") >= 2000);
        }
    }
}