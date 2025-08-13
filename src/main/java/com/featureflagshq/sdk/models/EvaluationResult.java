package com.featureflagshq.sdk.models;

import java.util.List;

public class EvaluationResult {
    private Object value;
    private boolean flagFound;
    private boolean flagActive;
    private boolean defaultValueUsed;
    private List<String> segmentsMatched;
    private List<String> segmentsEvaluated;
    private boolean rolloutQualified;
    private String reason;
    private long evaluationTimeMs;
    
    public EvaluationResult() {}
    
    public EvaluationResult(Object value, boolean flagFound, boolean defaultValueUsed, String reason) {
        this.value = value;
        this.flagFound = flagFound;
        this.defaultValueUsed = defaultValueUsed;
        this.reason = reason;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public boolean isFlagFound() {
        return flagFound;
    }
    
    public void setFlagFound(boolean flagFound) {
        this.flagFound = flagFound;
    }
    
    public boolean isFlagActive() {
        return flagActive;
    }
    
    public void setFlagActive(boolean flagActive) {
        this.flagActive = flagActive;
    }
    
    public boolean isDefaultValueUsed() {
        return defaultValueUsed;
    }
    
    public void setDefaultValueUsed(boolean defaultValueUsed) {
        this.defaultValueUsed = defaultValueUsed;
    }
    
    public List<String> getSegmentsMatched() {
        return segmentsMatched;
    }
    
    public void setSegmentsMatched(List<String> segmentsMatched) {
        this.segmentsMatched = segmentsMatched;
    }
    
    public List<String> getSegmentsEvaluated() {
        return segmentsEvaluated;
    }
    
    public void setSegmentsEvaluated(List<String> segmentsEvaluated) {
        this.segmentsEvaluated = segmentsEvaluated;
    }
    
    public boolean isRolloutQualified() {
        return rolloutQualified;
    }
    
    public void setRolloutQualified(boolean rolloutQualified) {
        this.rolloutQualified = rolloutQualified;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public long getEvaluationTimeMs() {
        return evaluationTimeMs;
    }
    
    public void setEvaluationTimeMs(long evaluationTimeMs) {
        this.evaluationTimeMs = evaluationTimeMs;
    }
    
    @Override
    public String toString() {
        return "EvaluationResult{" +
                "value=" + value +
                ", flagFound=" + flagFound +
                ", flagActive=" + flagActive +
                ", defaultValueUsed=" + defaultValueUsed +
                ", reason='" + reason + '\'' +
                ", evaluationTimeMs=" + evaluationTimeMs +
                '}';
    }
}