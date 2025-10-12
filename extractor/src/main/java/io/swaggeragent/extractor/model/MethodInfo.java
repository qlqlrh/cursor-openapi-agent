package io.swaggeragent.extractor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class MethodInfo {
    @JsonProperty("methodName")
    private String methodName;
    
    @JsonProperty("httpMethod")
    private String httpMethod;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("parameters")
    private List<ParameterInfo> parameters;
    
    @JsonProperty("returnType")
    private String returnType;
    
    @JsonProperty("exceptions")
    private List<String> exceptions;
    
    @JsonProperty("existingAnnotations")
    private Map<String, Object> existingAnnotations;
    
    @JsonProperty("lineNumber")
    private int lineNumber;

    // Constructors
    public MethodInfo() {}

    public MethodInfo(String methodName, String httpMethod, String path) {
        this.methodName = methodName;
        this.httpMethod = httpMethod;
        this.path = path;
    }

    // Getters and Setters
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public List<ParameterInfo> getParameters() { return parameters; }
    public void setParameters(List<ParameterInfo> parameters) { this.parameters = parameters; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }

    public List<String> getExceptions() { return exceptions; }
    public void setExceptions(List<String> exceptions) { this.exceptions = exceptions; }

    public Map<String, Object> getExistingAnnotations() { return existingAnnotations; }
    public void setExistingAnnotations(Map<String, Object> existingAnnotations) { this.existingAnnotations = existingAnnotations; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
}



