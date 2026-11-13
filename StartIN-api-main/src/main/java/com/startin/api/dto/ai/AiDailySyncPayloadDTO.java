package com.startin.api.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiDailySyncPayloadDTO {

    private String executedAt;
    private String scheduledTime;
    private List<ScrapedOpportunityDTO> scrapedOpportunities = new ArrayList<>();
    private List<StartupMatchesBatchDTO> startupMatches = new ArrayList<>();

    public AiDailySyncPayloadDTO() {
    }

    public String getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(String executedAt) {
        this.executedAt = executedAt;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public List<ScrapedOpportunityDTO> getScrapedOpportunities() {
        return scrapedOpportunities;
    }

    public void setScrapedOpportunities(List<ScrapedOpportunityDTO> scrapedOpportunities) {
        this.scrapedOpportunities = scrapedOpportunities;
    }

    public List<StartupMatchesBatchDTO> getStartupMatches() {
        return startupMatches;
    }

    public void setStartupMatches(List<StartupMatchesBatchDTO> startupMatches) {
        this.startupMatches = startupMatches;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScrapedOpportunityDTO {
        private String id;
        private String code;
        private String title;
        private String type;
        private String description;
        private String shortDescription;
        private String tags;
        private String url;
        private String externalUrl;
        private String deadline;
        private String meta;
        private String metaInfo;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getShortDescription() {
            return shortDescription;
        }

        public void setShortDescription(String shortDescription) {
            this.shortDescription = shortDescription;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getExternalUrl() {
            return externalUrl;
        }

        public void setExternalUrl(String externalUrl) {
            this.externalUrl = externalUrl;
        }

        public String getDeadline() {
            return deadline;
        }

        public void setDeadline(String deadline) {
            this.deadline = deadline;
        }

        public String getMeta() {
            return meta;
        }

        public void setMeta(String meta) {
            this.meta = meta;
        }

        public String getMetaInfo() {
            return metaInfo;
        }

        public void setMetaInfo(String metaInfo) {
            this.metaInfo = metaInfo;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StartupMatchesBatchDTO {
        private Long startupId;
        private String startupName;
        private Integer totalAnalyzed;
        private List<AiMatchItemDTO> matches = new ArrayList<>();

        public Long getStartupId() {
            return startupId;
        }

        public void setStartupId(Long startupId) {
            this.startupId = startupId;
        }

        public String getStartupName() {
            return startupName;
        }

        public void setStartupName(String startupName) {
            this.startupName = startupName;
        }

        public Integer getTotalAnalyzed() {
            return totalAnalyzed;
        }

        public void setTotalAnalyzed(Integer totalAnalyzed) {
            this.totalAnalyzed = totalAnalyzed;
        }

        public List<AiMatchItemDTO> getMatches() {
            return matches;
        }

        public void setMatches(List<AiMatchItemDTO> matches) {
            this.matches = matches;
        }
    }
}
