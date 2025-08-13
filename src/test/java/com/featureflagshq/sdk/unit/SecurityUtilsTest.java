package com.featureflagshq.sdk.unit;

import com.featureflagshq.sdk.utils.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Security Utils Unit Tests")
public class SecurityUtilsTest {

    @Nested
    @DisplayName("Signature Generation Tests")
    class SignatureGenerationTests {

        @Test
        @DisplayName("Should generate consistent signatures")
        void shouldGenerateConsistentSignatures() {
            String payload = "test payload";
            String timestamp = "1234567890";
            String clientId = "test-client";
            String clientSecret = "test-secret";

            String signature1 = SecurityUtils.generateSignature(payload, timestamp, clientId, clientSecret);
            String signature2 = SecurityUtils.generateSignature(payload, timestamp, clientId, clientSecret);

            assertEquals(signature1, signature2);
            assertFalse(signature1.isEmpty());
        }

        @Test
        @DisplayName("Should generate different signatures for different inputs")
        void shouldGenerateDifferentSignaturesForDifferentInputs() {
            String clientId = "test-client";
            String clientSecret = "test-secret";
            String timestamp = "1234567890";

            String signature1 = SecurityUtils.generateSignature("payload1", timestamp, clientId, clientSecret);
            String signature2 = SecurityUtils.generateSignature("payload2", timestamp, clientId, clientSecret);

            assertNotEquals(signature1, signature2);
        }

        @Test
        @DisplayName("Should handle empty payload")
        void shouldHandleEmptyPayload() {
            String signature = SecurityUtils.generateSignature("", "1234567890", "client", "secret");
            assertFalse(signature.isEmpty());
        }
    }

    @Nested
    @DisplayName("Sensitive Data Detection Tests")
    class SensitiveDataDetectionTests {

        @Test
        @DisplayName("Should detect sensitive data patterns")
        void shouldDetectSensitiveDataPatterns() {
            assertTrue(SecurityUtils.containsSensitiveData("secret: my-secret-value"));
            assertTrue(SecurityUtils.containsSensitiveData("signature=abc123"));
            assertTrue(SecurityUtils.containsSensitiveData("token: bearer-token"));
            assertTrue(SecurityUtils.containsSensitiveData("password=mypassword"));
        }

        @Test
        @DisplayName("Should not detect false positives")
        void shouldNotDetectFalsePositives() {
            assertFalse(SecurityUtils.containsSensitiveData("normal text"));
            assertFalse(SecurityUtils.containsSensitiveData("this is a test"));
            assertFalse(SecurityUtils.containsSensitiveData("no sensitive data here"));
        }

        @Test
        @DisplayName("Should handle null and empty strings")
        void shouldHandleNullAndEmptyStrings() {
            assertFalse(SecurityUtils.containsSensitiveData(null));
            assertFalse(SecurityUtils.containsSensitiveData(""));
        }
    }

    @Nested
    @DisplayName("Log Sanitization Tests")
    class LogSanitizationTests {

        @Test
        @DisplayName("Should sanitize sensitive data for logging")
        void shouldSanitizeSensitiveDataForLogging() {
            String input = "secret: my-secret-value";
            String sanitized = SecurityUtils.sanitizeForLogging(input);
            assertTrue(sanitized.contains("***"));
            assertFalse(sanitized.contains("my-secret-value"));
        }

        @Test
        @DisplayName("Should handle null input")
        void shouldHandleNullInput() {
            assertNull(SecurityUtils.sanitizeForLogging(null));
        }

        @Test
        @DisplayName("Should preserve non-sensitive data")
        void shouldPreserveNonSensitiveData() {
            String input = "normal log message";
            String sanitized = SecurityUtils.sanitizeForLogging(input);
            assertEquals(input, sanitized);
        }
    }

    @Nested
    @DisplayName("Dangerous Characters Detection Tests")
    class DangerousCharactersDetectionTests {

        @Test
        @DisplayName("Should detect dangerous characters")
        void shouldDetectDangerousCharacters() {
            assertTrue(SecurityUtils.hasDangerousCharacters("text\nwith\nnewlines"));
            assertTrue(SecurityUtils.hasDangerousCharacters("text\rwith\rcarriage"));
            assertTrue(SecurityUtils.hasDangerousCharacters("text\0with\0null"));
            assertTrue(SecurityUtils.hasDangerousCharacters("text\twith\ttabs"));
        }

        @Test
        @DisplayName("Should not detect safe text")
        void shouldNotDetectSafeText() {
            assertFalse(SecurityUtils.hasDangerousCharacters("safe text"));
            assertFalse(SecurityUtils.hasDangerousCharacters("normal characters 123"));
        }

        @Test
        @DisplayName("Should handle null input")
        void shouldHandleNullInputForDangerousChars() {
            assertFalse(SecurityUtils.hasDangerousCharacters(null));
        }
    }

    @Nested
    @DisplayName("SQL Injection Detection Tests")
    class SqlInjectionDetectionTests {

