# Getting Started with FeatureFlagsHQ Java SDK

This guide will help you get started with the FeatureFlagsHQ Java SDK in just a few minutes.

## Prerequisites

- Java 8 or higher
- Maven 3.6+ or Gradle 6.0+
- FeatureFlagsHQ account ([sign up here](https://featureflagshq.com))

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.featureflagshq</groupId>
    <artifactId>featureflagshq-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```gradle
implementation 'com.featureflagshq:featureflagshq-java-sdk:1.0.0'
```

## Quick Start

### 1. Get Your Credentials

1. Log in to your [FeatureFlagsHQ dashboard](https://app.featureflagshq.com)
2. Go to Settings → API Keys
3. Copy your Client ID and Client Secret

### 2. Initialize the SDK

```java
import com.featureflagshq.sdk.FeatureFlagsHQSDK;

// Option 1: Using builder pattern
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .clientId("your-client-id")
    .clientSecret("your-client-secret")
    .environment("production")
    .build();

// Option 2: Using environment variables
// Set FEATUREFLAGSHQ_CLIENT_ID and FEATUREFLAGSHQ_CLIENT_SECRET
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .environment("production")
    .build();
```

### 3. Use Feature Flags

```java
String userId = "user_12345";

// Boolean flag
boolean newFeatureEnabled = sdk.getBool(userId, "new_feature", false);

// String flag
String theme = sdk.getString(userId, "ui_theme", "light");

// Integer flag
int maxRetries = sdk.getInt(userId, "max_retries", 3);

// With user segments
Map<String, Object> userSegments = new HashMap<>();
userSegments.put("plan", "premium");
userSegments.put("region", "US");

boolean premiumFeature = sdk.getBool(userId, "premium_feature", false, userSegments);
```

### 4. Clean Up

```java
// Always close the SDK when your application shuts down
sdk.close();

// Or use try-with-resources
try (FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .build()) {
    
    boolean flag = sdk.getBool("user", "feature", false);
    // SDK will be automatically closed
}
```

## Configuration Options

The SDK supports various configuration options:

```java
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .clientId("your-client-id")
    .clientSecret("your-client-secret")
    .apiBaseUrl("https://api.featureflagshq.com")  // Custom API URL
    .environment("staging")                         // Environment name
    .timeout(30)                                   // Request timeout in seconds
    .maxRetries(3)                                 // Max retry attempts
    .offlineMode(false)                            // Enable offline mode
    .enableMetrics(true)                           // Enable usage metrics
    .onFlagChange(change -> {                      // Flag change callback
        System.out.println("Flag " + change.getFlagName() + " changed");
    })
    .build();
```

## Environment Variables

You can configure the SDK using environment variables:

- `FEATUREFLAGSHQ_CLIENT_ID` - Your client ID
- `FEATUREFLAGSHQ_CLIENT_SECRET` - Your client secret
- `FEATUREFLAGSHQ_ENVIRONMENT` - Environment name (default: "production")
- `FEATUREFLAGSHQ_API_BASE_URL` - Custom API base URL

## Error Handling

The SDK is designed to be resilient and will:

- Return default values when flags are not found
- Continue working during network outages
- Use circuit breaker pattern to prevent cascading failures
- Log warnings but never throw exceptions during normal operation

```java
// This will return the default value (false) if:
// - The flag doesn't exist
// - Network is unavailable
// - Authentication fails
// - Any other error occurs
boolean safeFlag = sdk.getBool("user", "risky_feature", false);
```

## Thread Safety

All SDK methods are thread-safe and can be called concurrently from multiple threads without external synchronization.

## Best Practices

1. **Singleton Pattern**: Create one SDK instance per application and reuse it
2. **Graceful Defaults**: Always provide sensible default values
3. **Resource Cleanup**: Always call `close()` when shutting down
4. **Environment Variables**: Use environment variables for credentials in production
5. **Error Handling**: Don't catch SDK exceptions - they indicate configuration issues

## Next Steps

- [API Reference](api-reference.md) - Complete API documentation
- [Examples](../examples/) - Working code examples
- [Spring Boot Integration](examples/spring-boot-integration.md) - Framework integration guide
- [Migration Guide](migration/from-v0-to-v1.md) - Upgrading from previous versions

## Support

- 📖 [Documentation](https://docs.featureflagshq.com)
- 💬 [Community Forum](https://community.featureflagshq.com)
- 📧 [Support Email](mailto:hello@featureflagshq.com)
- 🐛 [Bug Reports](https://github.com/featureflagshq/java-sdk/issues)