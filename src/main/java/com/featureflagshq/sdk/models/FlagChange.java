package com.featureflagshq.sdk.models;

public class FlagChange {
    private String flagName;
    private Object oldValue;
    private Object newValue;
    private long timestamp;
    
    public FlagChange() {}
    
    public FlagChange(String flagName, Object oldValue, Object newValue) {
        this.flagName = flagName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getFlagName() {
        return flagName;
    }
    
    public void setFlagName(String flagName) {
        this.flagName = flagName;
    }
    
    public Object getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }
    
    public Object getNewValue() {
        return newValue;
    }
    
    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "FlagChange{" +
                "flagName='" + flagName + '\'' +
                ", oldValue=" + oldValue +
                ", newValue=" + newValue +
                ", timestamp=" + timestamp +
                '}';
    }
}