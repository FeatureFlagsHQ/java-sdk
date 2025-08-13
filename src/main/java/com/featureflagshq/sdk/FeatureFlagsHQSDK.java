package com.featureflagshq.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * FeatureFlagsHQ Java SDK - Core functionality with Enhanced Logging
 */
public class FeatureFlagsHQSDK implements AutoCloseable {
    
    // Constants
    private static final String SDK_VERSION = "1.0.0";
    private static final String DEFAULT_API_BASE_URL = "https://api.featureflagshq.com";
    private static final String COMPANY_NAME = "FeatureFlagsHQ";
    private static final String USER_AGENT_PREFIX = COMPANY_NAME + "-Java-SDK";
    
    private static final int MAX_USER_ID_LENGTH = 255;
    private static final int MAX_FLAG_NAME_LENGTH = 255;
    private static final long POLLING_INTERVAL = 300000; // 5 minutes in ms
    private static final long LOG_UPLOAD_INTERVAL = 120000; // 2 minutes in ms
    private static final int MAX_UNIQUE_USERS_TRACKED = 10000;
    private static final int MAX_UNIQUE_FLAGS_TRACKED = 1000;
    private static final boolean ENABLE_LOGGING = false;
    
    private static final Logger logger = Logger.getLogger(FeatureFlagsHQSDK.class.getName());
    
