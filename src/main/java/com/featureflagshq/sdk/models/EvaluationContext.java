package com.featureflagshq.sdk.models;

import java.util.List;
import java.util.Map;

public class EvaluationContext {
    private String userId;
    private String flagName;
    private Map<String, Object> segments;
    private String sessionId;
    private String environment;
    private long timestamp;
    private Map<String, Object> metadata;
    
    public EvaluationContext() {}
    
    public EvaluationContext(String userId, String flagName, Map<String, Object> segments) {
        this.userId = userId;
        this.flagName = flagName;
        this.segments = segments;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getFlagName() {
        return flagName;
    }
    
    public void setFlagName(String flagName) {
        this.flagName = flagName;
    }
    
    public Map<String, Object> getSegments() {
        return segments;
    }
    
    public void setSegments(Map<String, Object> segments) {
        this.segments = segments;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "EvaluationContext{" +
                "userId='" + userId + '\'' +
                ", flagName='" + flagName + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", environment='" + environment + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}