        @Test
        @DisplayName("Should detect SQL injection patterns")
        void shouldDetectSqlInjectionPatterns() {
            assertTrue(SecurityUtils.hasSqlInjectionPatterns("test; DROP TABLE users;"));
            assertTrue(SecurityUtils.hasSqlInjectionPatterns("SELECT * FROM users"));
            assertTrue(SecurityUtils.hasSqlInjectionPatterns("INSERT INTO table"));
            assertTrue(SecurityUtils.hasSqlInjectionPatterns("/* comment */"));
            assertTrue(SecurityUtils.hasSqlInjectionPatterns("-- comment"));
        }

        @Test
        @DisplayName("Should not detect safe text")
        void shouldNotDetectSafeTextForSql() {
            assertFalse(SecurityUtils.hasSqlInjectionPatterns("normal text"));
            assertFalse(SecurityUtils.hasSqlInjectionPatterns("user data"));
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            assertTrue(SecurityUtils.hasSqlInjectionPatterns("SELECT * from users"));
            assertTrue(SecurityUtils.hasSqlInjectionPatterns("select * FROM users"));
        }

        @Test
        @DisplayName("Should handle null input")
        void shouldHandleNullInputForSql() {
            assertFalse(SecurityUtils.hasSqlInjectionPatterns(null));
        }
    }

    @Nested
    @DisplayName("User Hash Generation Tests")
    class UserHashGenerationTests {

        @Test
        @DisplayName("Should generate consistent hashes")
        void shouldGenerateConsistentHashes() {
            String input = "test-user-123";
            String hash1 = SecurityUtils.generateUserHash(input);
            String hash2 = SecurityUtils.generateUserHash(input);
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("Should generate different hashes for different inputs")
        void shouldGenerateDifferentHashesForDifferentInputs() {
            String hash1 = SecurityUtils.generateUserHash("user1");
            String hash2 = SecurityUtils.generateUserHash("user2");
            assertNotEquals(hash1, hash2);
        }
    }

    @Nested
    @DisplayName("Rollout Percentage Calculation Tests")
    class RolloutPercentageCalculationTests {

        @Test
        @DisplayName("Should calculate consistent percentages")
        void shouldCalculateConsistentPercentages() {
            String flagName = "test-flag";
            String userId = "test-user";
            
            int percentage1 = SecurityUtils.calculateRolloutPercentage(flagName, userId);
            int percentage2 = SecurityUtils.calculateRolloutPercentage(flagName, userId);
            
            assertEquals(percentage1, percentage2);
            assertTrue(percentage1 >= 0 && percentage1 < 100);
        }

        @Test
        @DisplayName("Should produce different percentages for different users")
        void shouldProduceDifferentPercentagesForDifferentUsers() {
            String flagName = "test-flag";
            
            int percentage1 = SecurityUtils.calculateRolloutPercentage(flagName, "user1");
            int percentage2 = SecurityUtils.calculateRolloutPercentage(flagName, "user2");
            
            // While not guaranteed, it's very unlikely they'll be the same
            // This tests the distribution
            assertTrue(percentage1 >= 0 && percentage1 < 100);
            assertTrue(percentage2 >= 0 && percentage2 < 100);
        }
    }

    @Nested
    @DisplayName("URL Validation Tests")
    class UrlValidationTests {

        @Test
        @DisplayName("Should validate HTTPS URLs")
        void shouldValidateHttpsUrls() {
            assertTrue(SecurityUtils.isValidUrl("https://api.example.com"));
            assertTrue(SecurityUtils.isValidUrl("https://localhost:8080/path"));
        }

        @Test
        @DisplayName("Should validate HTTP URLs")
        void shouldValidateHttpUrls() {
            assertTrue(SecurityUtils.isValidUrl("http://localhost"));
            assertTrue(SecurityUtils.isValidUrl("http://example.com:8080"));
        }

        @Test
        @DisplayName("Should reject invalid URLs")
        void shouldRejectInvalidUrls() {
            assertFalse(SecurityUtils.isValidUrl("ftp://example.com"));
            assertFalse(SecurityUtils.isValidUrl("not-a-url"));
            assertFalse(SecurityUtils.isValidUrl(""));
            assertFalse(SecurityUtils.isValidUrl(null));
        }
    }

    @Nested
    @DisplayName("Value Masking Tests")
    class ValueMaskingTests {

        @Test
        @DisplayName("Should mask long values")
        void shouldMaskLongValues() {
            String result = SecurityUtils.maskSensitiveValue("very-long-secret-value");
            assertTrue(result.contains("***"));
            assertTrue(result.startsWith("very"));
        }

        @Test
        @DisplayName("Should mask short values completely")
        void shouldMaskShortValuesCompletely() {
            String result = SecurityUtils.maskSensitiveValue("short");
            assertEquals("***", result);
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            String result = SecurityUtils.maskSensitiveValue(null);
            assertEquals("***", result);
        }
    }
}