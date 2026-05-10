package com.sinolink.channel.service;

import com.sinolink.channel.dto.request.AgentChatRequest;
import com.sinolink.channel.dto.response.AgentChatResponse;

public interface AgentService {
    AgentChatResponse chat(AgentChatRequest request);
}
