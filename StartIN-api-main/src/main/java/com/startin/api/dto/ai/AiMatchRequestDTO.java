package com.startin.api.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class AiMatchRequestDTO {

    private AiStartupInputDTO startup;
    private List<AiOpportunityInputDTO> opportunities = new ArrayList<>();
    private Boolean scrapeLiveWeb = false;

    public AiMatchRequestDTO() {
    }

    public AiMatchRequestDTO(AiStartupInputDTO startup, List<AiOpportunityInputDTO> opportunities, Boolean scrapeLiveWeb) {
        this.startup = startup;
        this.opportunities = opportunities != null ? opportunities : new ArrayList<>();
        this.scrapeLiveWeb = scrapeLiveWeb != null ? scrapeLiveWeb : false;
    }

    public AiStartupInputDTO getStartup() { return startup; }
    public void setStartup(AiStartupInputDTO startup) { this.startup = startup; }
    public List<AiOpportunityInputDTO> getOpportunities() { return opportunities; }
    public void setOpportunities(List<AiOpportunityInputDTO> opportunities) { this.opportunities = opportunities; }
    public Boolean getScrapeLiveWeb() { return scrapeLiveWeb; }
    public void setScrapeLiveWeb(Boolean scrapeLiveWeb) { this.scrapeLiveWeb = scrapeLiveWeb; }
}
