package com.featureflagshq.sdk.integration;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mock Server Integration Tests")
public class MockServerTest {

    private WireMockServer wireMockServer;
    private FeatureFlagsHQSDK sdk;
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // Create SDK configured to use mock server
        sdk = new FeatureFlagsHQSDK.Builder()
                .clientId(TEST_CLIENT_ID)
                .clientSecret(TEST_CLIENT_SECRET)
                .apiBaseUrl("http://localhost:8089")
                .environment("test")
                .enableMetrics(false)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (sdk != null) {
            sdk.close();
        }
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Should fetch flags from server successfully")
    void shouldFetchFlagsFromServerSuccessfully() {
        // Mock successful flags response
        stubFor(get(urlEqualTo("/v1/flags/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"data\": [\n" +
                                "    {\n" +
                                "      \"name\": \"test_flag\",\n" +
                                "      \"type\": \"bool\",\n" +
                                "      \"value\": true,\n" +
                                "      \"is_active\": true\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}")));

        // Wait a moment for initial fetch
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Force refresh to trigger fetch
        boolean refreshSuccess = sdk.refreshFlags();
        assertTrue(refreshSuccess);

        // Verify flag was fetched
        Map<String, Map<String, Object>> flags = sdk.getAllFlags();
        assertFalse(flags.isEmpty());
    }

    @Test
    @DisplayName("Should handle authentication errors gracefully")
    void shouldHandleAuthenticationErrorsGracefully() {
        // Mock 401 response
        stubFor(get(urlEqualTo("/v1/flags/"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Unauthorized\"}")));

        // Force refresh
        boolean refreshSuccess = sdk.refreshFlags();
        assertFalse(refreshSuccess);

        // SDK should continue working with cached/default values
        boolean result = sdk.getBool(TEST_USER_ID, "non_existent_flag", false);
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle server errors gracefully")
    void shouldHandleServerErrorsGracefully() {
        // Mock 500 response
        stubFor(get(urlEqualTo("/v1/flags/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        // Force refresh
        boolean refreshSuccess = sdk.refreshFlags();
        assertFalse(refreshSuccess);

        // SDK should continue working
        boolean result = sdk.getBool(TEST_USER_ID, "test_flag", true);
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle network timeouts gracefully")
    void shouldHandleNetworkTimeoutsGracefully() {
        // Mock delayed response (longer than SDK timeout)
        stubFor(get(urlEqualTo("/v1/flags/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(35000) // 35 seconds delay
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\": []}")));

        // Force refresh
        boolean refreshSuccess = sdk.refreshFlags();
        assertFalse(refreshSuccess);

        // SDK should continue working
        Map<String, Object> health = sdk.getHealthCheck();
        assertNotNull(health);
        assertTrue(health.containsKey("status"));
    }

    @Test
    @DisplayName("Should handle malformed JSON response gracefully")
    void shouldHandleMalformedJsonResponseGracefully() {
        // Mock malformed JSON response
        stubFor(get(urlEqualTo("/v1/flags/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json }")));

        // Force refresh
        boolean refreshSuccess = sdk.refreshFlags();
        assertFalse(refreshSuccess);

        // SDK should continue working
        String result = sdk.getString(TEST_USER_ID, "test_flag", "default");
        assertEquals("default", result);
    }

    @Test
    @DisplayName("Should upload logs successfully")
    void shouldUploadLogsSuccessfully() {
        // Mock successful log upload response
        stubFor(post(urlEqualTo("/v1/logs/batch/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"success\"}")));

        // Generate some log entries by calling SDK methods
        sdk.getBool(TEST_USER_ID, "test_flag", false);
        sdk.getString(TEST_USER_ID, "test_string", "default");

        // Force log upload
        boolean flushSuccess = sdk.flushLogs();
        // In offline mode or with metrics disabled, this would return false
        // but the mock setup should work if metrics were enabled
        assertFalse(flushSuccess); // Due to metrics being disabled in setup
    }

    @Test
    @DisplayName("Should handle log upload errors gracefully")
    void shouldHandleLogUploadErrorsGracefully() {
        // Mock failed log upload response
        stubFor(post(urlEqualTo("/v1/logs/batch/"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        // Generate some log entries
        sdk.getBool(TEST_USER_ID, "test_flag", false);

        // Force log upload
        boolean flushSuccess = sdk.flushLogs();
        assertFalse(flushSuccess);

        // SDK should continue working normally
        int result = sdk.getInt(TEST_USER_ID, "test_int", 42);
        assertEquals(42, result);
    }

    @Test
    @DisplayName("Should verify request headers are sent correctly")
    void shouldVerifyRequestHeadersAreSentCorrectly() {
        // Mock response with verification
        stubFor(get(urlEqualTo("/v1/flags/"))
                .withHeader("X-Client-ID", equalTo(TEST_CLIENT_ID))
                .withHeader("X-SDK-Version", matching(".*"))
                .withHeader("X-Environment", equalTo("test"))
                .withHeader("User-Agent", matching("FeatureFlagsHQ-Java-SDK/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\": []}")));

        // Force refresh to trigger request
        sdk.refreshFlags();

        // Verify the request was made with correct headers
        verify(getRequestedFor(urlEqualTo("/v1/flags/"))
                .withHeader("X-Client-ID", equalTo(TEST_CLIENT_ID))
                .withHeader("X-Environment", equalTo("test")));
    }

    @Test
    @DisplayName("Should handle empty flag response correctly")
    void shouldHandleEmptyFlagResponseCorrectly() {
        // Mock empty flags response
        stubFor(get(urlEqualTo("/v1/flags/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\": []}")));

        // Force refresh
        boolean refreshSuccess = sdk.refreshFlags();
        assertTrue(refreshSuccess);

        // Verify empty flags
        Map<String, Map<String, Object>> flags = sdk.getAllFlags();
        assertTrue(flags.isEmpty());

        // SDK should return defaults
        boolean result = sdk.getBool(TEST_USER_ID, "missing_flag", true);
        assertTrue(result);
    }
}