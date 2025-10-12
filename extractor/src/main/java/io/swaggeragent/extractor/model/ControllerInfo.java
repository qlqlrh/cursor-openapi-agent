package io.swaggeragent.extractor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ControllerInfo {
    @JsonProperty("className")
    private String className;
    
    @JsonProperty("packageName")
    private String packageName;
    
    @JsonProperty("requestMapping")
    private String requestMapping;
    
    @JsonProperty("methods")
    private List<MethodInfo> methods;
    
    @JsonProperty("existingAnnotations")
    private Map<String, Object> existingAnnotations;

    // Constructors
    public ControllerInfo() {}

    public ControllerInfo(String className, String packageName, String requestMapping) {
        this.className = className;
        this.packageName = packageName;
        this.requestMapping = requestMapping;
    }

    // Getters and Setters
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getRequestMapping() { return requestMapping; }
    public void setRequestMapping(String requestMapping) { this.requestMapping = requestMapping; }

    public List<MethodInfo> getMethods() { return methods; }
    public void setMethods(List<MethodInfo> methods) { this.methods = methods; }

    public Map<String, Object> getExistingAnnotations() { return existingAnnotations; }
    public void setExistingAnnotations(Map<String, Object> existingAnnotations) { this.existingAnnotations = existingAnnotations; }
}



