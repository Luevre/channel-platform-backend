package com.sinolink.channel.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AgentChatRequest {

    @NotEmpty(message = "消息列表不能为空")
    private List<Message> messages;

    private String sessionId;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
