package com.startin.api.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class AiMatchResponseDTO {

    private String startupName;
    private int totalAnalyzed;
    private List<AiMatchItemDTO> matches = new ArrayList<>();

    public AiMatchResponseDTO() {
    }

    public String getStartupName() { return startupName; }
    public void setStartupName(String startupName) { this.startupName = startupName; }
    public int getTotalAnalyzed() { return totalAnalyzed; }
    public void setTotalAnalyzed(int totalAnalyzed) { this.totalAnalyzed = totalAnalyzed; }
    public List<AiMatchItemDTO> getMatches() { return matches; }
    public void setMatches(List<AiMatchItemDTO> matches) { this.matches = matches; }
}
