/**
 * FeatureFlagsHQ Java SDK
 * <p>
 * Official Java SDK for FeatureFlagsHQ feature flag service. This package provides
 * a comprehensive, thread-safe, and production-ready SDK for managing feature flags
 * in Java applications.
 * </p>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Support for boolean, string, integer, float, and JSON flags</li>
 *   <li>User segmentation and targeting</li>
 *   <li>Percentage rollouts</li>
 *   <li>Real-time flag updates with configurable polling</li>
 *   <li>Offline mode support</li>
 *   <li>Circuit breaker pattern for resilience</li>
 *   <li>Comprehensive analytics and metrics</li>
 *   <li>Thread-safe operations</li>
 *   <li>Security-focused design with input validation</li>
 * </ul>
 * 
 * <h2>Basic Usage:</h2>
 * <pre>{@code
 * // Initialize the SDK
 * FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
 *     .clientId("your-client-id")
 *     .clientSecret("your-client-secret")
 *     .environment("production")
 *     .build();
 * 
 * // Use feature flags
 * boolean isEnabled = sdk.getBool("user123", "new_feature", false);
 * String theme = sdk.getString("user123", "ui_theme", "light");
 * 
 * // Clean up when done
 * sdk.close();
 * }</pre>
 * 
 * <h2>Thread Safety:</h2>
 * <p>
 * All public methods of the SDK are thread-safe and can be called concurrently
 * from multiple threads without external synchronization.
 * </p>
 * 
 * <h2>Error Handling:</h2>
 * <p>
 * The SDK is designed to be resilient and will continue operating even when
 * network connectivity is lost or the service is temporarily unavailable.
 * It will return default values and log appropriate warnings.
 * </p>
 * 
 * @version 1.0.0
 * @since 1.0.0
 * @author FeatureFlagsHQ Team
 */
package com.featureflagshq.sdk;