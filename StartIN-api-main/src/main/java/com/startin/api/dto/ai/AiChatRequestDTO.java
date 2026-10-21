package com.startin.api.dto.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiChatRequestDTO {

    private List<Map<String, String>> messages = new ArrayList<>();
    private AiStartupInputDTO startup;

    public AiChatRequestDTO() {
    }

    public List<Map<String, String>> getMessages() { return messages; }
    public void setMessages(List<Map<String, String>> messages) { this.messages = messages; }
    public AiStartupInputDTO getStartup() { return startup; }
    public void setStartup(AiStartupInputDTO startup) { this.startup = startup; }
}
