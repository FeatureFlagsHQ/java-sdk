package com.featureflagshq.sdk.config;

import java.util.Map;
import java.util.function.Consumer;
import com.featureflagshq.sdk.models.FlagChange;

public class SDKConfig {
    private String clientId;
    private String clientSecret;
    private String apiBaseUrl;
    private String environment;
    private int timeout;
    private int maxRetries;
    private boolean offlineMode;
    private boolean enableMetrics;
    private long pollingIntervalMs;
    private long logUploadIntervalMs;
    private Consumer<FlagChange> onFlagChange;
    private Map<String, Object> customHeaders;
    
    public SDKConfig() {
        this.apiBaseUrl = Constants.DEFAULT_API_BASE_URL;
        this.environment = "production";
        this.timeout = 30;
        this.maxRetries = 3;
        this.offlineMode = false;
        this.enableMetrics = true;
        this.pollingIntervalMs = Constants.DEFAULT_POLLING_INTERVAL_MS;
        this.logUploadIntervalMs = Constants.DEFAULT_LOG_UPLOAD_INTERVAL_MS;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
    
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public boolean isOfflineMode() {
        return offlineMode;
    }
    
    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }
    
    public boolean isEnableMetrics() {
        return enableMetrics;
    }
    
    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }
    
    public long getPollingIntervalMs() {
        return pollingIntervalMs;
    }
    
    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }
    
    public long getLogUploadIntervalMs() {
        return logUploadIntervalMs;
    }
    
    public void setLogUploadIntervalMs(long logUploadIntervalMs) {
        this.logUploadIntervalMs = logUploadIntervalMs;
    }
    
    public Consumer<FlagChange> getOnFlagChange() {
        return onFlagChange;
    }
    
    public void setOnFlagChange(Consumer<FlagChange> onFlagChange) {
        this.onFlagChange = onFlagChange;
    }
    
    public Map<String, Object> getCustomHeaders() {
        return customHeaders;
    }
    
    public void setCustomHeaders(Map<String, Object> customHeaders) {
        this.customHeaders = customHeaders;
    }
    
    @Override
    public String toString() {
        return "SDKConfig{" +
                "clientId='" + (clientId != null ? clientId.substring(0, Math.min(8, clientId.length())) + "..." : "null") + '\'' +
                ", apiBaseUrl='" + apiBaseUrl + '\'' +
                ", environment='" + environment + '\'' +
                ", timeout=" + timeout +
                ", maxRetries=" + maxRetries +
                ", offlineMode=" + offlineMode +
                ", enableMetrics=" + enableMetrics +
                '}';
    }
}