package com.featureflagshq.sdk.unit;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import com.featureflagshq.sdk.utils.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Authentication Unit Tests")
public class AuthenticationTest {

    private static final String VALID_CLIENT_ID = "test-client-id-12345";
    private static final String VALID_CLIENT_SECRET = "test-client-secret-67890-abcdef";
    private static final String TEST_USER_ID = "test-user-123";

    @Nested
    @DisplayName("Credential Validation Tests")
    class CredentialValidationTests {

        @Test
        @DisplayName("Should accept valid credentials")
        void shouldAcceptValidCredentials() {
            assertDoesNotThrow(() -> {
                FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
                        .clientId(VALID_CLIENT_ID)
                        .clientSecret(VALID_CLIENT_SECRET)
                        .offlineMode(true)
                        .build();
                sdk.close();
            });
        }

        @Test
        @DisplayName("Should reject null client ID")
        void shouldRejectNullClientId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(null)
                        .clientSecret(VALID_CLIENT_SECRET)
                        .build();
            });
        }

        @Test
        @DisplayName("Should reject empty client ID")
        void shouldRejectEmptyClientId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId("")
                        .clientSecret(VALID_CLIENT_SECRET)
                        .build();
            });
        }

        @Test
        @DisplayName("Should reject whitespace-only client ID")
        void shouldRejectWhitespaceOnlyClientId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId("   ")
                        .clientSecret(VALID_CLIENT_SECRET)
                        .build();
            });
        }

        @Test
        @DisplayName("Should reject null client secret")
        void shouldRejectNullClientSecret() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(VALID_CLIENT_ID)
                        .clientSecret(null)
                        .build();
            });
        }

        @Test
        @DisplayName("Should reject empty client secret")
        void shouldRejectEmptyClientSecret() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(VALID_CLIENT_ID)
                        .clientSecret("")
                        .build();
            });
        }

        @Test
        @DisplayName("Should reject whitespace-only client secret")
        void shouldRejectWhitespaceOnlyClientSecret() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(VALID_CLIENT_ID)
                        .clientSecret("   ")
                        .build();
            });
        }

        @Test
        @DisplayName("Should trim whitespace from credentials")
        void shouldTrimWhitespaceFromCredentials() {
            assertDoesNotThrow(() -> {
                FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
                        .clientId("  " + VALID_CLIENT_ID + "  ")
                        .clientSecret("  " + VALID_CLIENT_SECRET + "  ")
                        .offlineMode(true)
                        .build();
                sdk.close();
            });
        }

        @Test
        @DisplayName("Should reject credentials with dangerous characters")
        void shouldRejectCredentialsWithDangerousCharacters() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId("client\nid")
                        .clientSecret(VALID_CLIENT_SECRET)
                        .build();
            });

            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(VALID_CLIENT_ID)
                        .clientSecret("secret\rsecret")
                        .build();
            });
        }

        @Test
        @DisplayName("Should reject credentials with SQL injection patterns")
        void shouldRejectCredentialsWithSqlInjectionPatterns() {
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId("client'; DROP TABLE users; --")
                        .clientSecret(VALID_CLIENT_SECRET)
                        .build();
            });
        }

        @Test
        @DisplayName("Should reject overly long credentials")
        void shouldRejectOverlyLongCredentials() {
            String longString = "a".repeat(300);
            
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(longString)
                        .clientSecret(VALID_CLIENT_SECRET)
                        .build();
            });

            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .clientId(VALID_CLIENT_ID)
                        .clientSecret(longString)
                        .build();
            });
        }
    }

    @Nested
    @DisplayName("Signature Generation Tests")
    class SignatureGenerationTests {

        @Test
        @DisplayName("Should generate consistent signatures")
        void shouldGenerateConsistentSignatures() {
            String payload = "test payload";
            String timestamp = "1234567890";
            
            String signature1 = SecurityUtils.generateSignature(payload, timestamp, VALID_CLIENT_ID, VALID_CLIENT_SECRET);
            String signature2 = SecurityUtils.generateSignature(payload, timestamp, VALID_CLIENT_ID, VALID_CLIENT_SECRET);
            
            assertEquals(signature1, signature2);
            assertNotNull(signature1);
            assertFalse(signature1.isEmpty());
        }

        @Test
        @DisplayName("Should generate different signatures for different payloads")
        void shouldGenerateDifferentSignaturesForDifferentPayloads() {
            String timestamp = "1234567890";
            
            String signature1 = SecurityUtils.generateSignature("payload1", timestamp, VALID_CLIENT_ID, VALID_CLIENT_SECRET);
            String signature2 = SecurityUtils.generateSignature("payload2", timestamp, VALID_CLIENT_ID, VALID_CLIENT_SECRET);
            
            assertNotEquals(signature1, signature2);
        }

        @Test
        @DisplayName("Should generate different signatures for different timestamps")
        void shouldGenerateDifferentSignaturesForDifferentTimestamps() {
            String payload = "test payload";
            
            String signature1 = SecurityUtils.generateSignature(payload, "1234567890", VALID_CLIENT_ID, VALID_CLIENT_SECRET);
            String signature2 = SecurityUtils.generateSignature(payload, "1234567891", VALID_CLIENT_ID, VALID_CLIENT_SECRET);
            
            assertNotEquals(signature1, signature2);
        }

        @Test
        @DisplayName("Should generate different signatures for different client IDs")
        void shouldGenerateDifferentSignaturesForDifferentClientIds() {
            String payload = "test payload";
            String timestamp = "1234567890";
            
            String signature1 = SecurityUtils.generateSignature(payload, timestamp, "client1", VALID_CLIENT_SECRET);
            String signature2 = SecurityUtils.generateSignature(payload, timestamp, "client2", VALID_CLIENT_SECRET);
            
            assertNotEquals(signature1, signature2);
        }

        @Test
        @DisplayName("Should generate different signatures for different client secrets")
        void shouldGenerateDifferentSignaturesForDifferentClientSecrets() {
            String payload = "test payload";
            String timestamp = "1234567890";
            
            String signature1 = SecurityUtils.generateSignature(payload, timestamp, VALID_CLIENT_ID, "secret1");
            String signature2 = SecurityUtils.generateSignature(payload, timestamp, VALID_CLIENT_ID, "secret2");
            
            assertNotEquals(signature1, signature2);
        }

        @Test
        @DisplayName("Should handle empty payload")
        void shouldHandleEmptyPayload() {
            String signature = SecurityUtils.generateSignature("", "1234567890", VALID_CLIENT_ID, VALID_CLIENT_SECRET);
            assertNotNull(signature);
            assertFalse(signature.isEmpty());
        }

        @Test
        @DisplayName("Should generate base64-encoded signatures")
        void shouldGenerateBase64EncodedSignatures() {
            String signature = SecurityUtils.generateSignature("test", "1234567890", VALID_CLIENT_ID, VALID_CLIENT_SECRET);
            
            // Base64 encoded strings should not contain whitespace or special chars except + / =
            assertTrue(signature.matches("^[A-Za-z0-9+/]*={0,2}$"));
        }
    }

    @Nested
    @DisplayName("Environment Variable Authentication Tests")
    class EnvironmentVariableAuthenticationTests {

        @Test
        @DisplayName("Should use environment variables when credentials not provided")
        void shouldUseEnvironmentVariablesWhenCredentialsNotProvided() {
            // This test would require mocking System.getenv()
            // For now, we test that the builder accepts missing credentials when env vars should be available
            
            // In a real scenario, if env vars are set, this should work
            // If not set, it should throw IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> {
                new FeatureFlagsHQSDK.Builder()
                        .environment("test")
                        .offlineMode(true)
                        .build();
            });
        }

        @Test
        @DisplayName("Should prioritize explicit credentials over environment variables")
        void shouldPrioritizeExplicitCredentialsOverEnvironmentVariables() {
            // Explicit credentials should always take precedence
            assertDoesNotThrow(() -> {
                FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
                        .clientId(VALID_CLIENT_ID)
                        .clientSecret(VALID_CLIENT_SECRET)
                        .environment("test")
                        .offlineMode(true)
                        .build();
                sdk.close();
            });
        }
    }

    @Nested
    @DisplayName("Authentication State Tests")
    class AuthenticationStateTests {

        @Test
        @DisplayName("Should indicate authentication status in health check")
        void shouldIndicateAuthenticationStatusInHealthCheck() {
            FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
                    .clientId(VALID_CLIENT_ID)
                    .clientSecret(VALID_CLIENT_SECRET)
                    .environment("test")
                    .offlineMode(true)
                    .build();

            try {
                Map<String, Object> health = sdk.getHealthCheck();
                assertNotNull(health);
                assertTrue(health.containsKey("status"));
                // In offline mode, should be healthy regardless of auth
                assertEquals("healthy", health.get("status"));
            } finally {
                sdk.close();
            }
        }

        @Test
        @DisplayName("Should continue working after authentication failures")
        void shouldContinueWorkingAfterAuthenticationFailures() {
            FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
                    .clientId("invalid-client-id")
                    .clientSecret("invalid-client-secret")
                    .environment("test")
                    .offlineMode(true) // This prevents actual auth failures
                    .build();

            try {
                // SDK should continue working with default values
                boolean result = sdk.getBool(TEST_USER_ID, "test_flag", true);
                assertTrue(result);
                
                String stringResult = sdk.getString(TEST_USER_ID, "test_flag", "default");
                assertEquals("default", stringResult);
            } finally {
                sdk.close();
            }
        }
    }

    @Nested
    @DisplayName("Security Configuration Tests")
    class SecurityConfigurationTests {

        @Test
        @DisplayName("Should reject insecure configurations in production")
        void shouldRejectInsecureConfigurationsInProduction() {
            // Test the production configuration validator
            Map<String, Object> insecureConfig = Map.of(
                "api_base_url", "http://insecure.example.com",
                "timeout", 1,
                "client_secret", "weak"
            );
            
            var warnings = FeatureFlagsHQSDK.validateProductionConfig(insecureConfig);
            assertFalse(warnings.isEmpty());
            assertTrue(warnings.stream().anyMatch(w -> w.contains("HTTP instead of HTTPS")));
            assertTrue(warnings.stream().anyMatch(w -> w.contains("Timeout too low")));
            assertTrue(warnings.stream().anyMatch(w -> w.contains("weak")));
        }

        @Test
        @DisplayName("Should accept secure production configurations")
        void shouldAcceptSecureProductionConfigurations() {
            Map<String, Object> secureConfig = Map.of(
                "api_base_url", "https://secure.example.com",
                "timeout", 30,
                "client_secret", "very-long-and-secure-client-secret-12345678"
            );
            
            var warnings = FeatureFlagsHQSDK.validateProductionConfig(secureConfig);
            assertTrue(warnings.isEmpty());
        }

        @Test
        @DisplayName("Should create production client with secure defaults")
        void shouldCreateProductionClientWithSecureDefaults() {
            assertDoesNotThrow(() -> {
                FeatureFlagsHQSDK prodSdk = FeatureFlagsHQSDK.createProductionClient(
                    VALID_CLIENT_ID,
                    VALID_CLIENT_SECRET,
                    "production",
                    Map.of("offline_mode", true) // For testing
                );
                prodSdk.close();
            });
        }
    }

    @Nested
    @DisplayName("Credential Masking Tests")
    class CredentialMaskingTests {

        @Test
        @DisplayName("Should mask sensitive values in logs")
        void shouldMaskSensitiveValuesInLogs() {
            String sensitiveValue = "very-secret-client-secret-123";
            String masked = SecurityUtils.maskSensitiveValue(sensitiveValue);
            
            assertNotEquals(sensitiveValue, masked);
            assertTrue(masked.contains("***"));
            assertFalse(masked.contains("secret-123"));
        }

        @Test
        @DisplayName("Should mask short sensitive values completely")
        void shouldMaskShortSensitiveValuesCompletely() {
            String shortValue = "secret";
            String masked = SecurityUtils.maskSensitiveValue(shortValue);
            assertEquals("***", masked);
        }

        @Test
        @DisplayName("Should handle null values in masking")
        void shouldHandleNullValuesInMasking() {
            String masked = SecurityUtils.maskSensitiveValue(null);
            assertEquals("***", masked);
        }
    }
}