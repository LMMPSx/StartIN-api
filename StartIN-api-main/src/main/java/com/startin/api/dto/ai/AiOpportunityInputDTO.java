package com.startin.api.dto.ai;

public class AiOpportunityInputDTO {

    private String id;
    private String title;
    private String type;
    private String description;
    private String tags;
    private String url;
    private String deadline;
    private String meta;

    public AiOpportunityInputDTO() {
    }

    public AiOpportunityInputDTO(String id, String title, String type, String description, String tags, String url, String deadline, String meta) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.description = description;
        this.tags = tags;
        this.url = url;
        this.deadline = deadline;
        this.meta = meta;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
}
