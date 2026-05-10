package com.sinolink.channel.controller;

import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.dto.request.AgentChatRequest;
import com.sinolink.channel.dto.response.AgentChatResponse;
import com.sinolink.channel.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "AI 助手", description = "投助 AI 对话接口")
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @Operation(summary = "AI 对话", description = "发送消息到投助 AI 助手，返回回复内容")
    @PostMapping("/chat")
    public Result<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        log.info("收到 AI 对话请求, sessionId={}", request.getSessionId());
        return Result.success(agentService.chat(request));
    }

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("投助 AI 助手服务正常");
    }
}
