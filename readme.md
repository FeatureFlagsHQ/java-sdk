# FeatureFlagsHQ Java SDK

[![Build Status](https://github.com/featureflagshq/java-sdk/workflows/CI/badge.svg)](https://github.com/featureflagshq/java-sdk/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.featureflagshq/featureflagshq-java-sdk/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.featureflagshq/featureflagshq-java-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Coverage](https://codecov.io/gh/featureflagshq/java-sdk/branch/main/graph/badge.svg)](https://codecov.io/gh/featureflagshq/java-sdk)

Official Java SDK for [FeatureFlagsHQ](https://featureflagshq.com) - A powerful feature flag service that helps you ship faster and safer.

## Features

- ✅ **Boolean, String, Integer, Float, and JSON flags**
- ✅ **User segmentation and targeting**
- ✅ **Percentage rollouts**
- ✅ **Real-time flag updates**
- ✅ **Offline mode support**
- ✅ **Circuit breaker for resilience**
- ✅ **Comprehensive analytics and metrics**
- ✅ **Thread-safe and production-ready**
- ✅ **Java 8+ compatible**

## Quick Start

### Installation

#### Maven
```xml
<dependency>
    <groupId>com.featureflagshq</groupId>
    <artifactId>featureflagshq-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle
```gradle
implementation 'com.featureflagshq:featureflagshq-java-sdk:1.0.0'
```

### Basic Usage

```java
import com.featureflagshq.sdk.FeatureFlagsHQSDK;

// Initialize the SDK
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .clientId("your-client-id")
    .clientSecret("your-client-secret")
    .environment("production")
    .build();

// Check if a feature is enabled
boolean newFeatureEnabled = sdk.getBool("user123", "new_checkout_flow", false);

if (newFeatureEnabled) {
    // Show new checkout flow
} else {
    // Show legacy checkout flow
}

// Get different flag types
String welcomeMessage = sdk.getString("user123", "welcome_message", "Welcome!");
int maxRetries = sdk.getInt("user123", "max_retry_attempts", 3);
double discountRate = sdk.getFloat("user123", "discount_rate", 0.1);

// Clean up when done
sdk.close();
```

### Using with Segments

```java
import java.util.Map;
import java.util.HashMap;

// Define user segments
Map<String, Object> userSegments = new HashMap<>();
userSegments.put("age", 28);
userSegments.put("plan", "premium");
userSegments.put("country", "US");

// Evaluate flags with segments
boolean premiumFeature = sdk.getBool("user123", "premium_dashboard", false, userSegments);
```

### Environment Variables

You can configure the SDK using environment variables:

```bash
export FEATUREFLAGSHQ_CLIENT_ID="your-client-id"
export FEATUREFLAGSHQ_CLIENT_SECRET="your-client-secret"
export FEATUREFLAGSHQ_ENVIRONMENT="production"
```

```java
// SDK will automatically pick up environment variables
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder().build();
```

## Advanced Configuration

### Full Configuration Options

```java
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .clientId("your-client-id")
    .clientSecret("your-client-secret")
    .apiBaseUrl("https://api.featureflagshq.com")
    .environment("production")
    .timeout(30)                    // Request timeout in seconds
    .maxRetries(3)                  // Max retry attempts
    .offlineMode(false)             // Enable offline mode
    .enableMetrics(true)            // Enable analytics
    .onFlagChange(change -> {       // Flag change callback
        System.out.println("Flag " + change.flagName + 
                          " changed from " + change.oldValue + 
                          " to " + change.newValue);
    })
    .build();
```

### Production Setup

```java
import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import java.util.Map;

// Create production-ready client with validation
Map<String, Object> config = Map.of(
    "timeout", 30,
    "max_retries", 3,
    "enable_metrics", true
);

FeatureFlagsHQSDK sdk = FeatureFlagsHQSDK.createProductionClient(
    "your-client-id",
    "your-client-secret", 
    "production",
    config
);
```

## Spring Boot Integration

### Configuration

```java
@Configuration
public class FeatureFlagsConfig {
    
    @Bean
    @ConditionalOnProperty(name = "featureflags.enabled", havingValue = "true", matchIfMissing = true)
    public FeatureFlagsHQSDK featureFlagsSDK(
            @Value("${featureflags.client-id}") String clientId,
            @Value("${featureflags.client-secret}") String clientSecret,
            @Value("${featureflags.environment:production}") String environment) {
        
        return new FeatureFlagsHQSDK.Builder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .environment(environment)
            .enableMetrics(true)
            .build();
    }
    
    @PreDestroy
    public void cleanup() {
        if (featureFlagsSDK != null) {
            featureFlagsSDK.close();
        }
    }
}
```

### Service Usage

```java
@Service
public class UserService {
    
    @Autowired
    private FeatureFlagsHQSDK featureFlags;
    
    public void processUser(String userId) {
        // Get user segments
        Map<String, Object> segments = getUserSegments(userId);
        
        // Check feature flags
        boolean useNewAlgorithm = featureFlags.getBool(userId, "new_recommendation_algorithm", false, segments);
        
        if (useNewAlgorithm) {
            // Use new algorithm
        } else {
            // Use legacy algorithm
        }
    }
}
```

## Android Integration

### Gradle Setup

```gradle
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'com.featureflagshq:featureflagshq-java-sdk:1.0.0'
}
```

### Application Class

```java
public class MyApplication extends Application {
    private FeatureFlagsHQSDK featureFlags;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        featureFlags = new FeatureFlagsHQSDK.Builder()
            .clientId(BuildConfig.FEATURE_FLAGS_CLIENT_ID)
            .clientSecret(BuildConfig.FEATURE_FLAGS_CLIENT_SECRET)
            .environment(BuildConfig.DEBUG ? "development" : "production")
            .build();
    }
    
    public FeatureFlagsHQSDK getFeatureFlags() {
        return featureFlags;
    }
    
    @Override
    public void onTerminate() {
        if (featureFlags != null) {
            featureFlags.close();
        }
        super.onTerminate();
    }
}
```

### Activity Usage

```java
public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FeatureFlagsHQSDK featureFlags = ((MyApplication) getApplication()).getFeatureFlags();
        
        // Get user ID (from authentication, device ID, etc.)
        String userId = getCurrentUserId();
        
        // Check feature flags
        boolean showNewUI = featureFlags.getBool(userId, "new_ui_design", false);
        
        if (showNewUI) {
            setContentView(R.layout.activity_main_new);
        } else {
            setContentView(R.layout.activity_main_legacy);
        }
    }
}
```

## API Reference

### SDK Builder

| Method | Description | Default |
|--------|-------------|---------|
| `clientId(String)` | Your FeatureFlagsHQ client ID | Required |
| `clientSecret(String)` | Your FeatureFlagsHQ client secret | Required |
| `apiBaseUrl(String)` | API base URL | `https://api.featureflagshq.com` |
| `environment(String)` | Environment name | `production` |
| `timeout(int)` | Request timeout in seconds | `30` |
| `maxRetries(int)` | Maximum retry attempts | `3` |
| `offlineMode(boolean)` | Enable offline mode | `false` |
| `enableMetrics(boolean)` | Enable analytics | `true` |
| `onFlagChange(Consumer<FlagChange>)` | Flag change callback | `null` |

