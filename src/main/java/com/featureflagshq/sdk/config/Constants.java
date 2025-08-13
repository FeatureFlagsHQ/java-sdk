package com.featureflagshq.sdk.config;

public final class Constants {
    
    public static final String SDK_VERSION = "1.0.0";
    public static final String DEFAULT_API_BASE_URL = "https://api.featureflagshq.com";
    public static final String COMPANY_NAME = "FeatureFlagsHQ";
    public static final String USER_AGENT_PREFIX = COMPANY_NAME + "-Java-SDK";
    
    public static final int MAX_USER_ID_LENGTH = 255;
    public static final int MAX_FLAG_NAME_LENGTH = 255;
    public static final int MAX_SEGMENT_KEY_LENGTH = 128;
    
    public static final long DEFAULT_POLLING_INTERVAL_MS = 300000; // 5 minutes
    public static final long DEFAULT_LOG_UPLOAD_INTERVAL_MS = 120000; // 2 minutes
    
    public static final int MAX_UNIQUE_USERS_TRACKED = 10000;
    public static final int MAX_UNIQUE_FLAGS_TRACKED = 1000;
    
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_MAX_RETRIES = 3;
    
    public static final int RATE_LIMIT_MAX_REQUESTS_PER_MINUTE = 1000;
    public static final long RATE_LIMIT_WINDOW_MS = 60000; // 1 minute
    
    public static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5;
    public static final long CIRCUIT_BREAKER_RECOVERY_TIMEOUT_MS = 60000; // 60 seconds
    
    public static final int MAX_LOGS_BATCH_SIZE = 100;
    public static final int MAX_LOGS_RETRY_SIZE = 10;
    
    public static final String[] DANGEROUS_CHARS = {"\n", "\r", "\0", "\t", "\u001b"};
    public static final String[] SQL_INJECTION_PATTERNS = {"--", ";", "/*", "*/", "union", "select", "insert", "delete", "update", "drop"};
    
    public static final String REGEX_USER_ID_PATTERN = "^[a-zA-Z0-9_@\\.\\-\\+]+$";
    public static final String REGEX_FLAG_NAME_PATTERN = "^[a-zA-Z0-9_\\-]+$";
    
    public static final String ENV_CLIENT_ID = "FEATUREFLAGSHQ_CLIENT_ID";
    public static final String ENV_CLIENT_KEY = "FEATUREFLAGSHQ_CLIENT_KEY"; // Alternative
    public static final String ENV_CLIENT_SECRET = "FEATUREFLAGSHQ_CLIENT_SECRET";
    public static final String ENV_ENVIRONMENT = "FEATUREFLAGSHQ_ENVIRONMENT";
    public static final String ENV_API_BASE_URL = "FEATUREFLAGSHQ_API_BASE_URL";
    
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_SDK_PROVIDER = "X-SDK-Provider";
    public static final String HEADER_CLIENT_ID = "X-Client-ID";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String HEADER_SESSION_ID = "X-Session-ID";
    public static final String HEADER_SDK_VERSION = "X-SDK-Version";
    public static final String HEADER_ENVIRONMENT = "X-Environment";
    public static final String HEADER_USER_AGENT = "User-Agent";
    
    public static final String CONTENT_TYPE_JSON = "application/json";
    
    public static final String API_ENDPOINT_FLAGS = "/v1/flags/";
    public static final String API_ENDPOINT_LOGS_BATCH = "/v1/logs/batch/";
    
    public static final String DEFAULT_VALUE_TYPE_BOOLEAN = "bool";
    public static final String DEFAULT_VALUE_TYPE_INTEGER = "int";
    public static final String DEFAULT_VALUE_TYPE_FLOAT = "float";
    public static final String DEFAULT_VALUE_TYPE_STRING = "string";
    public static final String DEFAULT_VALUE_TYPE_JSON = "json";
    
    public static final String HASH_ALGORITHM = "HmacSHA256";
    public static final String CHARSET_UTF8 = "UTF-8";
    
    public static final boolean DEFAULT_ENABLE_LOGGING = false;
    
    private Constants() {
        // Utility class - prevent instantiation
    }
}