package com.startin.api.dto.ai;

import java.util.List;

public class AiStartupInputDTO {

    private Long id;
    private String name;
    private String sector;
    private String mainArea;
    private String stage;
    private String currentStage;
    private String description;
    private List<String> tags;
    private String problem;
    private String howItWorks;
    private String differentiator;
    private List<String> technologies;
    private String businessModel;
    private String targetAudience;
    private String location;

    public AiStartupInputDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public String getMainArea() { return mainArea; }
    public void setMainArea(String mainArea) { this.mainArea = mainArea; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }
    public String getHowItWorks() { return howItWorks; }
    public void setHowItWorks(String howItWorks) { this.howItWorks = howItWorks; }
    public String getDifferentiator() { return differentiator; }
    public void setDifferentiator(String differentiator) { this.differentiator = differentiator; }
    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }
    public String getBusinessModel() { return businessModel; }
    public void setBusinessModel(String businessModel) { this.businessModel = businessModel; }
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
