package com.startin.api.dto.ai;

public class AiChatResponseDTO {

    private String reply;

    public AiChatResponseDTO() {
    }

    public AiChatResponseDTO(String reply) {
        this.reply = reply;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
