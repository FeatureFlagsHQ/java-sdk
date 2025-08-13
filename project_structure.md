# FeatureFlagsHQ Java SDK - Project Structure

```
featureflagshq-java-sdk/
├── pom.xml                                 # Maven build file
├── build.gradle                           # Gradle build file (alternative)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew                                 # Gradle wrapper script (Unix)
├── gradlew.bat                             # Gradle wrapper script (Windows)
├── README.md                               # Project documentation
├── LICENSE                                 # MIT License
├── CHANGELOG.md                            # Version history
├── .gitignore                              # Git ignore rules
├── .github/
│   └── workflows/
│       ├── ci.yml                          # GitHub Actions CI
│       └── release.yml                     # GitHub Actions Release
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── featureflagshq/
│   │   │           └── sdk/
│   │   │               ├── FeatureFlagsHQSDK.java     # Main SDK class
│   │   │               ├── exceptions/                # Custom exceptions
│   │   │               │   ├── FeatureFlagsHQException.java
│   │   │               │   ├── AuthenticationException.java
│   │   │               │   ├── ValidationException.java
│   │   │               │   └── NetworkException.java
│   │   │               ├── models/                    # Data models
│   │   │               │   ├── Flag.java
│   │   │               │   ├── Segment.java
│   │   │               │   ├── EvaluationResult.java
│   │   │               │   ├── EvaluationContext.java
│   │   │               │   └── FlagChange.java
│   │   │               ├── config/                    # Configuration classes
│   │   │               │   ├── SDKConfig.java
│   │   │               │   └── Constants.java
│   │   │               └── utils/                     # Utility classes
│   │   │                   ├── SecurityUtils.java
│   │   │                   ├── ValidationUtils.java
│   │   │                   └── LoggingUtils.java
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── services/               # Service loader files if needed
│   │       └── logging.properties          # Default logging configuration
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── featureflagshq/
│       │           └── sdk/
│       │               ├── FeatureFlagsHQSDKTest.java # Main SDK tests
│       │               ├── integration/              # Integration tests
│       │               │   ├── FeatureFlagsHQSDKIntegrationTest.java
│       │               │   └── MockServerTest.java
│       │               ├── unit/                     # Unit tests
│       │               │   ├── FlagEvaluationTest.java
│       │               │   ├── AuthenticationTest.java
│       │               │   ├── ValidationTest.java
│       │               │   └── CircuitBreakerTest.java
│       │               └── utils/                    # Test utilities
│       │                   ├── TestUtils.java
│       │                   └── MockFactory.java
│       └── resources/
│           ├── test-data/                  # Test data files
│           │   ├── flags-response.json
│           │   └── segments-data.json
│           └── logback-test.xml            # Test logging configuration
├── docs/                                   # Documentation
│   ├── getting-started.md
│   ├── api-reference.md
│   ├── examples/
│   │   ├── basic-usage.md
│   │   ├── spring-boot-integration.md
│   │   └── android-integration.md
│   └── migration/
│       └── from-v0-to-v1.md
├── examples/                               # Example projects
│   ├── basic-example/
│   │   └── src/main/java/BasicExample.java
│   ├── spring-boot-example/
│   │   ├── pom.xml
│   │   └── src/
│   └── android-example/
│       ├── build.gradle
│       └── src/
└── scripts/                                # Build and deployment scripts
    ├── build.sh                            # Build script
    ├── test.sh                             # Test script
    ├── release.sh                          # Release script
    └── deploy.sh                           # Deployment script
```

## File Descriptions

### Root Files
- **pom.xml**: Maven build configuration with dependencies, plugins, and publishing setup
- **build.gradle**: Gradle build configuration (alternative to Maven)
- **README.md**: Main project documentation with usage examples
- **LICENSE**: MIT license file
- **CHANGELOG.md**: Version history and release notes
- **.gitignore**: Git ignore patterns for Java projects

### Source Code Structure
- **Main SDK Class**: `FeatureFlagsHQSDK.java` - The primary class users interact with
- **Exceptions**: Custom exception classes for different error scenarios
- **Models**: Data transfer objects and value classes
- **Config**: Configuration and constants classes
- **Utils**: Utility classes for common functionality

### Test Structure
- **Unit Tests**: Fast, isolated tests for individual components
- **Integration Tests**: Tests that verify interaction with external services
- **Test Data**: JSON files and mock data for testing
- **Test Utilities**: Helper classes for test setup and assertions

### Documentation
- **Getting Started**: Quick start guide for new users
- **API Reference**: Detailed API documentation
- **Examples**: Code examples for different use cases
- **Migration Guides**: Help for upgrading between versions

### Examples
- **Basic Example**: Simple command-line usage
- **Spring Boot Example**: Integration with Spring Boot applications
- **Android Example**: Usage in Android applications

### Build Scripts
- **build.sh**: Automated build script
- **test.sh**: Run all tests with coverage
- **release.sh**: Create and publish releases
- **deploy.sh**: Deploy to repositories

## Key Features of This Structure

1. **Multi-Build System Support**: Both Maven and Gradle configurations
2. **Comprehensive Testing**: Unit, integration, and test utilities
3. **Documentation**: Complete docs with examples and migration guides
4. **CI/CD Ready**: GitHub Actions workflows for automated testing and releases
5. **Publishing Ready**: Configured for Maven Central publication
6. **Modular Design**: Clean separation of concerns across packages
7. **Example Projects**: Real-world usage examples for different frameworks