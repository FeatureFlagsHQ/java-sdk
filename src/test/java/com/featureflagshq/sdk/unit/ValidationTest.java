package com.featureflagshq.sdk.unit;

import com.featureflagshq.sdk.exceptions.ValidationException;
import com.featureflagshq.sdk.utils.ValidationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validation Unit Tests")
public class ValidationTest {

    @Nested
    @DisplayName("String Validation Tests")
    class StringValidationTests {

        @Test
        @DisplayName("Should validate normal strings")
        void shouldValidateNormalStrings() throws ValidationException {
            String result = ValidationUtils.validateString("valid_string", "test");
            assertEquals("valid_string", result);
        }

        @Test
        @DisplayName("Should throw exception for null strings")
        void shouldThrowExceptionForNullStrings() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateString(null, "test");
            });
        }

        @Test
        @DisplayName("Should throw exception for empty strings")
        void shouldThrowExceptionForEmptyStrings() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateString("", "test");
            });
        }

        @Test
        @DisplayName("Should throw exception for strings exceeding max length")
        void shouldThrowExceptionForStringsExceedingMaxLength() {
            String longString = "a".repeat(300);
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateString(longString, "test", 255);
            });
        }

        @Test
        @DisplayName("Should throw exception for strings with dangerous characters")
        void shouldThrowExceptionForStringsWithDangerousCharacters() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateString("test\nstring", "test");
            });
        }

        @Test
        @DisplayName("Should throw exception for strings with SQL injection patterns")
        void shouldThrowExceptionForStringsWithSqlInjectionPatterns() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateString("test; DROP TABLE users;", "test");
            });
        }
    }

    @Nested
    @DisplayName("User ID Validation Tests")
    class UserIdValidationTests {

        @Test
        @DisplayName("Should validate normal user IDs")
        void shouldValidateNormalUserIds() throws ValidationException {
            String result = ValidationUtils.validateUserId("user123");
            assertEquals("user123", result);
        }

        @Test
        @DisplayName("Should validate user IDs with allowed characters")
        void shouldValidateUserIdsWithAllowedCharacters() throws ValidationException {
            String result = ValidationUtils.validateUserId("user_123@example.com");
            assertEquals("user_123@example.com", result);
        }

        @Test
        @DisplayName("Should throw exception for null user ID")
        void shouldThrowExceptionForNullUserId() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateUserId(null);
            });
        }

        @Test
        @DisplayName("Should throw exception for empty user ID")
        void shouldThrowExceptionForEmptyUserId() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateUserId("");
            });
        }

        @Test
        @DisplayName("Should throw exception for user ID exceeding max length")
        void shouldThrowExceptionForUserIdExceedingMaxLength() {
            String longUserId = "user" + "a".repeat(300);
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateUserId(longUserId);
            });
        }
    }

    @Nested
    @DisplayName("Flag Name Validation Tests")
    class FlagNameValidationTests {

        @Test
        @DisplayName("Should validate normal flag names")
        void shouldValidateNormalFlagNames() throws ValidationException {
            String result = ValidationUtils.validateFlagName("feature_flag_1");
            assertEquals("feature_flag_1", result);
        }

        @Test
        @DisplayName("Should validate flag names with hyphens")
        void shouldValidateFlagNamesWithHyphens() throws ValidationException {
            String result = ValidationUtils.validateFlagName("feature-flag-1");
            assertEquals("feature-flag-1", result);
        }

        @Test
        @DisplayName("Should throw exception for null flag name")
        void shouldThrowExceptionForNullFlagName() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateFlagName(null);
            });
        }

        @Test
        @DisplayName("Should throw exception for flag name with invalid characters")
        void shouldThrowExceptionForFlagNameWithInvalidCharacters() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateFlagName("flag@name");
            });
        }

        @Test
        @DisplayName("Should throw exception for flag name exceeding max length")
        void shouldThrowExceptionForFlagNameExceedingMaxLength() {
            String longFlagName = "flag_" + "a".repeat(300);
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateFlagName(longFlagName);
            });
        }
    }

    @Nested
    @DisplayName("URL Validation Tests")
    class UrlValidationTests {

        @Test
        @DisplayName("Should validate HTTPS URLs")
        void shouldValidateHttpsUrls() throws ValidationException {
            String result = ValidationUtils.validateUrl("https://api.example.com");
            assertEquals("https://api.example.com", result);
        }

        @Test
        @DisplayName("Should validate HTTP URLs")
        void shouldValidateHttpUrls() throws ValidationException {
            String result = ValidationUtils.validateUrl("http://localhost:8080");
            assertEquals("http://localhost:8080", result);
        }

        @Test
        @DisplayName("Should remove trailing slash from URLs")
        void shouldRemoveTrailingSlashFromUrls() throws ValidationException {
            String result = ValidationUtils.validateUrl("https://api.example.com/");
            assertEquals("https://api.example.com", result);
        }

        @Test
        @DisplayName("Should throw exception for null URL")
        void shouldThrowExceptionForNullUrl() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateUrl(null);
            });
        }

        @Test
        @DisplayName("Should throw exception for empty URL")
        void shouldThrowExceptionForEmptyUrl() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateUrl("");
            });
        }

        @Test
        @DisplayName("Should throw exception for invalid URL format")
        void shouldThrowExceptionForInvalidUrlFormat() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateUrl("not-a-valid-url");
            });
        }

        @Test
        @DisplayName("Should throw exception for unsupported protocol")
        void shouldThrowExceptionForUnsupportedProtocol() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateUrl("ftp://example.com");
            });
        }
    }

    @Nested
    @DisplayName("Timeout Validation Tests")
    class TimeoutValidationTests {

        @Test
        @DisplayName("Should validate normal timeout values")
        void shouldValidateNormalTimeoutValues() throws ValidationException {
            int result = ValidationUtils.validateTimeout(30);
            assertEquals(30, result);
        }

        @Test
        @DisplayName("Should throw exception for zero timeout")
        void shouldThrowExceptionForZeroTimeout() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateTimeout(0);
            });
        }

        @Test
        @DisplayName("Should throw exception for negative timeout")
        void shouldThrowExceptionForNegativeTimeout() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateTimeout(-1);
            });
        }

        @Test
        @DisplayName("Should throw exception for timeout exceeding maximum")
        void shouldThrowExceptionForTimeoutExceedingMaximum() {
            assertThrows(ValidationException.class, () -> {
                ValidationUtils.validateTimeout(400);
            });
        }
    }

    @Nested
    @DisplayName("Segment Validation Tests")
    class SegmentValidationTests {

        @Test
        @DisplayName("Should validate normal segment keys")
        void shouldValidateNormalSegmentKeys() {
            assertTrue(ValidationUtils.isValidSegmentKey("age"));
            assertTrue(ValidationUtils.isValidSegmentKey("user_plan"));
        }

        @Test
        @DisplayName("Should reject null segment keys")
        void shouldRejectNullSegmentKeys() {
            assertFalse(ValidationUtils.isValidSegmentKey(null));
        }

        @Test
        @DisplayName("Should reject empty segment keys")
        void shouldRejectEmptySegmentKeys() {
            assertFalse(ValidationUtils.isValidSegmentKey(""));
        }

        @Test
        @DisplayName("Should validate segment values")
        void shouldValidateSegmentValues() {
            assertEquals("test", ValidationUtils.validateSegmentValue("test"));
            assertEquals(123, ValidationUtils.validateSegmentValue(123));
            assertEquals(true, ValidationUtils.validateSegmentValue(true));
            assertNull(ValidationUtils.validateSegmentValue(null));
        }

        @Test
        @DisplayName("Should convert complex objects to strings")
        void shouldConvertComplexObjectsToStrings() {
            Object complexObject = new Object() {
                @Override
                public String toString() {
                    return "complex";
                }
            };
            assertEquals("complex", ValidationUtils.validateSegmentValue(complexObject));
        }
    }
}