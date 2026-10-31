package com.startin.api.dto.match;

public class MatchTriggerRequestDTO {

    private Long startupId;
    private Boolean scrapeLiveWeb = false;

    public MatchTriggerRequestDTO() {
    }

    public MatchTriggerRequestDTO(Long startupId, Boolean scrapeLiveWeb) {
        this.startupId = startupId;
        this.scrapeLiveWeb = scrapeLiveWeb != null ? scrapeLiveWeb : false;
    }

    public Long getStartupId() {
        return startupId;
    }

    public void setStartupId(Long startupId) {
        this.startupId = startupId;
    }

    public Boolean getScrapeLiveWeb() {
        return scrapeLiveWeb;
    }

    public void setScrapeLiveWeb(Boolean scrapeLiveWeb) {
        this.scrapeLiveWeb = scrapeLiveWeb;
    }
}
