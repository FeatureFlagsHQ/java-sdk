import com.featureflagshq.sdk.FeatureFlagsHQSDK;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic example demonstrating FeatureFlagsHQ Java SDK usage
 */
public class BasicExample {

    public static void main(String[] args) {
        // Initialize the SDK
        FeatureFlagsHQSDK sdk = new FeatureFlagsHQSDK.Builder()
                .clientId(System.getenv("FEATUREFLAGSHQ_CLIENT_ID"))
                .clientSecret(System.getenv("FEATUREFLAGSHQ_CLIENT_SECRET"))
                .environment("production")
                .build();

        try {
            // Example user
            String userId = "user_12345";

            // Example 1: Boolean flag
            boolean newUIEnabled = sdk.getBool(userId, "new_ui_enabled", false);
            System.out.println("New UI enabled: " + newUIEnabled);

            // Example 2: String flag
            String theme = sdk.getString(userId, "ui_theme", "light");
            System.out.println("UI theme: " + theme);

            // Example 3: Integer flag
            int maxRetries = sdk.getInt(userId, "max_retry_attempts", 3);
            System.out.println("Max retries: " + maxRetries);

            // Example 4: Float flag
            double discountRate = sdk.getFloat(userId, "discount_rate", 0.0);
            System.out.println("Discount rate: " + discountRate);

            // Example 5: JSON flag
            Object config = sdk.getJson(userId, "app_config", new HashMap<>());
            System.out.println("App config: " + config);

            // Example 6: Using segments
            Map<String, Object> userSegments = new HashMap<>();
            userSegments.put("plan", "premium");
            userSegments.put("age", 25);
            userSegments.put("region", "US");

            boolean premiumFeature = sdk.getBool(userId, "premium_feature", false, userSegments);
            System.out.println("Premium feature enabled: " + premiumFeature);

            // Example 7: Get multiple flags at once
            Map<String, Object> allFlags = sdk.getUserFlags(userId, userSegments);
            System.out.println("All user flags: " + allFlags);

            // Example 8: Check SDK health
            Map<String, Object> health = sdk.getHealthCheck();
            System.out.println("SDK status: " + health.get("status"));

            // Example 9: Get SDK statistics
            Map<String, Object> stats = sdk.getStats();
            System.out.println("Total user accesses: " + stats.get("total_user_accesses"));

        } finally {
            // Always close the SDK when done
            sdk.close();
        }
    }
}