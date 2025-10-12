package io.swaggeragent.extractor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParameterInfo {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("in")
    private String in; // path, query, body, header
    
    @JsonProperty("required")
    private boolean required;
    
    @JsonProperty("validationAnnotations")
    private String[] validationAnnotations;
    
    @JsonProperty("description")
    private String description;

    // Constructors
    public ParameterInfo() {}

    public ParameterInfo(String name, String type, String in, boolean required) {
        this.name = name;
        this.type = type;
        this.in = in;
        this.required = required;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIn() { return in; }
    public void setIn(String in) { this.in = in; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String[] getValidationAnnotations() { return validationAnnotations; }
    public void setValidationAnnotations(String[] validationAnnotations) { this.validationAnnotations = validationAnnotations; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}



