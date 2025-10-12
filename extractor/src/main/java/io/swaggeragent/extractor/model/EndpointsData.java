package io.swaggeragent.extractor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EndpointsData {
    @JsonProperty("controllers")
    private List<ControllerInfo> controllers;
    
    @JsonProperty("extractedAt")
    private String extractedAt;
    
    @JsonProperty("totalMethods")
    private int totalMethods;

    // Constructors
    public EndpointsData() {}

    public EndpointsData(List<ControllerInfo> controllers) {
        this.controllers = controllers;
        this.extractedAt = java.time.Instant.now().toString();
        this.totalMethods = controllers.stream()
            .mapToInt(c -> c.getMethods() != null ? c.getMethods().size() : 0)
            .sum();
    }

    // Getters and Setters
    public List<ControllerInfo> getControllers() { return controllers; }
    public void setControllers(List<ControllerInfo> controllers) { this.controllers = controllers; }

    public String getExtractedAt() { return extractedAt; }
    public void setExtractedAt(String extractedAt) { this.extractedAt = extractedAt; }

    public int getTotalMethods() { return totalMethods; }
    public void setTotalMethods(int totalMethods) { this.totalMethods = totalMethods; }
}



