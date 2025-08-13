package com.featureflagshq.sdk.models;

import java.util.Map;

public class Segment {
    private String name;
    private String type;
    private String comparator;
    private Object value;
    private boolean isActive;
    private Map<String, Object> metadata;
    
    public Segment() {}
    
    public Segment(String name, String type, String comparator, Object value) {
        this.name = name;
        this.type = type;
        this.comparator = comparator;
        this.value = value;
        this.isActive = true;
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
    
    public String getComparator() {
        return comparator;
    }
    
    public void setComparator(String comparator) {
        this.comparator = comparator;
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
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "Segment{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", comparator='" + comparator + '\'' +
                ", value=" + value +
                ", isActive=" + isActive +
                '}';
    }
}