    // Security patterns
    private static final Pattern[] SENSITIVE_PATTERNS = {
        Pattern.compile("secret[\"']?\\s*[:=]\\s*[\"']?([^\"'\\s]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("signature[\"']?\\s*[:=]\\s*[\"']?([^\"'\\s]+)", Pattern.CASE_INSENSITIVE)
    };
    
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_@\\.\\-\\+]+$");
    private static final Pattern FLAG_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]+$");
    private static final String[] DANGEROUS_CHARS = {"\n", "\r", "\0", "\t", "\u001b"};
    private static final String[] SQL_PATTERNS = {"--", ";", "/*", "*/", "union", "select", "insert", "delete", "update", "drop"};
    
    // Configuration
    private final String clientId;
    private final String clientSecret;
    private final String apiBaseUrl;
    private final String environment;
    private final int timeout;
    private final int maxRetries;
    private final boolean offlineMode;
    private final boolean enableMetrics;
    private final Consumer<FlagChange> onFlagChange;
    
    // Internal state
    private final Map<String, Map<String, Object>> flags = new ConcurrentHashMap<>();
    private final String sessionId = UUID.randomUUID().toString();
    private final BlockingQueue<LogEntry> logsQueue = new LinkedBlockingQueue<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean stopEvent = new AtomicBoolean(false);
    private final CountDownLatch initializationComplete = new CountDownLatch(1);
    
    // Statistics
    private final Stats stats = new Stats();
    private final CircuitBreaker circuitBreaker = new CircuitBreaker();
    private final Map<String, RateLimit> rateLimits = new ConcurrentHashMap<>();
    
    // Background threads
    private ScheduledExecutorService scheduler;
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;
    
    // System info
    private final Map<String, Object> systemInfo;
    
    public static class Builder {
        private String clientId;
        private String clientSecret;
        private String apiBaseUrl = DEFAULT_API_BASE_URL;
        private String environment = "production";
        private int timeout = 30;
        private int maxRetries = 3;
        private boolean offlineMode = false;
        private boolean enableMetrics = true;
        private Consumer<FlagChange> onFlagChange;
        
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }
        
        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }
        
        public Builder apiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }
        
        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }
        
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder offlineMode(boolean offlineMode) {
            this.offlineMode = offlineMode;
            return this;
        }
        
        public Builder enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }
        
        public Builder onFlagChange(Consumer<FlagChange> onFlagChange) {
            this.onFlagChange = onFlagChange;
            return this;
        }
        
        public FeatureFlagsHQSDK build() {
            // Get credentials from environment if not provided
            if (clientId == null) {
                clientId = System.getenv("FEATUREFLAGSHQ_CLIENT_ID");
                if (clientId == null) {
                    clientId = System.getenv("FEATUREFLAGSHQ_CLIENT_KEY");
                }
            }
            if (clientSecret == null) {
                clientSecret = System.getenv("FEATUREFLAGSHQ_CLIENT_SECRET");
            }
            if (environment == null) {
                environment = System.getenv().getOrDefault("FEATUREFLAGSHQ_ENVIRONMENT", "production");
            }
            
            if (clientId == null || clientSecret == null) {
                throw new IllegalArgumentException("clientId and clientSecret are required");
            }
            
            return new FeatureFlagsHQSDK(this);
        }
    }
    
    private FeatureFlagsHQSDK(Builder builder) {
        this.clientId = validateString(builder.clientId, "clientId");
        this.clientSecret = validateString(builder.clientSecret, "clientSecret");
        this.apiBaseUrl = validateUrl(builder.apiBaseUrl);
        this.environment = validateString(builder.environment, "environment");
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
        this.offlineMode = builder.offlineMode;
        this.enableMetrics = builder.enableMetrics;
        this.onFlagChange = builder.onFlagChange;
        
        this.systemInfo = getSystemInfo();
        this.objectMapper = new ObjectMapper();
        
        initializeHttpClient();
        initialize();
    }
    
    private void initializeHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS);
        
        if (ENABLE_LOGGING) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }
        
        this.httpClient = builder.build();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }
    
    private Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("platform", System.getProperty("os.name"));
        info.put("java_version", System.getProperty("java.version"));
        info.put("hostname", getHostname());
        info.put("process_id", getProcessId());
        info.put("cpu_count", Runtime.getRuntime().availableProcessors());
        info.put("memory_total", Runtime.getRuntime().maxMemory());
        return info;
    }
    
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private long getProcessId() {
        try {
            return ProcessHandle.current().pid();
        } catch (Exception e) {
            return -1;
        }
    }
    
    private String validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("API base URL must be a non-empty string");
        }
        
        try {
            URL parsed = new URL(url);
            if (!parsed.getProtocol().equals("http") && !parsed.getProtocol().equals("https")) {
                throw new IllegalArgumentException("Invalid URL scheme. Only http and https are allowed");
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        }
        
        return url.replaceAll("/$", "");
    }
    
    private String validateString(String value, String fieldName) {
        return validateString(value, fieldName, 255);
    }
    
    private String validateString(String value, String fieldName, int maxLength) {
        if (value == null || !(value instanceof String)) {
            throw new IllegalArgumentException(fieldName + " must be a string");
        }
        
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " too long (max " + maxLength + " characters)");
        }
        
        // Check for dangerous characters
        for (String dangerousChar : DANGEROUS_CHARS) {
            if (value.contains(dangerousChar)) {
                throw new IllegalArgumentException(fieldName + " contains invalid characters");
            }
        }
        
        // Basic SQL injection prevention
        String valueLower = value.toLowerCase();
        for (String pattern : SQL_PATTERNS) {
            if (valueLower.contains(pattern)) {
                throw new IllegalArgumentException(fieldName + " contains potentially dangerous content");
            }
        }
        
        return value;
    }
    
    private String validateUserId(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        
        userId = validateString(userId, "userId", MAX_USER_ID_LENGTH);
        
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            if (ENABLE_LOGGING) {
                logger.warning("Potentially unsafe userId pattern: " + userId.substring(0, Math.min(50, userId.length())) + "...");
            }
        }
        
        return userId;
    }
    
    private String validateFlagName(String flagName) {
        if (flagName == null) {
            throw new IllegalArgumentException("flagName cannot be null");
        }
        
        flagName = validateString(flagName, "flagName", MAX_FLAG_NAME_LENGTH);
        
        if (!FLAG_NAME_PATTERN.matcher(flagName).matches()) {
            throw new IllegalArgumentException("flagName contains invalid characters");
        }
        
        return flagName;
    }
    
    private boolean rateLimitCheck(String userId) {
        if (offlineMode) return true;
        
        long currentTime = System.currentTimeMillis();
        
        // Clean up old entries
        rateLimits.entrySet().removeIf(entry -> currentTime - entry.getValue().lastTime > 60000);
        
        RateLimit limit = rateLimits.get(userId);
        if (limit != null) {
            if (currentTime - limit.lastTime < 60000) {
                if (limit.count > 1000) { // Max 1000 requests per minute per user
                    if (ENABLE_LOGGING) {
                        logger.warning("Rate limit exceeded for user: " + userId);
                    }
                    return false;
                }
                limit.count++;
                limit.lastTime = currentTime;
            } else {
                limit.count = 1;
                limit.lastTime = currentTime;
            }
        } else {
            rateLimits.put(userId, new RateLimit(1, currentTime));
        }
        
        return true;
    }
    
    private boolean checkCircuitBreaker() {
        if (circuitBreaker.state == CircuitBreakerState.OPEN) {
            if (circuitBreaker.lastFailureTime != null &&
                (System.currentTimeMillis() - circuitBreaker.lastFailureTime) > circuitBreaker.recoveryTimeout) {
                circuitBreaker.state = CircuitBreakerState.HALF_OPEN;
                if (ENABLE_LOGGING) {
                    logger.info("Circuit breaker moved to half-open state");
                }
                return true;
            }
            return false;
        }
        return true;
    }
    
    private void recordApiSuccess() {
        synchronized (stats) {
            stats.apiCalls.successful++;
            stats.apiCalls.total++;
        }
        
        if (circuitBreaker.state == CircuitBreakerState.HALF_OPEN) {
            circuitBreaker.state = CircuitBreakerState.CLOSED;
            circuitBreaker.failureCount = 0;
            if (ENABLE_LOGGING) {
                logger.info("Circuit breaker closed after successful call");
            }
        }
    }
    
    private void recordApiFailure(String errorType) {
        synchronized (stats) {
            stats.apiCalls.failed++;
            stats.apiCalls.total++;
            stats.errors.put(errorType, stats.errors.getOrDefault(errorType, 0) + 1);
        }
        
        circuitBreaker.failureCount++;
        circuitBreaker.lastFailureTime = System.currentTimeMillis();
        
        if (circuitBreaker.failureCount >= circuitBreaker.failureThreshold) {
            circuitBreaker.state = CircuitBreakerState.OPEN;
            if (ENABLE_LOGGING) {
                logger.warning("Circuit breaker opened due to repeated failures");
            }
        }
    }
    
    private String generateSignature(String payload, String timestamp) {
        try {
            String message = clientId + ":" + timestamp + ":" + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] signature = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    private Map<String, String> getHeaders(String payload) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = generateSignature(payload, timestamp);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-SDK-Provider", COMPANY_NAME);
        headers.put("X-Client-ID", clientId);
        headers.put("X-Timestamp", timestamp);
        headers.put("X-Signature", signature);
        headers.put("X-Session-ID", sessionId);
        headers.put("X-SDK-Version", SDK_VERSION);
        headers.put("X-Environment", environment);
        headers.put("User-Agent", USER_AGENT_PREFIX + "/" + SDK_VERSION);
        
        return headers;
    }
    
    private Map<String, Map<String, Object>> fetchFlags() {
        if (offlineMode || !checkCircuitBreaker()) {
            return new HashMap<>();
        }
        
        try {
            String url = apiBaseUrl + "/v1/flags/";
            Map<String, String> headers = getHeaders("");
            
            Request.Builder requestBuilder = new Request.Builder().url(url);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }
            
            Response response = httpClient.newCall(requestBuilder.build()).execute();
            
            if (response.code() == 401) {
                recordApiFailure("auth_errors");
                if (ENABLE_LOGGING) {
                    logger.severe("Authentication failed - check credentials");
                }
                return new HashMap<>();
            }
            
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            
            recordApiSuccess();
            
            String responseBody = response.body().string();
            JsonNode data = objectMapper.readTree(responseBody);
            Map<String, Map<String, Object>> flags = new HashMap<>();
            
            if (data.has("data") && data.get("data").isArray()) {
                for (JsonNode flagNode : data.get("data")) {
                    if (flagNode.has("name")) {
                        String flagName = flagNode.get("name").asText();
                        if (!flagName.isEmpty()) {
                            flags.put(flagName, objectMapper.convertValue(flagNode, Map.class));
                        }
                    }
                }
            }
            
            if (ENABLE_LOGGING) {
                logger.info("Fetched " + flags.size() + " flags from server");
            }
            return flags;
            
        } catch (Exception e) {
            recordApiFailure("network_errors");
            if (ENABLE_LOGGING) {
                logger.warning("Failed to fetch flags: " + e.getMessage());
            }
            return new HashMap<>();
        }
    }
    
    private EvaluationResult evaluateFlag(Map<String, Object> flagData, String userId, Map<String, Object> segments) {
        long startTime = System.currentTimeMillis();
        
        EvaluationContext context = new EvaluationContext();
        context.flagActive = (Boolean) flagData.getOrDefault("is_active", true);
        context.flagFound = true;
        context.defaultValueUsed = false;
        context.segmentsMatched = new ArrayList<>();
        context.segmentsEvaluated = new ArrayList<>();
        context.rolloutQualified = false;
        context.reason = "active_flag";
        
        if (!context.flagActive) {
            context.defaultValueUsed = true;
            context.reason = "flag_inactive";
            Object value = getDefaultValue((String) flagData.getOrDefault("type", "string"));
            long evaluationTime = System.currentTimeMillis() - startTime;
            context.totalSdkTimeMs = evaluationTime;
            return new EvaluationResult(value, context);
        }
        
        // Check segments
        List<Map<String, Object>> flagSegments = (List<Map<String, Object>>) flagData.get("segments");
        if (flagSegments != null) {
            List<Map<String, Object>> activeSegments = flagSegments.stream()
                    .filter(seg -> (Boolean) seg.getOrDefault("is_active", true))
                    .collect(java.util.stream.Collectors.toList());
            
            if (!activeSegments.isEmpty()) {
                List<String> segmentsMatched = new ArrayList<>();
                List<String> segmentsEvaluated = new ArrayList<>();
                
                for (Map<String, Object> segment : activeSegments) {
                    String segmentName = (String) segment.getOrDefault("name", "");
                    segmentsEvaluated.add(segmentName);
                    
                    if (checkSegmentMatch(segment, segments != null ? segments : new HashMap<>())) {
                        segmentsMatched.add(segmentName);
                    }
                }
                
                context.segmentsMatched = segmentsMatched;
                context.segmentsEvaluated = segmentsEvaluated;
                
                synchronized (stats) {
                    stats.segmentMatches += segmentsMatched.size();
                }
                
                if (segmentsMatched.isEmpty()) {
                    context.defaultValueUsed = true;
                    context.reason = "segment_not_matched";
                    Object value = getDefaultValue((String) flagData.getOrDefault("type", "string"));
                    long evaluationTime = System.currentTimeMillis() - startTime;
                    context.totalSdkTimeMs = evaluationTime;
                    return new EvaluationResult(value, context);
                }
            }
        }
        
        // Check rollout percentage
        Map<String, Object> rollout = (Map<String, Object>) flagData.get("rollout");
        int rolloutPercentage = rollout != null ? (Integer) rollout.getOrDefault("percentage", 100) : 100;
        
        if (rolloutPercentage < 100) {
            synchronized (stats) {
                stats.rolloutEvaluations++;
            }
            
            try {
                String hashInput = flagData.get("name") + ":" + userId;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8));
                int userPercentage = (hash[0] & 0xFF) % 100;
                
                if (userPercentage < rolloutPercentage) {
                    context.rolloutQualified = true;
                    context.reason = "rollout_qualified";
                } else {
                    context.defaultValueUsed = true;
                    context.reason = "rollout_not_qualified";
                    Object value = getDefaultValue((String) flagData.getOrDefault("type", "string"));
                    long evaluationTime = System.currentTimeMillis() - startTime;
                    context.totalSdkTimeMs = evaluationTime;
                    return new EvaluationResult(value, context);
                }
            } catch (NoSuchAlgorithmException e) {
                // Fallback to simple hash
                int userPercentage = Math.abs((userId + flagData.get("name")).hashCode()) % 100;
                if (userPercentage >= rolloutPercentage) {
                    context.defaultValueUsed = true;
                    context.reason = "rollout_not_qualified";
                    Object value = getDefaultValue((String) flagData.getOrDefault("type", "string"));
                    long evaluationTime = System.currentTimeMillis() - startTime;
                    context.totalSdkTimeMs = evaluationTime;
                    return new EvaluationResult(value, context);
                }
            }
        }
        
        Object value = convertValue(flagData.get("value"), (String) flagData.getOrDefault("type", "string"));
        long evaluationTime = System.currentTimeMillis() - startTime;
        context.totalSdkTimeMs = evaluationTime;
        
        // Update evaluation time stats
        synchronized (stats) {
            stats.evaluationTimes.totalMs += evaluationTime;
            stats.evaluationTimes.count++;
            stats.evaluationTimes.minMs = Math.min(stats.evaluationTimes.minMs, evaluationTime);
            stats.evaluationTimes.maxMs = Math.max(stats.evaluationTimes.maxMs, evaluationTime);
        }
        
        return new EvaluationResult(value, context);
    }
    
    private boolean checkSegmentMatch(Map<String, Object> segment, Map<String, Object> userSegments) {
        try {
            String segmentName = (String) segment.get("name");
            if (segmentName == null || !userSegments.containsKey(segmentName)) {
                return false;
            }
            
            String comparator = (String) segment.getOrDefault("comparator", "==");
            Object segmentValue = segment.get("value");
            String segmentType = (String) segment.getOrDefault("type", "str");
            Object userValue = userSegments.get(segmentName);
            
            // Convert values based on type
            Object userVal, segVal;
            
            if ("int".equals(segmentType) || "integer".equals(segmentType)) {
                userVal = ((Number) userValue).intValue();
                segVal = ((Number) segmentValue).intValue();
            } else if ("float".equals(segmentType)) {
                userVal = ((Number) userValue).doubleValue();
                segVal = ((Number) segmentValue).doubleValue();
            } else if ("bool".equals(segmentType) || "boolean".equals(segmentType)) {
                userVal = userValue instanceof Boolean ? (Boolean) userValue : 
                          String.valueOf(userValue).toLowerCase().matches("true|1|yes");
                segVal = segmentValue instanceof Boolean ? (Boolean) segmentValue : 
                         String.valueOf(segmentValue).toLowerCase().matches("true|1|yes");
            } else {
                userVal = String.valueOf(userValue);
                segVal = String.valueOf(segmentValue);
            }
            
            // Apply comparator
            switch (comparator) {
                case "==":
                    return userVal.equals(segVal);
                case "!=":
                    return !userVal.equals(segVal);
                case ">":
                    return ((Comparable) userVal).compareTo(segVal) > 0;
                case "<":
                    return ((Comparable) userVal).compareTo(segVal) < 0;
                case ">=":
                    return ((Comparable) userVal).compareTo(segVal) >= 0;
                case "<=":
                    return ((Comparable) userVal).compareTo(segVal) <= 0;
                case "contains":
                    return String.valueOf(userVal).contains(String.valueOf(segVal));
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private Object convertValue(Object value, String valueType) {
        try {
            if ("bool".equals(valueType)) {
                if (value instanceof Boolean) return value;
                return String.valueOf(value).toLowerCase().matches("true|1|yes");
            } else if ("int".equals(valueType)) {
                return ((Number) value).intValue();
            } else if ("float".equals(valueType)) {
                return ((Number) value).doubleValue();
            } else if ("json".equals(valueType)) {
                if (value instanceof Map || value instanceof List) return value;
                return objectMapper.readValue(String.valueOf(value), Object.class);
            } else {
                return String.valueOf(value);
            }
        } catch (Exception e) {
            return getDefaultValue(valueType);
        }
    }
    
    private Object getDefaultValue(String valueType) {
        switch (valueType) {
            case "bool": return false;
            case "int": return 0;
            case "float": return 0.0;
            case "json": return new HashMap<>();
            default: return "";
        }
    }
    
    private void logAccess(String userId, String flagName, Object flagValue, EvaluationContext evaluationContext,
                          long evaluationTimeMs, Map<String, Object> segments) {
        if (!enableMetrics) return;
        
        LogEntry logEntry = new LogEntry();
        logEntry.userId = userId;
        logEntry.flagName = flagName;
        logEntry.flagValue = flagValue;
        logEntry.timestamp = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        logEntry.sessionId = sessionId;
        logEntry.evaluationTimeMs = evaluationTimeMs;
        logEntry.evaluationContext = evaluationContext;
        logEntry.segments = segments != null ? segments : new HashMap<>();
        logEntry.metadata = new HashMap<>();
        logEntry.metadata.put("sdk_version", SDK_VERSION);
        logEntry.metadata.put("environment", environment);
        
        logsQueue.offer(logEntry);
        
        // Update statistics
        synchronized (stats) {
            stats.totalUserAccesses++;
            stats.uniqueUsers.add(userId);
            stats.uniqueFlagsAccessed.add(flagName);
        }
        
        // Cleanup stats periodically
        if (stats.totalUserAccesses % 1000 == 0) {
            cleanupOldStats();
        }
    }
    
    private void cleanupOldStats() {
        synchronized (stats) {
            if (stats.uniqueUsers.size() > MAX_UNIQUE_USERS_TRACKED) {
                List<String> usersList = new ArrayList<>(stats.uniqueUsers);
                stats.uniqueUsers = new HashSet<>(usersList.subList(usersList.size() - MAX_UNIQUE_USERS_TRACKED, usersList.size()));
                if (ENABLE_LOGGING) {
                    logger.info("Cleaned up old user stats, keeping " + MAX_UNIQUE_USERS_TRACKED + " most recent");
                }
            }
            
            if (stats.uniqueFlagsAccessed.size() > MAX_UNIQUE_FLAGS_TRACKED) {
                List<String> flagsList = new ArrayList<>(stats.uniqueFlagsAccessed);
                stats.uniqueFlagsAccessed = new HashSet<>(flagsList.subList(flagsList.size() - MAX_UNIQUE_FLAGS_TRACKED, flagsList.size()));
                if (ENABLE_LOGGING) {
                    logger.info("Cleaned up old flag stats, keeping " + MAX_UNIQUE_FLAGS_TRACKED + " most recent");
                }
            }
        }
    }
    
    private void uploadLogs() {
        if (offlineMode || logsQueue.isEmpty() || !checkCircuitBreaker()) {
            return;
        }
        
        List<LogEntry> logs = new ArrayList<>();
        while (!logsQueue.isEmpty() && logs.size() < 100) {
            LogEntry entry = logsQueue.poll();
            if (entry != null) {
                logs.add(entry);
            }
        }
        
        if (logs.isEmpty()) return;
        
        try {
            String url = apiBaseUrl + "/v1/logs/batch/";
            Map<String, Object> payload = new HashMap<>();
            payload.put("logs", logs);
            payload.put("session_metadata", getSessionMetadata());
            
            String payloadStr = objectMapper.writeValueAsString(payload);
            Map<String, String> headers = getHeaders(payloadStr);
            
            RequestBody body = RequestBody.create(payloadStr, MediaType.get("application/json"));
            Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }
            
            Response response = httpClient.newCall(requestBuilder.build()).execute();
            response.close();
            
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            
            recordApiSuccess();
            stats.lastLogUpload = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            
            if (ENABLE_LOGGING) {
                logger.info("Uploaded " + logs.size() + " log entries");
            }
            
        } catch (Exception e) {
            recordApiFailure("network_errors");
            if (ENABLE_LOGGING) {
                logger.warning("Failed to upload logs: " + e.getMessage());
            }
            
            // Put logs back in queue for retry (limit to prevent memory bloat)
            if (logs.size() <= 10) {
                for (LogEntry log : logs) {
                    logsQueue.offer(log);
                }
            }
        }
    }
    
    private Map<String, Object> getSessionMetadata() {
        synchronized (stats) {
            double avgMs = stats.evaluationTimes.count > 0 ? 
                          (double) stats.evaluationTimes.totalMs / stats.evaluationTimes.count : 0;
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("session_id", sessionId);
            
            Map<String, Object> env = new HashMap<>();
            env.put("name", environment);
            metadata.put("environment", env);
            
            metadata.put("system_info", systemInfo);
            
            Map<String, Object> statsMap = new HashMap<>();
            statsMap.put("total_user_accesses", stats.totalUserAccesses);
            statsMap.put("unique_users_count", stats.uniqueUsers.size());
            statsMap.put("unique_flags_count", stats.uniqueFlagsAccessed.size());
            statsMap.put("segment_matches", stats.segmentMatches);
            statsMap.put("rollout_evaluations", stats.rolloutEvaluations);
            
            Map<String, Object> evalTimes = new HashMap<>();
            evalTimes.put("avg_ms", avgMs);
            evalTimes.put("min_ms", stats.evaluationTimes.minMs == Long.MAX_VALUE ? 0 : stats.evaluationTimes.minMs);
            evalTimes.put("max_ms", stats.evaluationTimes.maxMs);
            evalTimes.put("total_ms", stats.evaluationTimes.totalMs);
            evalTimes.put("count", stats.evaluationTimes.count);
            statsMap.put("evaluation_times", evalTimes);
            
            metadata.put("stats", statsMap);
            
            return metadata;
        }
    }
    
    private void pollingWorker() {
        if (ENABLE_LOGGING) {
            logger.info("Polling worker started");
        }
        
        try {
            while (!stopEvent.get()) {
                try {
                    Map<String, Map<String, Object>> oldFlags = onFlagChange != null ? new HashMap<>(flags) : null;
                    Map<String, Map<String, Object>> newFlags = fetchFlags();
                    
                    if (!newFlags.isEmpty()) {
                        lock.writeLock().lock();
                        try {
                            // Detect changes for callbacks
                            if (onFlagChange != null && oldFlags != null) {
                                for (Map.Entry<String, Map<String, Object>> entry : newFlags.entrySet()) {
                                    String flagName = entry.getKey();
                                    Map<String, Object> newFlagData = entry.getValue();
                                    Map<String, Object> oldFlagData = oldFlags.get(flagName);
                                    
                                    Object oldValue = oldFlagData != null ? oldFlagData.get("value") : null;
                                    Object newValue = newFlagData.get("value");
                                    
                                    if (!Objects.equals(oldValue, newValue)) {
                                        try {
                                            onFlagChange.accept(new FlagChange(flagName, oldValue, newValue));
                                        } catch (Exception e) {
                                            if (ENABLE_LOGGING) {
                                                logger.warning("Error in flag change callback: " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                            
                            flags.putAll(newFlags);
                        } finally {
                            lock.writeLock().unlock();
                        }
                        
                        stats.lastSync = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                        if (ENABLE_LOGGING) {
                            logger.info("Updated flags from polling");
                        }
                    }
                } catch (Exception e) {
                    if (ENABLE_LOGGING) {
                        logger.warning("Error in polling worker: " + e.getMessage());
                    }
                }
                
                try {
                    Thread.sleep(POLLING_INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            if (ENABLE_LOGGING) {
                logger.info("Polling worker stopped");
            }
        }
    }
    
    private void logUploadWorker() {
        if (ENABLE_LOGGING) {
            logger.info("Log upload worker started");
        }
        
        try {
            while (!stopEvent.get()) {
                try {
                    uploadLogs();
                } catch (Exception e) {
                    if (ENABLE_LOGGING) {
                        logger.warning("Error in log upload worker: " + e.getMessage());
                    }
                }
                
                try {
                    Thread.sleep(LOG_UPLOAD_INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            if (ENABLE_LOGGING) {
                logger.info("Log upload worker stopped");
            }
        }
    }
    
    private void initialize() {
        try {
            if (!offlineMode) {
                // Initial fetch
                Map<String, Map<String, Object>> initialFlags = fetchFlags();
                lock.writeLock().lock();
                try {
                    flags.putAll(initialFlags);
                } finally {
                    lock.writeLock().unlock();
                }
                
                // Start background threads
                scheduler.scheduleWithFixedDelay(this::pollingWorker, 0, POLLING_INTERVAL, TimeUnit.MILLISECONDS);
                
                if (enableMetrics) {
                    scheduler.scheduleWithFixedDelay(this::logUploadWorker, 0, LOG_UPLOAD_INTERVAL, TimeUnit.MILLISECONDS);
                }
            }
            
            if (ENABLE_LOGGING) {
                logger.info("SDK initialized successfully");
            }
        } catch (Exception e) {
            if (ENABLE_LOGGING) {
                logger.warning("SDK initialization failed: " + e.getMessage() + ", continuing in degraded mode");
            }
        } finally {
            initializationComplete.countDown();
        }
    }
    
    // Public API methods
    
    public Object get(String userId, String flagName, Object defaultValue, Map<String, Object> segments) {
        // Wait for initialization
        try {
            if (!initializationComplete.await(5, TimeUnit.SECONDS)) {
                if (ENABLE_LOGGING) {
                    logger.warning("Initialization still in progress, proceeding anyway");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Validate inputs
        try {
            userId = validateUserId(userId);
            flagName = validateFlagName(flagName);
        } catch (IllegalArgumentException e) {
            if (ENABLE_LOGGING) {
                logger.warning("Input validation failed: " + e.getMessage());
            }
            return defaultValue;
        }
        
        // Sanitize segments
        if (segments != null) {
            Map<String, Object> cleanSegments = new HashMap<>();
            for (Map.Entry<String, Object> entry : segments.entrySet()) {
                String key = entry.getKey();
                if (key != null && key.length() <= 128) {
                    try {
                        String cleanKey = validateString(key, "segment_key", 128);
                        cleanSegments.put(cleanKey, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        // Skip invalid segment
                    }
                }
            }
            segments = cleanSegments;
        }
        
        // Rate limiting
        if (!offlineMode && !rateLimitCheck(userId)) {
            if (ENABLE_LOGGING) {
                logger.warning("Request blocked due to rate limiting: " + userId);
            }
            return defaultValue;
        }
        
        Map<String, Object> flagData;
        lock.readLock().lock();
        try {
            flagData = flags.get(flagName);
        } finally {
            lock.readLock().unlock();
        }
        
        Object result;
        EvaluationContext evaluationContext;
        long evaluationTimeMs;
        
        if (flagData == null) {
            // Flag not found
            result = defaultValue;
            evaluationContext = new EvaluationContext();
            evaluationContext.flagFound = false;
            evaluationContext.defaultValueUsed = true;
            evaluationContext.reason = "flag_not_found";
            evaluationTimeMs = 0;
        } else {
            // Evaluate flag
            EvaluationResult evalResult = evaluateFlag(flagData, userId, segments);
            result = evalResult.value;
            evaluationContext = evalResult.context;
            evaluationTimeMs = evaluationContext.totalSdkTimeMs;
            
            // Use custom default if evaluation returned default and custom default provided
            if (evaluationContext.defaultValueUsed && defaultValue != null) {
                result = defaultValue;
            }
        }
        
        // Log the access
        logAccess(userId, flagName, result, evaluationContext, evaluationTimeMs, segments);
        
        return result;
    }
    
    public Object get(String userId, String flagName, Object defaultValue) {
        return get(userId, flagName, defaultValue, null);
    }
    
    public Object get(String userId, String flagName) {
        return get(userId, flagName, null, null);
    }
    
    public boolean getBool(String userId, String flagName, boolean defaultValue, Map<String, Object> segments) {
        Object value = get(userId, flagName, defaultValue, segments);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return ((String) value).toLowerCase().matches("true|1|yes");
        }
        return value != null ? Boolean.parseBoolean(String.valueOf(value)) : defaultValue;
    }
    
    public boolean getBool(String userId, String flagName, boolean defaultValue) {
        return getBool(userId, flagName, defaultValue, null);
    }
    
    public boolean getBool(String userId, String flagName) {
        return getBool(userId, flagName, false, null);
    }
    
    public String getString(String userId, String flagName, String defaultValue, Map<String, Object> segments) {
        Object value = get(userId, flagName, defaultValue, segments);
        return value != null ? String.valueOf(value) : defaultValue;
    }
    
    public String getString(String userId, String flagName, String defaultValue) {
        return getString(userId, flagName, defaultValue, null);
    }
    
    public String getString(String userId, String flagName) {
        return getString(userId, flagName, "", null);
    }
    
    public int getInt(String userId, String flagName, int defaultValue, Map<String, Object> segments) {
        Object value = get(userId, flagName, defaultValue, segments);
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return value != null ? Integer.parseInt(String.valueOf(value)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public int getInt(String userId, String flagName, int defaultValue) {
        return getInt(userId, flagName, defaultValue, null);
    }
    
    public int getInt(String userId, String flagName) {
        return getInt(userId, flagName, 0, null);
    }
    
    public double getFloat(String userId, String flagName, double defaultValue, Map<String, Object> segments) {
        Object value = get(userId, flagName, defaultValue, segments);
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return value != null ? Double.parseDouble(String.valueOf(value)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public double getFloat(String userId, String flagName, double defaultValue) {
        return getFloat(userId, flagName, defaultValue, null);
    }
    
    public double getFloat(String userId, String flagName) {
        return getFloat(userId, flagName, 0.0, null);
    }
    
    public Object getJson(String userId, String flagName, Object defaultValue, Map<String, Object> segments) {
        if (defaultValue == null) {
            defaultValue = new HashMap<>();
        }
        
        Object value = get(userId, flagName, defaultValue, segments);
        
        if (value instanceof Map || value instanceof List) {
            return value;
        }
        if (value instanceof String) {
            try {
                return objectMapper.readValue((String) value, Object.class);
            } catch (JsonProcessingException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
    
    public Object getJson(String userId, String flagName, Object defaultValue) {
        return getJson(userId, flagName, defaultValue, null);
    }
    
    public Object getJson(String userId, String flagName) {
        return getJson(userId, flagName, new HashMap<>(), null);
    }
    
    public boolean isFlagEnabledForUser(String userId, String flagName, Map<String, Object> segments) {
        return getBool(userId, flagName, false, segments);
    }
    
    public boolean isFlagEnabledForUser(String userId, String flagName) {
        return isFlagEnabledForUser(userId, flagName, null);
    }
    
    public Map<String, Object> getUserFlags(String userId, Map<String, Object> segments, List<String> flagKeys) {
        try {
            userId = validateUserId(userId);
        } catch (IllegalArgumentException e) {
            if (ENABLE_LOGGING) {
                logger.warning("Invalid userId: " + e.getMessage());
            }
            return new HashMap<>();
        }
        
        Map<String, Object> userFlags = new HashMap<>();
        
        Map<String, Map<String, Object>> flagsToEvaluate;
        lock.readLock().lock();
        try {
            flagsToEvaluate = new HashMap<>(flags);
            if (flagKeys != null) {
                Set<String> validatedKeys = new HashSet<>();
                for (String key : flagKeys) {
                    try {
                        validatedKeys.add(validateFlagName(key));
                    } catch (IllegalArgumentException e) {
                        // Skip invalid key
                    }
                }
                flagsToEvaluate.entrySet().removeIf(entry -> !validatedKeys.contains(entry.getKey()));
            }
        } finally {
            lock.readLock().unlock();
        }
        
        for (Map.Entry<String, Map<String, Object>> entry : flagsToEvaluate.entrySet()) {
            String flagKey = entry.getKey();
            Map<String, Object> flagData = entry.getValue();
            
            try {
                EvaluationResult evalResult = evaluateFlag(flagData, userId, segments);
                userFlags.put(flagKey, evalResult.value);
                
                // Log each flag access
                logAccess(userId, flagKey, evalResult.value, evalResult.context, 
                         evalResult.context.totalSdkTimeMs, segments);
            } catch (Exception e) {
                if (ENABLE_LOGGING) {
                    logger.warning("Error evaluating flag " + flagKey + " for user " + userId + ": " + e.getMessage());
                }
                // Set default based on flag type
                String flagType = (String) flagData.getOrDefault("type", "string");
                userFlags.put(flagKey, getDefaultValue(flagType));
            }
        }
        
        return userFlags;
    }
    
    public Map<String, Object> getUserFlags(String userId, Map<String, Object> segments) {
        return getUserFlags(userId, segments, null);
    }
    
    public Map<String, Object> getUserFlags(String userId) {
        return getUserFlags(userId, null, null);
    }
    
    public Map<String, Map<String, Object>> getAllFlags() {
        lock.readLock().lock();
        try {
            return new HashMap<>(flags);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean refreshFlags() {
        if (offlineMode) {
            if (ENABLE_LOGGING) {
                logger.warning("Cannot refresh flags in offline mode");
            }
            return false;
        }
        
        try {
            Map<String, Map<String, Object>> newFlags = fetchFlags();
            if (!newFlags.isEmpty()) {
                lock.writeLock().lock();
                try {
                    flags.putAll(newFlags);
                } finally {
                    lock.writeLock().unlock();
                }
                stats.lastSync = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                if (ENABLE_LOGGING) {
                    logger.info("Flags manually refreshed");
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            if (ENABLE_LOGGING) {
                logger.warning("Manual refresh failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    public boolean flushLogs() {
        if (offlineMode || !enableMetrics) {
            if (ENABLE_LOGGING) {
                logger.warning("Cannot flush logs in offline mode or with metrics disabled");
            }
            return false;
        }
        
        try {
            uploadLogs();
            if (ENABLE_LOGGING) {
                logger.info("Logs manually flushed");
            }
            return true;
        } catch (Exception e) {
            if (ENABLE_LOGGING) {
                logger.warning("Manual log flush failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    public Map<String, Object> getStats() {
        try {
            synchronized (stats) {
                double avgMs = stats.evaluationTimes.count > 0 ? 
                              (double) stats.evaluationTimes.totalMs / stats.evaluationTimes.count : 0;
                
                Map<String, Object> result = new HashMap<>();
                result.put("total_user_accesses", stats.totalUserAccesses);
                result.put("unique_users_count", stats.uniqueUsers.size());
                result.put("unique_flags_count", stats.uniqueFlagsAccessed.size());
                result.put("segment_matches", stats.segmentMatches);
                result.put("rollout_evaluations", stats.rolloutEvaluations);
                result.put("last_sync", stats.lastSync);
                result.put("last_log_upload", stats.lastLogUpload);
                result.put("api_calls", Map.of(
                    "successful", stats.apiCalls.successful,
                    "failed", stats.apiCalls.failed,
                    "total", stats.apiCalls.total
                ));
                result.put("errors", new HashMap<>(stats.errors));
                result.put("session_id", sessionId);
                result.put("cached_flags_count", flags.size());
                result.put("pending_user_logs", logsQueue.size());
                result.put("circuit_breaker", Map.of(
                    "state", circuitBreaker.state.toString(),
                    "failure_count", circuitBreaker.failureCount
                ));
                result.put("evaluation_times", Map.of(
                    "avg_ms", avgMs,
                    "min_ms", stats.evaluationTimes.minMs == Long.MAX_VALUE ? 0 : stats.evaluationTimes.minMs,
                    "max_ms", stats.evaluationTimes.maxMs,
                    "total_ms", stats.evaluationTimes.totalMs,
                    "count", stats.evaluationTimes.count
                ));
                result.put("configuration", Map.of(
                    "polling_interval", POLLING_INTERVAL,
                    "log_upload_interval", LOG_UPLOAD_INTERVAL,
                    "offline_mode", offlineMode,
                    "enable_metrics", enableMetrics,
                    "environment", environment
                ));
                
                return result;
            }
        } catch (Exception e) {
            if (ENABLE_LOGGING) {
                logger.warning("Error getting stats: " + e.getMessage());
            }
            return Map.of("error", e.getMessage());
        }
    }
    
    public Map<String, Object> getHealthCheck() {
        try {
            int cachedFlagsCount;
            lock.readLock().lock();
            try {
                cachedFlagsCount = flags.size();
            } finally {
                lock.readLock().unlock();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", circuitBreaker.state != CircuitBreakerState.OPEN ? "healthy" : "degraded");
            result.put("sdk_version", SDK_VERSION);
            result.put("api_base_url", apiBaseUrl);
            result.put("cached_flags_count", cachedFlagsCount);
            result.put("session_id", sessionId);
            result.put("environment", environment);
            result.put("offline_mode", offlineMode);
            result.put("last_sync", stats.lastSync);
            result.put("circuit_breaker", Map.of(
                "state", circuitBreaker.state.toString(),
                "failure_count", circuitBreaker.failureCount
            ));
            result.put("system_info", systemInfo);
            result.put("initialization_complete", initializationComplete.getCount() == 0);
            
            return result;
        } catch (Exception e) {
            if (ENABLE_LOGGING) {
                logger.warning("Error getting health check: " + e.getMessage());
            }
            return Map.of("status", "error", "error", e.getMessage());
        }
    }
    
    @Override
    public void close() {
        if (ENABLE_LOGGING) {
            logger.info("Shutting down SDK...");
        }
        
        // Stop background threads
        stopEvent.set();
        
        // Upload remaining logs
        if (enableMetrics && !offlineMode) {
            try {
                uploadLogs();
            } catch (Exception e) {
                if (ENABLE_LOGGING) {
                    logger.warning("Error during final log upload: " + e.getMessage());
                }
            }
        }
        
        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Close HTTP client
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
        
        if (ENABLE_LOGGING) {
            logger.info("SDK shutdown complete");
        }
    }
    
    // Helper classes and enums
    
    public static class FlagChange {
        public final String flagName;
        public final Object oldValue;
        public final Object newValue;
        
        public FlagChange(String flagName, Object oldValue, Object newValue) {
            this.flagName = flagName;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
    
    private static class EvaluationResult {
        public final Object value;
        public final EvaluationContext context;
        
        public EvaluationResult(Object value, EvaluationContext context) {
            this.value = value;
            this.context = context;
        }
    }
    
    private static class EvaluationContext {
        public boolean flagActive;
        public boolean flagFound;
        public boolean defaultValueUsed;
        public List<String> segmentsMatched;
        public List<String> segmentsEvaluated;
        public boolean rolloutQualified;
        public String reason;
        public long totalSdkTimeMs;
    }
    
    private static class LogEntry {
        public String userId;
        public String flagName;
        public Object flagValue;
        public String timestamp;
        public String sessionId;
        public long evaluationTimeMs;
        public EvaluationContext evaluationContext;
        public Map<String, Object> segments;
        public Map<String, Object> metadata;
    }
    
    private static class Stats {
        public int totalUserAccesses = 0;
        public Set<String> uniqueUsers = new HashSet<>();
        public Set<String> uniqueFlagsAccessed = new HashSet<>();
        public String lastSync;
        public String lastLogUpload;
        public ApiCalls apiCalls = new ApiCalls();
        public Map<String, Integer> errors = new HashMap<>();
        public int segmentMatches = 0;
        public int rolloutEvaluations = 0;
        public EvaluationTimes evaluationTimes = new EvaluationTimes();
        
        public static class ApiCalls {
            public int successful = 0;
            public int failed = 0;
            public int total = 0;
        }
        
        public static class EvaluationTimes {
            public long totalMs = 0;
            public int count = 0;
            public long minMs = Long.MAX_VALUE;
            public long maxMs = 0;
        }
    }
    
    private static class CircuitBreaker {
        public CircuitBreakerState state = CircuitBreakerState.CLOSED;
        public int failureCount = 0;
        public Long lastFailureTime;
        public final int failureThreshold = 5;
        public final long recoveryTimeout = 60000; // 60 seconds
    }
    
    private enum CircuitBreakerState {
        CLOSED, OPEN, HALF_OPEN
    }
    
    private static class RateLimit {
        public int count;
        public long lastTime;
        
        public RateLimit(int count, long lastTime) {
            this.count = count;
            this.lastTime = lastTime;
        }
    }
    
    // Utility methods for production deployment
    
    public static List<String> validateProductionConfig(Map<String, Object> config) {
        List<String> warnings = new ArrayList<>();
        
        String apiBaseUrl = (String) config.get("api_base_url");
        if (apiBaseUrl != null && apiBaseUrl.startsWith("http://")) {
            warnings.add("Using HTTP instead of HTTPS - security risk");
        }
        
        Integer timeout = (Integer) config.get("timeout");
        if (timeout != null && timeout < 5) {
            warnings.add("Timeout too low - may cause instability");
        }
        
        String clientSecret = (String) config.get("client_secret");
        if (clientSecret == null || clientSecret.isEmpty()) {
            warnings.add("Missing client secret");
        } else if (clientSecret.length() < 32) {
            warnings.add("Client secret appears to be weak");
        }
        
        return warnings;
    }
    
    public static FeatureFlagsHQSDK createProductionClient(String clientId, String clientSecret, String environment, Map<String, Object> options) {
        Map<String, Object> secureConfig = new HashMap<>();
        secureConfig.put("timeout", 30);
        secureConfig.put("max_retries", 3);
        secureConfig.put("offline_mode", false);
        secureConfig.put("enable_metrics", true);
        if (options != null) {
            secureConfig.putAll(options);
        }
        
        // Validate configuration
        List<String> warnings = validateProductionConfig(secureConfig);
        for (String warning : warnings) {
            if (ENABLE_LOGGING) {
                logger.warning("Configuration warning: " + warning);
            }
        }
        
        // Create SDK instance
        Builder builder = new Builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .environment(environment)
                .timeout((Integer) secureConfig.getOrDefault("timeout", 30))
                .maxRetries((Integer) secureConfig.getOrDefault("max_retries", 3))
                .offlineMode((Boolean) secureConfig.getOrDefault("offline_mode", false))
                .enableMetrics((Boolean) secureConfig.getOrDefault("enable_metrics", true));
        
        String apiBaseUrl = (String) secureConfig.get("api_base_url");
        if (apiBaseUrl != null) {
            builder.apiBaseUrl(apiBaseUrl);
        }
        
        FeatureFlagsHQSDK sdk = builder.build();
        
        if (ENABLE_LOGGING) {
            logger.info("Secure " + COMPANY_NAME + " SDK initialized with production configuration");
        }
        
        return sdk;
    }
}