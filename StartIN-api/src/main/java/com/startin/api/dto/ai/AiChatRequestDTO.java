package com.startin.api.dto.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiChatRequestDTO {

    private List<Map<String, String>> messages = new ArrayList<>();
    private Long startupId;
    private AiStartupInputDTO startup;
    private Map<String, Object> selectedOpportunity;
    private String attachmentName;
    private String attachmentBase64;
    private String attachmentText;

    public AiChatRequestDTO() {
    }

    public List<Map<String, String>> getMessages() { return messages; }
    public void setMessages(List<Map<String, String>> messages) { this.messages = messages; }
    public Long getStartupId() { return startupId; }
    public void setStartupId(Long startupId) { this.startupId = startupId; }
    public AiStartupInputDTO getStartup() { return startup; }
    public void setStartup(AiStartupInputDTO startup) { this.startup = startup; }
    public Map<String, Object> getSelectedOpportunity() { return selectedOpportunity; }
    public void setSelectedOpportunity(Map<String, Object> selectedOpportunity) { this.selectedOpportunity = selectedOpportunity; }
    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
    public String getAttachmentBase64() { return attachmentBase64; }
    public void setAttachmentBase64(String attachmentBase64) { this.attachmentBase64 = attachmentBase64; }
    public String getAttachmentText() { return attachmentText; }
    public void setAttachmentText(String attachmentText) { this.attachmentText = attachmentText; }
}
