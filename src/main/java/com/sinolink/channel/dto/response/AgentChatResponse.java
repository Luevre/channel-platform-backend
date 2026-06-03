package com.sinolink.channel.dto.response;

import lombok.Data;

@Data
public class AgentChatResponse {
    private String content;
    private String sessionId;
    /** 本次对话所用的 Skill 类型，前端用于显示 badge */
    private String skillType;
}
