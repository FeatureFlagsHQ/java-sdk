# Basic Usage Guide

This guide provides practical examples of how to use the FeatureFlagsHQ Java SDK in your applications.

## Table of Contents

- [SDK Initialization](#sdk-initialization)
- [Boolean Flags](#boolean-flags)
- [String Flags](#string-flags)
- [Numeric Flags](#numeric-flags)
- [JSON Flags](#json-flags)
- [User Segmentation](#user-segmentation)
- [Bulk Operations](#bulk-operations)
- [Error Handling](#error-handling)
- [Advanced Features](#advanced-features)

## SDK Initialization

### Basic Initialization

```java
import com.featureflagshq.sdk.FeatureFlagsHQSDK;

// Simple initialization
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .clientId("your-client-id")
    .clientSecret("your-client-secret")
    .build();
```

### Using Environment Variables

```java
// Set environment variables first:
// FEATUREFLAGSHQ_CLIENT_ID=your-client-id
// FEATUREFLAGSHQ_CLIENT_SECRET=your-client-secret

FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .environment("production")
    .build();
```

### Production Configuration

```java
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .clientId("your-client-id")
    .clientSecret("your-client-secret")
    .environment("production")
    .timeout(30)                    // 30 second timeout
    .maxRetries(3)                  // 3 retry attempts
    .enableMetrics(true)            // Enable usage metrics
    .onFlagChange(change -> {       // Flag change callback
        System.out.println("Flag " + change.getFlagName() + " changed from " + 
                          change.getOldValue() + " to " + change.getNewValue());
    })
    .build();
```

### Try-with-Resources Pattern

```java
try (FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        .build()) {
    
    // Use the SDK
    boolean feature = sdk.getBool("user123", "new_feature", false);
    
    // SDK automatically closed when exiting try block
}
```

## Boolean Flags

### Simple Boolean Flags

```java
String userId = "user_12345";

// Basic usage with default value
boolean newUIEnabled = sdk.getBool(userId, "new_ui_enabled", false);

if (newUIEnabled) {
    // Show new UI
    renderNewInterface();
} else {
    // Show old UI
    renderLegacyInterface();
}
```

### Feature Toggles

```java
// A/B testing
boolean variantA = sdk.getBool(userId, "experiment_variant_a", false);

if (variantA) {
    // User is in variant A
    showVariantA();
} else {
    // User is in control group or variant B
    showControl();
}

// Gradual rollouts
boolean betaFeature = sdk.getBool(userId, "beta_feature_rollout", false);
if (betaFeature) {
    enableBetaFeatures();
}
```

### Safety Checks

```java
// Critical features with safe defaults
boolean maintenanceMode = sdk.getBool(userId, "maintenance_mode", false);
boolean emergencyShutdown = sdk.getBool(userId, "emergency_shutdown", true); // Default to safe state

if (emergencyShutdown || maintenanceMode) {
    showMaintenancePage();
    return;
}
```

## String Flags

### Configuration Values

```java
// Theme selection
String theme = sdk.getString(userId, "ui_theme", "light");
applyTheme(theme);

// API endpoints
String apiEndpoint = sdk.getString(userId, "api_endpoint", "https://api.default.com");
configureApiClient(apiEndpoint);

// Welcome messages
String welcomeMessage = sdk.getString(userId, "welcome_message", "Welcome!");
displayMessage(welcomeMessage);
```

### Content Variations

```java
// Marketing copy
String ctaText = sdk.getString(userId, "cta_button_text", "Sign Up");
String headline = sdk.getString(userId, "landing_page_headline", "Welcome to Our Service");

// Localization
String language = sdk.getString(userId, "user_language", "en");
String localizedContent = sdk.getString(userId, "content_" + language, "Default content");
```

## Numeric Flags

### Integer Flags

```java
// Limits and thresholds
int maxRetries = sdk.getInt(userId, "max_retry_attempts", 3);
int uploadSizeLimit = sdk.getInt(userId, "max_upload_size_mb", 10);
int requestsPerMinute = sdk.getInt(userId, "rate_limit_rpm", 100);

// Feature quantities
int freeTrialDays = sdk.getInt(userId, "free_trial_days", 14);
int maxProjects = sdk.getInt(userId, "max_projects", 5);

// Use the values
for (int attempt = 0; attempt < maxRetries; attempt++) {
    if (tryOperation()) {
        break;
    }
}
```

### Float Flags

```java
// Pricing and discounts
double discountRate = sdk.getFloat(userId, "discount_percentage", 0.0);
double price = basePrice * (1.0 - discountRate);

// Performance tuning
double cacheHitRatio = sdk.getFloat(userId, "cache_hit_ratio_threshold", 0.8);
double loadBalancerWeight = sdk.getFloat(userId, "server_weight", 1.0);

// Algorithm parameters
double recommendationScore = sdk.getFloat(userId, "min_recommendation_score", 0.5);
```

## JSON Flags

### Configuration Objects

```java
import java.util.Map;
import java.util.HashMap;

// Default configuration
Map<String, Object> defaultConfig = new HashMap<>();
defaultConfig.put("timeout", 30);
defaultConfig.put("retries", 3);
defaultConfig.put("debug", false);

// Get configuration
Object configObj = sdk.getJson(userId, "app_config", defaultConfig);
if (configObj instanceof Map) {
    @SuppressWarnings("unchecked")
    Map<String, Object> config = (Map<String, Object>) configObj;
    
    int timeout = (Integer) config.getOrDefault("timeout", 30);
    int retries = (Integer) config.getOrDefault("retries", 3);
    boolean debug = (Boolean) config.getOrDefault("debug", false);
    
    configureApplication(timeout, retries, debug);
}
```

### Feature Sets

```java
// Feature toggles as JSON
Map<String, Object> defaultFeatures = new HashMap<>();
defaultFeatures.put("advanced_search", false);
defaultFeatures.put("real_time_sync", false);
defaultFeatures.put("dark_mode", true);

Object featuresObj = sdk.getJson(userId, "enabled_features", defaultFeatures);
if (featuresObj instanceof Map) {
    @SuppressWarnings("unchecked")
    Map<String, Object> features = (Map<String, Object>) featuresObj;
    
    if ((Boolean) features.getOrDefault("advanced_search", false)) {
        enableAdvancedSearch();
    }
    
    if ((Boolean) features.getOrDefault("real_time_sync", false)) {
        enableRealTimeSync();
    }
}
```

## User Segmentation

### Basic Segmentation

```java
import java.util.Map;
import java.util.HashMap;

// Define user segments
Map<String, Object> userSegments = new HashMap<>();
userSegments.put("plan", "premium");           // User's subscription plan
userSegments.put("age", 28);                   // User's age
userSegments.put("region", "US");              // User's region
userSegments.put("signup_date", "2024-01-15"); // When user signed up
userSegments.put("beta_user", true);           // Beta program participation

// Use segments in flag evaluation
boolean premiumFeature = sdk.getBool(userId, "premium_dashboard", false, userSegments);
String supportLevel = sdk.getString(userId, "support_tier", "basic", userSegments);
```

### Dynamic Segmentation

```java
public Map<String, Object> getUserSegments(User user) {
    Map<String, Object> segments = new HashMap<>();
    
    // User attributes
    segments.put("plan", user.getSubscriptionPlan());
    segments.put("age", user.getAge());
    segments.put("region", user.getRegion());
    segments.put("account_type", user.getAccountType());
    
    // Behavioral data
    segments.put("login_count", user.getLoginCount());
    segments.put("last_active", user.getLastActiveDate());
    segments.put("total_purchases", user.getTotalPurchases());
    
    // Computed segments
    segments.put("is_power_user", user.getLoginCount() > 100);
    segments.put("is_recent", user.getSignupDate().isAfter(LocalDate.now().minusDays(30)));
    
    return segments;
}

// Usage
Map<String, Object> segments = getUserSegments(currentUser);
boolean advancedFeatures = sdk.getBool(userId, "advanced_features", false, segments);
```

### Role-Based Access

```java
// Role-based feature access
Map<String, Object> userContext = new HashMap<>();
userContext.put("role", user.getRole());              // "admin", "editor", "viewer"
userContext.put("permissions", user.getPermissions()); // List of permissions
userContext.put("team_size", user.getTeam().size());  // Team size

boolean adminPanel = sdk.getBool(userId, "admin_panel_access", false, userContext);
boolean bulkOperations = sdk.getBool(userId, "bulk_operations", false, userContext);
int exportLimit = sdk.getInt(userId, "export_limit", 100, userContext);
```

## Bulk Operations

### Getting Multiple Flags

```java
import java.util.List;
import java.util.Arrays;

// Get all flags for a user
Map<String, Object> allFlags = sdk.getUserFlags(userId);
System.out.println("User has " + allFlags.size() + " flags");

// Get all flags with segments
Map<String, Object> userSegments = getUserSegments(currentUser);
Map<String, Object> segmentedFlags = sdk.getUserFlags(userId, userSegments);

// Get specific flags only
List<String> flagsOfInterest = Arrays.asList(
    "new_dashboard",
    "premium_features", 
    "beta_search",
    "mobile_app_promo"
);
Map<String, Object> specificFlags = sdk.getUserFlags(userId, userSegments, flagsOfInterest);
```

### Checking Multiple Features

```java
public class FeatureManager {
    private final FeatureFlagsHQSDK sdk;
    private final String userId;
    private final Map<String, Object> userSegments;
    
    public FeatureManager(FeatureFlagsHQSDK sdk, String userId, Map<String, Object> segments) {
        this.sdk = sdk;
        this.userId = userId;
        this.userSegments = segments;
    }
    
    public boolean isFeatureEnabled(String feature) {
        return sdk.getBool(userId, feature, false, userSegments);
    }
    
    public Map<String, Boolean> getFeatureSet(String... features) {
        Map<String, Boolean> result = new HashMap<>();
        for (String feature : features) {
            result.put(feature, isFeatureEnabled(feature));
        }
        return result;
    }
}

// Usage
FeatureManager features = new FeatureManager(sdk, userId, userSegments);
Map<String, Boolean> uiFeatures = features.getFeatureSet(
    "new_navigation",
    "dark_mode", 
    "real_time_updates",
    "advanced_filters"
);
```

## Error Handling

### Graceful Degradation

```java
// The SDK handles errors gracefully and returns defaults
public void renderUserInterface(String userId) {
    try {
        // These calls will never throw exceptions
        boolean newUI = sdk.getBool(userId, "new_ui", false);
        String theme = sdk.getString(userId, "theme", "light");
        int maxItems = sdk.getInt(userId, "max_display_items", 10);
        
        renderUI(newUI, theme, maxItems);
        
    } catch (Exception e) {
        // This catch block should never execute for normal flag operations
        // Only configuration errors during SDK initialization throw exceptions
        logger.error("Unexpected error", e);
        renderDefaultUI();
    }
}
```

### Validation and Safety

```java
public boolean isValidUserId(String userId) {
    return userId != null && 
           !userId.trim().isEmpty() && 
           userId.length() <= 255 &&
           userId.matches("^[a-zA-Z0-9_@\\.\\-\\+]+$");
}

public Object getFlagSafely(String userId, String flagName, Object defaultValue) {
    // Validate inputs
    if (!isValidUserId(userId)) {
        logger.warn("Invalid user ID: " + userId);
        return defaultValue;
    }
    
    if (flagName == null || flagName.trim().isEmpty()) {
        logger.warn("Invalid flag name");
        return defaultValue;
    }
    
    // SDK handles the rest gracefully
    return sdk.get(userId, flagName, defaultValue);
}
```

## Advanced Features

### Health Monitoring

```java
import java.util.Map;

// Check SDK health
Map<String, Object> health = sdk.getHealthCheck();
String status = (String) health.get("status");
boolean initialized = (Boolean) health.get("initialization_complete");

if (!"healthy".equals(status)) {
    logger.warn("SDK is not healthy: " + health);
}

// Get usage statistics
Map<String, Object> stats = sdk.getStats();
int totalAccesses = (Integer) stats.get("total_user_accesses");
int uniqueUsers = (Integer) stats.get("unique_users_count");

logger.info("SDK Stats - Accesses: " + totalAccesses + ", Users: " + uniqueUsers);
```

### Manual Operations

```java
// Force refresh flags from server
boolean refreshed = sdk.refreshFlags();
if (refreshed) {
    logger.info("Flags refreshed successfully");
} else {
    logger.warn("Failed to refresh flags");
}

// Force upload pending logs
boolean logsFlushed = sdk.flushLogs();
if (logsFlushed) {
    logger.info("Logs uploaded successfully");
}
```

### Flag Change Notifications

```java
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .clientId("your-client-id")
    .clientSecret("your-client-secret")
    .onFlagChange(change -> {
        logger.info("Flag changed: " + change.getFlagName() + 
                   " from " + change.getOldValue() + 
                   " to " + change.getNewValue());
        
        // React to specific flag changes
        if ("maintenance_mode".equals(change.getFlagName())) {
            boolean maintenanceMode = (Boolean) change.getNewValue();
            if (maintenanceMode) {
                enableMaintenanceMode();
            } else {
                disableMaintenanceMode();
            }
        }
    })
    .build();
```

### Production Client Factory

```java
// Use the production client factory for secure defaults
Map<String, Object> options = new HashMap<>();
options.put("timeout", 30);
options.put("enable_metrics", true);

FeatureFlagsHQSDK prodSDK = FeatureFlagsHQSDK.createProductionClient(
    "your-client-id",
    "your-client-secret", 
    "production",
    options
);
```

## Best Practices

### 1. SDK Lifecycle Management

```java
@Component
public class FeatureFlagService {
    private final FeatureFlagsHQSDK sdk;
    
    public FeatureFlagService() {
        this.sdk = new FeatureFlagsHQSDK.Builder()
            .clientId(System.getenv("FEATUREFLAGSHQ_CLIENT_ID"))
            .clientSecret(System.getenv("FEATUREFLAGSHQ_CLIENT_SECRET"))
            .environment(System.getenv("ENVIRONMENT"))
            .build();
    }
    
    @PreDestroy
    public void cleanup() {
        sdk.close();
    }
    
    // Service methods...
}
```

### 2. Consistent User Context

```java
public class UserContextBuilder {
    public static Map<String, Object> buildContext(User user, HttpServletRequest request) {
        Map<String, Object> context = new HashMap<>();
        
        // User attributes
        context.put("user_id", user.getId());
        context.put("plan", user.getSubscriptionPlan());
        context.put("region", user.getRegion());
        
        // Request context
        context.put("user_agent", request.getHeader("User-Agent"));
        context.put("ip_address", getClientIP(request));
        
        // Time-based context
        context.put("hour_of_day", LocalDateTime.now().getHour());
        context.put("day_of_week", LocalDateTime.now().getDayOfWeek().toString());
        
        return context;
    }
}
```

### 3. Caching for Performance

```java
@Service
public class CachedFeatureFlagService {
    private final FeatureFlagsHQSDK sdk;
    private final Cache<String, Object> flagCache;
    
    public CachedFeatureFlagService(FeatureFlagsHQSDK sdk) {
        this.sdk = sdk;
        this.flagCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    }
    
    public boolean getBoolWithCache(String userId, String flagName, boolean defaultValue) {
        String key = userId + ":" + flagName;
        Object cached = flagCache.getIfPresent(key);
        
        if (cached instanceof Boolean) {
            return (Boolean) cached;
        }
        
        boolean value = sdk.getBool(userId, flagName, defaultValue);
        flagCache.put(key, value);
        return value;
    }
}
```

This guide covers the most common usage patterns. For more advanced features and complete API documentation, see the [API Reference](../api-reference.md).