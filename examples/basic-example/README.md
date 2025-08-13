# FeatureFlagsHQ Java SDK - Basic Example

This example demonstrates the basic usage of the FeatureFlagsHQ Java SDK.

## Prerequisites

1. Java 8 or higher
2. Maven 3.6 or higher
3. FeatureFlagsHQ account with valid credentials

## Setup

1. Set your credentials as environment variables:
   ```bash
   export FEATUREFLAGSHQ_CLIENT_ID="your-client-id"
   export FEATUREFLAGSHQ_CLIENT_SECRET="your-client-secret"
   ```

2. Build the example:
   ```bash
   mvn clean compile
   ```

## Running the Example

Run the basic example:
```bash
mvn exec:java
```

Or compile and run directly:
```bash
mvn clean compile
java -cp target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) BasicExample
```

## What the Example Demonstrates

- **SDK Initialization** - How to set up the SDK with credentials
- **Boolean Flags** - Getting true/false feature flags
- **String Flags** - Getting string configuration values
- **Integer Flags** - Getting numeric configuration values
- **Float Flags** - Getting decimal configuration values
- **JSON Flags** - Getting complex configuration objects
- **User Segments** - Using user attributes for targeting
- **Bulk Operations** - Getting multiple flags at once
- **Health Monitoring** - Checking SDK status
- **Statistics** - Getting usage metrics
- **Resource Cleanup** - Properly closing the SDK

## Expected Output

```
New UI enabled: false
UI theme: light
Max retries: 3
Discount rate: 0.0
App config: {}
Premium feature enabled: false
All user flags: {}
SDK status: healthy
Total user accesses: 7
```

Note: The actual values will depend on your flag configurations in FeatureFlagsHQ.

## Next Steps

- Check out the [Spring Boot example](../spring-boot-example/) for framework integration
- Read the [API documentation](../../docs/api-reference.md) for advanced usage
- See the [migration guide](../../docs/migration/) for upgrading between versions