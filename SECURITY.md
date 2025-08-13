# Security Policy

## Supported Versions

We provide security updates for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

The FeatureFlagsHQ team takes security seriously. If you discover a security vulnerability, please follow these steps:

### 1. Do NOT Open a Public Issue

Please do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.

### 2. Send a Private Report

Instead, please send an email to **[security@featureflagshq.com](mailto:security@featureflagshq.com)** with the following information:

- **Subject**: "Security Vulnerability Report - Java SDK"
- **Description**: Detailed description of the vulnerability
- **Steps to Reproduce**: Clear steps to reproduce the issue
- **Impact**: Potential impact and severity assessment
- **Suggested Fix**: If you have suggestions for fixing the issue

### 3. What to Include

Please include as much of the following information as possible:

- Type of issue (e.g., buffer overflow, SQL injection, cross-site scripting, etc.)
- Full paths of source file(s) related to the manifestation of the issue
- The location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit it

### 4. Response Timeline

- **Initial Response**: Within 24 hours of report submission
- **Confirmation**: Within 72 hours we will confirm if the issue is a vulnerability
- **Update**: Weekly updates on the progress of fixing the vulnerability
- **Resolution**: Target resolution within 90 days for most issues

### 5. Disclosure Policy

- We ask that you give us a reasonable amount of time to fix the issue before public disclosure
- We will work with you to ensure we understand the issue fully
- We will credit you in our security advisory (unless you prefer to remain anonymous)
- We will coordinate the disclosure timeline with you

## Security Best Practices

### For Users

When using the FeatureFlagsHQ Java SDK:

1. **Secure Credential Storage**
   - Store client secrets in environment variables or secure vaults
   - Never commit credentials to version control
   - Rotate credentials regularly

2. **Network Security**
   - Use HTTPS endpoints only in production
   - Implement proper TLS certificate validation
   - Consider using API gateways or proxies for additional security

3. **Input Validation**
   - Validate user IDs and segment data before passing to the SDK
   - Be cautious with user-provided flag names
   - Sanitize any data that might be logged

4. **Monitoring**
   - Monitor SDK usage patterns
   - Set up alerts for authentication failures
   - Review SDK logs regularly

### For Contributors

When contributing to the SDK:

1. **Secure Coding Practices**
   - Follow OWASP secure coding guidelines
   - Validate all inputs at API boundaries
   - Use parameterized queries/prepared statements
   - Implement proper error handling without information leakage

2. **Dependencies**
   - Keep dependencies up to date
   - Use tools like Dependabot for vulnerability scanning
   - Review dependency security advisories

3. **Testing**
   - Include security test cases
   - Test with malicious inputs
   - Verify proper input sanitization

## Security Features

The FeatureFlagsHQ Java SDK includes several security features:

### Input Validation
- All user inputs are validated and sanitized
- Protection against injection attacks
- Length limits on string inputs
- Character filtering for dangerous content

### Authentication
- HMAC-SHA256 signature-based authentication
- Timestamp-based request validation
- Secure credential handling

### Data Protection
- Sensitive data masking in logs
- No sensitive information in error messages
- Secure transmission over HTTPS only

### Resilience
- Circuit breaker pattern prevents abuse
- Rate limiting to prevent DoS
- Graceful degradation during failures

## Known Security Considerations

### 1. Client-Side Usage
- The SDK is designed for server-side use
- Client secrets should never be exposed to client-side code
- Consider using a proxy/gateway for client-side applications

### 2. Logging
- The SDK may log user IDs and flag names
- Ensure your logging infrastructure is secure
- Consider log retention and access policies

### 3. Network Traffic
- All API calls contain user identification information
- Flag values are transmitted in responses
- Use appropriate network security measures

## Vulnerability Response Process

When a security vulnerability is confirmed:

1. **Assessment**: We assess the severity and impact
2. **Development**: We develop and test a fix
3. **Testing**: We thoroughly test the fix across supported versions
4. **Release**: We release patched versions
5. **Advisory**: We publish a security advisory
6. **Notification**: We notify users through multiple channels

## Security Updates

Security updates are distributed through:

- GitHub Security Advisories
- Maven Central with updated versions
- Email notifications to registered users
- Security bulletin on our website

## Contact Information

- **Security Email**: [security@featureflagshq.com](mailto:security@featureflagshq.com)
- **General Support**: [hello@featureflagshq.com](mailto:hello@featureflagshq.com)
- **Documentation**: [https://docs.featureflagshq.com/security](https://docs.featureflagshq.com/security)

## Acknowledgments

We would like to thank the security researchers and community members who help keep FeatureFlagsHQ secure by responsibly disclosing vulnerabilities.

---

*This security policy is subject to change. Please check back regularly for updates.*