### Flag Evaluation Methods

#### Boolean Flags
```java
boolean getBool(String userId, String flagName)
boolean getBool(String userId, String flagName, boolean defaultValue)
boolean getBool(String userId, String flagName, boolean defaultValue, Map<String, Object> segments)
```

#### String Flags
```java
String getString(String userId, String flagName)
String getString(String userId, String flagName, String defaultValue)
String getString(String userId, String flagName, String defaultValue, Map<String, Object> segments)
```

#### Integer Flags
```java
int getInt(String userId, String flagName)
int getInt(String userId, String flagName, int defaultValue)
int getInt(String userId, String flagName, int defaultValue, Map<String, Object> segments)
```

#### Float Flags
```java
double getFloat(String userId, String flagName)
double getFloat(String userId, String flagName, double defaultValue)
double getFloat(String userId, String flagName, double defaultValue, Map<String, Object> segments)
```

#### JSON Flags
```java
Object getJson(String userId, String flagName)
Object getJson(String userId, String flagName, Object defaultValue)
Object getJson(String userId, String flagName, Object defaultValue, Map<String, Object> segments)
```

### Utility Methods

```java
// Check if flag is enabled
boolean isFlagEnabledForUser(String userId, String flagName)
boolean isFlagEnabledForUser(String userId, String flagName, Map<String, Object> segments)

// Get multiple flags at once
Map<String, Object> getUserFlags(String userId)
Map<String, Object> getUserFlags(String userId, Map<String, Object> segments)
Map<String, Object> getUserFlags(String userId, Map<String, Object> segments, List<String> flagKeys)

// Get all cached flags
Map<String, Map<String, Object>> getAllFlags()

// Manual operations
boolean refreshFlags()          // Manually refresh flags from server
boolean flushLogs()            // Manually upload pending logs
Map<String, Object> getStats() // Get SDK statistics
Map<String, Object> getHealthCheck() // Get health status
```

