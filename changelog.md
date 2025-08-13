# Changelog

All notable changes to the FeatureFlagsHQ Java SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-01-15

### Added
- Initial release of FeatureFlagsHQ Java SDK
- Builder pattern for SDK configuration
- Support for boolean, string, integer, float, and JSON feature flags
- User segmentation and targeting capabilities
- Percentage-based rollout support
- Real-time flag updates via background polling
- Comprehensive analytics and metrics collection
- Circuit breaker pattern for API resilience
- Rate limiting per user to prevent abuse
- Thread-safe operations with concurrent access support
- Offline mode for testing and degraded scenarios
- Flag change callbacks for real-time notifications
- Comprehensive input validation and sanitization
- Security features including HMAC authentication
- Java 8+ compatibility
- Maven and Gradle build support
- Extensive test coverage with unit and integration tests
- GitHub Actions CI/CD pipeline
- Documentation and examples for various frameworks
- Production-ready configuration utilities

### Core Features
- **Authentication**: HMAC-SHA256 signature-based authentication
- **Flag Types**: Boolean, String, Integer, Float, JSON
- **Targeting**: User segments with multiple comparison operators
- **Rollouts**: Percentage-based gradual rollouts
- **Resilience**: Circuit breaker, retry logic, graceful degradation
- **Performance**: Local caching, background updates, minimal latency
- **Monitoring**: Comprehensive metrics, health checks, statistics
- **Security**: Input validation, rate limiting, secure communications

### API Methods
- `getBool(userId, flagName, defaultValue, segments)` - Boolean flags
- `getString(userId, flagName, defaultValue, segments)` - String flags  
- `getInt(userId, flagName, defaultValue, segments)` - Integer flags
- `getFloat(userId, flagName, defaultValue, segments)` - Float flags
- `getJson(userId, flagName, defaultValue, segments)` - JSON flags
- `isFlagEnabledForUser(userId, flagName, segments)` - Convenience method
- `getUserFlags(userId, segments, flagKeys)` - Multiple flags at once
- `getAllFlags()` - Get all cached flags
- `refreshFlags()` - Manual flag refresh
- `flushLogs()` - Manual log upload
- `getStats()` - SDK usage statistics
- `getHealthCheck()` - Health and status information

### Configuration Options
- Client ID and secret (required)
- API base URL (default: https://api.featureflagshq.com)
- Environment name (default: production)
- Request timeout (default: 30 seconds)
- Maximum retries (default: 3)
- Offline mode (default: false)
- Metrics enabled (default: true)
- Flag change callbacks

### Dependencies
- OkHttp 4.12.0 for HTTP client
- Jackson 2.15.2 for JSON processing
- JUnit 5.9.3 for testing
- Mockito 5.3.1 for mocking
- WireMock 2.35.0 for integration testing

### Build Tools
- Maven 3.6+ support with comprehensive pom.xml
- Gradle 7.0+ support with build.gradle
- Java 8, 11, 17, 21 compatibility tested
- Maven Central publishing configuration
- Code coverage with JaCoCo
- Static analysis with SpotBugs and PMD
- Security scanning with OWASP Dependency Check

### Documentation
- Comprehensive README with examples
- Javadoc for all public APIs
- Integration guides for Spring Boot and Android
- Migration guides and best practices
- Performance and security recommendations

### Testing
- 95%+ code coverage
- Unit tests for all core functionality
- Integration tests with mock servers
- Performance and load testing
- Thread safety testing
- Error condition testing

### Security
- HMAC-SHA256 authentication
- Input validation and sanitization
- SQL injection prevention
- Rate limiting and abuse protection
- Secure defaults and production hardening
- No sensitive data in logs

### Performance
- Sub-millisecond flag evaluation
- Local caching with background updates
- Connection pooling and keep-alive
- Minimal memory footprint
- Efficient JSON processing
- Background thread management

### Compatibility
- Java 8+ (tested on 8, 11, 17, 21)
- Spring Boot 2.x and 3.x
- Android API level 21+
- Maven 3.6+
- Gradle 7.0+

### Distribution
- Available on Maven Central
- Source and Javadoc JARs included
- GPG signed releases
- Gradle and Maven wrapper included
- Docker examples available

---

## Release Process

### Version Numbering
We follow [Semantic Versioning](https://semver.org/):
- **MAJOR** version for incompatible API changes
- **MINOR** version for new functionality in a backwards compatible manner  
- **PATCH** version for backwards compatible bug fixes

### Release Types
- **Alpha**: Early development releases (x.y.z-alpha.n)
- **Beta**: Feature complete, testing releases (x.y.z-beta.n)
- **Release Candidate**: Production ready candidates (x.y.z-rc.n)
- **Stable**: Production releases (x.y.z)

### Support Policy
- **Current major version**: Full support with new features and bug fixes
- **Previous major version**: Security updates and critical bug fixes for 12 months
- **Older versions**: Best effort community support

### Upgrade Guidelines
- **Patch versions**: Safe to upgrade, no breaking changes
- **Minor versions**: Safe to upgrade, may include new features
- **Major versions**: Review migration guide, may include breaking changes

---

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on:
- Code of conduct
- Development setup
- Testing requirements  
- Pull request process
- Release procedures

## Support

- 📧 Email: [hello@featureflagshq.com](mailto:hello@featureflagshq.com)
- 📖 Documentation: [https://docs.featureflagshq.com](https://docs.featureflagshq.com)
- 🐛 Issues: [GitHub Issues](https://github.com/featureflagshq/java-sdk/issues)
- 💬 Discord: [FeatureFlagsHQ Community](https://discord.gg/featureflagshq)