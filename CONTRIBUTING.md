# Contributing to FeatureFlagsHQ Java SDK

Thank you for your interest in contributing to the FeatureFlagsHQ Java SDK! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Security](#security)

## Code of Conduct

This project adheres to a Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to [hello@featureflagshq.com](mailto:hello@featureflagshq.com).

## Getting Started

1. Fork the repository on GitHub
2. Clone your fork locally
3. Create a new branch for your changes
4. Make your changes
5. Test your changes
6. Submit a pull request

## Development Setup

### Prerequisites

- Java 8 or higher
- Maven 3.6+ or Gradle 6.0+
- Git

### Setting Up the Development Environment

1. Clone the repository:
   ```bash
   git clone https://github.com/featureflagshq/java-sdk.git
   cd java-sdk
   ```

2. Install dependencies:
   ```bash
   mvn clean install
   ```

3. Run tests to verify setup:
   ```bash
   mvn test
   ```

### IDE Setup

#### IntelliJ IDEA
1. Import the project as a Maven project
2. Configure code style settings (see `.editorconfig`)
3. Enable automatic import organization

#### Eclipse
1. Import as an existing Maven project
2. Configure formatter settings
3. Enable save actions for import organization

## Making Changes

### Branch Naming

Use descriptive branch names with prefixes:
- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Critical fixes
- `docs/` - Documentation changes
- `refactor/` - Code refactoring

Examples:
- `feature/add-retry-mechanism`
- `bugfix/fix-null-pointer-exception`
- `docs/update-api-reference`

### Commit Messages

Follow conventional commit format:
```
type(scope): description

Longer explanation if needed

Fixes #123
```

Types:
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation
- `style` - Code style changes
- `refactor` - Code refactoring
- `test` - Adding tests
- `chore` - Maintenance tasks

Examples:
```
feat(sdk): add circuit breaker pattern
fix(validation): handle null user segments
docs(readme): update installation instructions
```

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=FeatureFlagsHQSDKTest

# Run with coverage
mvn test jacoco:report
```

### Test Categories

1. **Unit Tests** - Fast, isolated tests
2. **Integration Tests** - Tests with external dependencies
3. **Performance Tests** - Load and performance validation

### Writing Tests

- Write tests for all new functionality
- Maintain or improve test coverage
- Use descriptive test names
- Follow AAA pattern (Arrange, Act, Assert)
- Use proper test data and mocks

Example:
```java
@Test
@DisplayName("Should return default value for non-existent flag")
void shouldReturnDefaultValueForNonExistentFlag() {
    // Arrange
    String userId = "test-user";
    String flagName = "non-existent-flag";
    boolean defaultValue = true;

    // Act
    boolean result = sdk.getBool(userId, flagName, defaultValue);

    // Assert
    assertTrue(result);
}
```

## Pull Request Process

### Before Submitting

1. Ensure all tests pass
2. Update documentation if needed
3. Add/update CHANGELOG.md entry
4. Verify code follows style guidelines
5. Squash commits if necessary

### Pull Request Template

Include:
- **Description** - What changes were made and why
- **Type of Change** - Bug fix, feature, documentation, etc.
- **Testing** - How the changes were tested
- **Checklist** - Completed verification steps

### Review Process

1. Automated checks must pass (CI/CD)
2. At least one maintainer review required
3. Address all review comments
4. Maintain clean commit history

## Coding Standards

### Java Style Guide

- Follow Google Java Style Guide
- Use meaningful variable and method names
- Keep methods under 50 lines when possible
- Add JavaDoc for public APIs
- Use proper exception handling

### Code Formatting

- 4 spaces for indentation (no tabs)
- 120 character line limit
- Consistent bracket placement
- Organize imports automatically

### Documentation

- Update JavaDoc for public methods
- Include parameter descriptions
- Document complex logic with comments
- Keep README.md up to date

### Security Guidelines

- Never commit credentials or secrets
- Validate all user inputs
- Use secure coding practices
- Follow OWASP guidelines

## Security

### Reporting Security Issues

Please report security vulnerabilities to [security@featureflagshq.com](mailto:security@featureflagshq.com) rather than opening public issues.

### Security Best Practices

- Input validation on all external data
- Secure handling of credentials
- Protection against injection attacks
- Proper error handling without information leakage

## Development Workflow

### 1. Issue Creation
- Check existing issues first
- Use issue templates
- Provide clear reproduction steps for bugs
- Include acceptance criteria for features

### 2. Development
- Create feature branch from `main`
- Make focused, logical commits
- Write tests alongside code
- Update documentation

### 3. Testing
- Run full test suite
- Test edge cases
- Verify performance impact
- Test with different Java versions

### 4. Documentation
- Update API documentation
- Add/update examples
- Update CHANGELOG.md
- Review README.md

### 5. Submission
- Create pull request
- Respond to feedback
- Make necessary changes
- Squash commits if requested

## Release Process

Releases follow semantic versioning (SemVer):
- **MAJOR** - Breaking changes
- **MINOR** - New features (backward compatible)
- **PATCH** - Bug fixes (backward compatible)

## Questions and Support

- 📖 [Documentation](https://docs.featureflagshq.com)
- 💬 [Discussions](https://github.com/featureflagshq/java-sdk/discussions)
- 🐛 [Issues](https://github.com/featureflagshq/java-sdk/issues)
- 📧 [Email](mailto:hello@featureflagshq.com)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

Thank you for contributing to FeatureFlagsHQ! 🎉