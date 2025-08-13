package com.featureflagshq.sdk.models;

import java.util.List;
import java.util.Map;

public class Flag {
    private String name;
    private String type;
    private Object value;
    private boolean isActive;
    private String description;
    private Map<String, Object> rollout;
    private List<Segment> segments;
    private Map<String, Object> metadata;
    
    public Flag() {}
    
    public Flag(String name, String type, Object value, boolean isActive) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isActive = isActive;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, Object> getRollout() {
        return rollout;
    }
    
    public void setRollout(Map<String, Object> rollout) {
        this.rollout = rollout;
    }
    
    public List<Segment> getSegments() {
        return segments;
    }
    
    public void setSegments(List<Segment> segments) {
        this.segments = segments;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "Flag{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value=" + value +
                ", isActive=" + isActive +
                ", description='" + description + '\'' +
                '}';
    }
}