package com.sinolink.channel.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AgentChatRequest {

    @NotEmpty(message = "消息列表不能为空")
    private List<Message> messages;

    private String sessionId;

    /** 技能类型：param_apply / delivery_config / strategy_config / general（null 则由 Dispatcher 自动识别） */
    private String skillType;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