## Error Handling

The SDK includes comprehensive error handling:

```java
try {
    boolean featureEnabled = sdk.getBool("user123", "new_feature", false);
    // Use the flag value
} catch (Exception e) {
    // SDK handles errors gracefully and returns default values
    // Logging is available if ENABLE_LOGGING is true
}
```

### Custom Exception Types

```java
import com.featureflagshq.sdk.exceptions.*;

try {
    FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
        .clientId("invalid-id")
        .clientSecret("invalid-secret")
        .build();
} catch (ValidationException e) {
    // Handle validation errors
} catch (AuthenticationException e) {
    // Handle authentication errors
} catch (NetworkException e) {
    // Handle network errors
}
```

## Performance & Best Practices

### Thread Safety
The SDK is thread-safe and can be used concurrently across multiple threads.

### Caching
- Flags are cached locally and updated in the background
- No network calls during flag evaluation
- Configurable polling interval (default: 5 minutes)

### Resource Management
```java
// Always close the SDK when done
try (FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .build()) {
    
    // Use SDK
    boolean flag = sdk.getBool("user123", "feature", false);
} // SDK automatically closed
```

### Production Recommendations

1. **Use environment variables** for credentials
2. **Enable metrics** for monitoring and analytics
3. **Set appropriate timeouts** based on your application needs
4. **Handle flag changes** with callbacks for real-time updates
5. **Monitor SDK health** using the health check endpoint
6. **Use segments** for sophisticated targeting

## Monitoring & Analytics

### SDK Statistics
```java
Map<String, Object> stats = sdk.getStats();
System.out.println("Total accesses: " + stats.get("total_user_accesses"));
System.out.println("Unique users: " + stats.get("unique_users_count"));
System.out.println("Average evaluation time: " + 
    ((Map<String, Object>) stats.get("evaluation_times")).get("avg_ms") + "ms");
```

### Health Check
```java
Map<String, Object> health = sdk.getHealthCheck();
String status = (String) health.get("status"); // "healthy", "degraded", or "error"
boolean initComplete = (Boolean) health.get("initialization_complete");
```

## Testing

### Unit Testing with Mocks
```java
@Test
public void testFeatureFlagIntegration() {
    // Create SDK in offline mode for testing
    FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
        .clientId("test-client-id")
        .clientSecret("test-client-secret")
        .offlineMode(true)  // No network calls in tests
        .build();
    
    // Test with default values
    boolean result = sdk.getBool("test-user", "test-flag", true);
    assertTrue(result); // Returns default value in offline mode
}
```

### Integration Testing
```java
@Test
public void testSDKIntegration() {
    // Use test environment credentials
    FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
        .clientId(System.getenv("TEST_CLIENT_ID"))
        .clientSecret(System.getenv("TEST_CLIENT_SECRET"))
        .environment("test")
        .build();
    
    try {
        boolean flag = sdk.getBool("test-user", "test-flag", false);
        // Assert expected behavior
    } finally {
        sdk.close();
    }
}
```

## Migration Guide

### From Version 0.x to 1.x

The main changes in version 1.x:

1. **Builder Pattern**: Use `FeatureFlagsHQSDK.Builder()` instead of direct constructor
2. **Method Names**: `isEnabled()` is now `getBool()`
3. **Configuration**: Environment variables follow new naming convention

```java
// Old (0.x)
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK("client-id", "client-secret");
boolean enabled = sdk.isEnabled("user", "flag");

// New (1.x)
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .clientId("client-id")
    .clientSecret("client-secret")
    .build();
boolean enabled = sdk.getBool("user", "flag", false);
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository
2. Install Java 8+ and Maven/Gradle
3. Run tests: `mvn test` or `gradle test`
4. Build: `mvn package` or `gradle build`

## Support

- 📧 Email: [hello@featureflagshq.com](mailto:hello@featureflagshq.com)
- 📖 Documentation: [https://docs.featureflagshq.com](https://docs.featureflagshq.com)
- 🐛 Issues: [GitHub Issues](https://github.com/featureflagshq/java-sdk/issues)
- 💬 Discord: [FeatureFlagsHQ Community](https://discord.gg/featureflagshq)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a detailed history of changes.
        