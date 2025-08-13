# FeatureFlagsHQ Java SDK - API Reference

Complete API reference for the FeatureFlagsHQ Java SDK.

## Table of Contents

- [FeatureFlagsHQSDK Class](#featureflagshqsdk-class)
- [Builder Pattern](#builder-pattern)
- [Flag Evaluation Methods](#flag-evaluation-methods)
- [Utility Methods](#utility-methods)
- [Data Models](#data-models)
- [Exception Handling](#exception-handling)

## FeatureFlagsHQSDK Class

The main SDK class that provides all feature flag functionality.

### Constructor

The SDK uses the Builder pattern for initialization. Direct construction is not available.

```java
FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
    .clientId("your-client-id")
    .clientSecret("your-client-secret")
    .build();
```

## Builder Pattern

### FeatureFlagsHQSDK.Builder

#### Methods

##### `clientId(String clientId)`
Sets the client ID for authentication.
- **Parameters**: `clientId` - Your FeatureFlagsHQ client ID
- **Returns**: Builder instance for chaining
- **Throws**: `IllegalArgumentException` if clientId is null, empty, or invalid

##### `clientSecret(String clientSecret)`
Sets the client secret for authentication.
- **Parameters**: `clientSecret` - Your FeatureFlagsHQ client secret  
- **Returns**: Builder instance for chaining
- **Throws**: `IllegalArgumentException` if clientSecret is null, empty, or invalid

##### `apiBaseUrl(String apiBaseUrl)`
Sets a custom API base URL.
- **Parameters**: `apiBaseUrl` - Custom API base URL
- **Returns**: Builder instance for chaining
- **Default**: `https://api.featureflagshq.com`
- **Throws**: `IllegalArgumentException` if URL is malformed

##### `environment(String environment)`
Sets the environment name.
- **Parameters**: `environment` - Environment name (e.g., "production", "staging")
- **Returns**: Builder instance for chaining
- **Default**: `"production"`

##### `timeout(int timeout)`
Sets the request timeout in seconds.
- **Parameters**: `timeout` - Timeout in seconds (1-300)
- **Returns**: Builder instance for chaining
- **Default**: `30`
- **Throws**: `IllegalArgumentException` if timeout is out of range

##### `maxRetries(int maxRetries)`
Sets the maximum number of retry attempts.
- **Parameters**: `maxRetries` - Maximum retries (0-10)
- **Returns**: Builder instance for chaining
- **Default**: `3`

##### `offlineMode(boolean offlineMode)`
Enables or disables offline mode.
- **Parameters**: `offlineMode` - True to enable offline mode
- **Returns**: Builder instance for chaining
- **Default**: `false`

##### `enableMetrics(boolean enableMetrics)`
Enables or disables usage metrics collection.
- **Parameters**: `enableMetrics` - True to enable metrics
- **Returns**: Builder instance for chaining
- **Default**: `true`

##### `onFlagChange(Consumer<FlagChange> onFlagChange)`
Sets a callback for flag change notifications.
- **Parameters**: `onFlagChange` - Callback function
- **Returns**: Builder instance for chaining

##### `build()`
Creates the SDK instance.
- **Returns**: Configured FeatureFlagsHQSDK instance
- **Throws**: `IllegalArgumentException` if required configuration is missing

## Flag Evaluation Methods

### Boolean Flags

#### `getBool(String userId, String flagName)`
Gets a boolean flag value with default false.
- **Parameters**: 
  - `userId` - User identifier
  - `flagName` - Flag name
- **Returns**: Boolean flag value or false if not found
- **Thread Safe**: Yes

#### `getBool(String userId, String flagName, boolean defaultValue)`
Gets a boolean flag value with custom default.
- **Parameters**: 
  - `userId` - User identifier
  - `flagName` - Flag name
  - `defaultValue` - Default value if flag not found
- **Returns**: Boolean flag value or defaultValue if not found

#### `getBool(String userId, String flagName, boolean defaultValue, Map<String, Object> segments)`
Gets a boolean flag value with segments.
- **Parameters**: 
  - `userId` - User identifier
  - `flagName` - Flag name
  - `defaultValue` - Default value if flag not found
  - `segments` - User segments for targeting
- **Returns**: Boolean flag value or defaultValue if not found

### String Flags

#### `getString(String userId, String flagName)`
Gets a string flag value with default empty string.

#### `getString(String userId, String flagName, String defaultValue)`
Gets a string flag value with custom default.

#### `getString(String userId, String flagName, String defaultValue, Map<String, Object> segments)`
Gets a string flag value with segments.

### Integer Flags

#### `getInt(String userId, String flagName)`
Gets an integer flag value with default 0.

#### `getInt(String userId, String flagName, int defaultValue)`
Gets an integer flag value with custom default.

#### `getInt(String userId, String flagName, int defaultValue, Map<String, Object> segments)`
Gets an integer flag value with segments.

### Float Flags

#### `getFloat(String userId, String flagName)`
Gets a float flag value with default 0.0.

#### `getFloat(String userId, String flagName, double defaultValue)`
Gets a float flag value with custom default.

#### `getFloat(String userId, String flagName, double defaultValue, Map<String, Object> segments)`
Gets a float flag value with segments.

### JSON Flags

#### `getJson(String userId, String flagName)`
Gets a JSON flag value with default empty map.

#### `getJson(String userId, String flagName, Object defaultValue)`
Gets a JSON flag value with custom default.

#### `getJson(String userId, String flagName, Object defaultValue, Map<String, Object> segments)`
Gets a JSON flag value with segments.

## Utility Methods

### `isFlagEnabledForUser(String userId, String flagName)`
Checks if a flag is enabled for a user (boolean flags only).
- **Returns**: True if flag is enabled, false otherwise

### `isFlagEnabledForUser(String userId, String flagName, Map<String, Object> segments)`
Checks if a flag is enabled for a user with segments.

### `getUserFlags(String userId)`
Gets all flags for a user.
- **Returns**: Map of flag names to values

### `getUserFlags(String userId, Map<String, Object> segments)`
Gets all flags for a user with segments.

### `getUserFlags(String userId, Map<String, Object> segments, List<String> flagKeys)`
Gets specific flags for a user.
- **Parameters**: `flagKeys` - List of flag names to retrieve

### `getAllFlags()`
Gets all cached flags.
- **Returns**: Map of flag names to flag data

### `refreshFlags()`
Manually refresh flags from the server.
- **Returns**: True if refresh was successful

### `flushLogs()`
Manually flush pending logs to the server.
- **Returns**: True if flush was successful

### `getStats()`
Gets SDK usage statistics.
- **Returns**: Map containing statistics

### `getHealthCheck()`
Gets SDK health information.
- **Returns**: Map containing health data

### `close()`
Closes the SDK and releases resources.
Implements `AutoCloseable` for use with try-with-resources.

## Data Models

### FlagChange
Represents a flag value change.

#### Properties
- `String flagName` - Name of the changed flag
- `Object oldValue` - Previous value
- `Object newValue` - New value
- `long timestamp` - When the change occurred

### EvaluationResult
Represents the result of flag evaluation.

#### Properties
- `Object value` - The flag value
- `boolean flagFound` - Whether the flag was found
- `boolean flagActive` - Whether the flag is active
- `boolean defaultValueUsed` - Whether default value was used
- `List<String> segmentsMatched` - Matched user segments
- `String reason` - Reason for the result

### Flag
Represents a feature flag.

#### Properties
- `String name` - Flag name
- `String type` - Flag type (bool, string, int, float, json)
- `Object value` - Flag value
- `boolean isActive` - Whether flag is active
- `String description` - Flag description

### Segment
Represents a user segment.

#### Properties
- `String name` - Segment name
- `String type` - Data type
- `String comparator` - Comparison operator
- `Object value` - Comparison value
- `boolean isActive` - Whether segment is active

## Exception Handling

### FeatureFlagsHQException
Base exception class for all SDK exceptions.

### AuthenticationException
Thrown when authentication fails.

### ValidationException
Thrown when input validation fails.

### NetworkException
Thrown when network operations fail.

## Production Utilities

### `validateProductionConfig(Map<String, Object> config)`
Validates configuration for production use.
- **Returns**: List of configuration warnings

### `createProductionClient(String clientId, String clientSecret, String environment, Map<String, Object> options)`
Creates a production-ready SDK instance with secure defaults.
- **Returns**: Configured SDK instance

## Thread Safety

All SDK methods are thread-safe and can be called concurrently from multiple threads.

## Error Handling

The SDK follows a fail-safe approach:
- Methods return default values instead of throwing exceptions
- Network failures are handled gracefully with circuit breaker pattern
- Input validation failures return default values and log warnings
- Only configuration errors during initialization throw exceptions

## Performance

- Flag evaluations are optimized for low latency (typically < 5ms)
- Background polling and log uploads don't block main operations
- Circuit breaker prevents cascading failures
- Local caching provides fast access to flag values