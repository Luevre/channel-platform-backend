package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sinolink.channel.dto.request.AgentChatRequest;
import com.sinolink.channel.dto.response.AgentChatResponse;
import com.sinolink.channel.entity.MyParameter;
import com.sinolink.channel.entity.PlacementParam;
import com.sinolink.channel.mapper.MyParameterMapper;
import com.sinolink.channel.mapper.PlacementParamMapper;
import com.sinolink.channel.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PlacementParamMapper placementParamMapper;
    private final MyParameterMapper myParameterMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    private static final int MAX_TOOL_ROUNDS = 5;

    private static final String SYSTEM_PROMPT = """
            你是"投助"，国金证券渠道投放中台的 AI 助手。

            你可以帮用户完成以下操作：
            1. 查询现有投放参数（按关键词、分类搜索）
            2. 查看某个参数的详细信息和审核状态
            3. 发起新的参数申请（会进入审核流程）

            使用规则：
            - 用户想搜索参数时，调用 query_params
            - 用户想看某个参数详情时，调用 get_param_detail
            - 用户想申请新参数时，先确认必要信息（中文名、英文名、分类、说明），然后调用 create_param_application
            - 分类只能是：场景参数、人员参数、活动参数、产品参数
            - 回复使用简洁专业的中文，不要使用任何 Markdown 格式（不要用 **加粗**、# 标题、- 列表前缀、```代码块等），直接用纯文本
            - 申请参数前务必和用户确认信息，不要自己编造参数名""";

    // ========== Tool JSON Schema 定义 ==========

    private static final String TOOLS_JSON = """
            [
              {
                "name": "query_params",
                "description": "查询现有投放参数列表，支持按关键词和分类筛选",
                "input_schema": {
                  "type": "object",
                  "properties": {
                    "keyword": {
                      "type": "string",
                      "description": "搜索关键词，匹配参数中文名或英文名，可选"
                    },
                    "category": {
                      "type": "string",
                      "description": "参数分类筛选，可选值：场景参数、人员参数、活动参数、产品参数",
                      "enum": ["场景参数", "人员参数", "活动参数", "产品参数"]
                    }
                  },
                  "required": []
                }
              },
              {
                "name": "get_param_detail",
                "description": "查询单个投放参数的完整详情，包括审核状态",
                "input_schema": {
                  "type": "object",
                  "properties": {
                    "param_id": {
                      "type": "integer",
                      "description": "参数ID，与 param_key 二选一"
                    },
                    "param_key": {
                      "type": "string",
                      "description": "参数英文名，与 param_id 二选一"
                    }
                  },
                  "required": []
                }
              },
              {
                "name": "create_param_application",
                "description": "发起新的投放参数申请，会写入审核流程",
                "input_schema": {
                  "type": "object",
                  "properties": {
                    "param_name_cn": {
                      "type": "string",
                      "description": "参数中文名称"
                    },
                    "param_name_en": {
                      "type": "string",
                      "description": "参数英文名称，仅支持英文、下划线、数字"
                    },
                    "category": {
                      "type": "string",
                      "description": "参数分类",
                      "enum": ["场景参数", "人员参数", "活动参数", "产品参数"]
                    },
                    "description": {
                      "type": "string",
                      "description": "参数应用说明"
                    },
                    "applicant": {
                      "type": "string",
                      "description": "申请人姓名"
                    }
                  },
                  "required": ["param_name_cn", "param_name_en", "category", "description", "applicant"]
                }
              }
            ]""";

    public AgentServiceImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            PlacementParamMapper placementParamMapper,
            MyParameterMapper myParameterMapper,
            @Value("${anthropic.api-key}") String apiKey,
            @Value("${anthropic.model:claude-sonnet-4-20250514}") String model,
            @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.placementParamMapper = placementParamMapper;
        this.myParameterMapper = myParameterMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        try {
            // 构建消息列表（可变，后续会追加 tool_use / tool_result）
            List<Object> messages = new ArrayList<>();
            for (AgentChatRequest.Message msg : request.getMessages()) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }

            JsonNode tools = objectMapper.readTree(TOOLS_JSON);
            String finalText = runAgentLoop(messages, tools);

            AgentChatResponse chatResponse = new AgentChatResponse();
            chatResponse.setContent(finalText);
            chatResponse.setSessionId(sessionId);
            return chatResponse;

        } catch (Exception e) {
            log.error("AI 对话失败", e);
            AgentChatResponse fallback = new AgentChatResponse();
            fallback.setContent("抱歉，AI 助手暂时无法响应，请稍后重试。");
            fallback.setSessionId(sessionId);
            return fallback;
        }
    }

    // ========== Agent 循环：调用 Claude → 处理 tool_use → 回传 tool_result → 再调 Claude ==========

    private String runAgentLoop(List<Object> messages, JsonNode tools) throws Exception {
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            JsonNode response = callClaude(messages, tools);

            String stopReason = response.path("stop_reason").asText();
            JsonNode contentArray = response.path("content");

            if ("end_turn".equals(stopReason)) {
                return extractTextFromContent(contentArray);
            }

            if ("tool_use".equals(stopReason)) {
                // 1. 把 assistant 的完整 content（含 text + tool_use）追加到 messages
                messages.add(Map.of("role", "assistant", "content", contentArray));

                // 2. 遍历 content，找到所有 tool_use，执行并收集 tool_result
                List<Object> toolResults = new ArrayList<>();
                for (JsonNode block : contentArray) {
                    if ("tool_use".equals(block.path("type").asText())) {
                        String toolUseId = block.path("id").asText();
                        String toolName = block.path("name").asText();
                        JsonNode input = block.path("input");

                        log.info("执行工具: {} 参数: {}", toolName, input);
                        String result = executeTool(toolName, input);
                        log.info("工具结果: {}", result.length() > 200 ? result.substring(0, 200) + "..." : result);

                        toolResults.add(Map.of(
                                "type", "tool_result",
                                "tool_use_id", toolUseId,
                                "content", result
                        ));
                    }
                }

                // 3. 把 tool_result 作为 user 消息追加
                messages.add(Map.of("role", "user", "content", toolResults));
            } else {
                return extractTextFromContent(contentArray);
            }
        }

        return "处理轮次过多，请简化您的问题后重试。";
    }

    private JsonNode callClaude(List<Object> messages, JsonNode tools) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 2048);
        body.put("system", SYSTEM_PROMPT);
        body.put("tools", tools);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        String requestBody = objectMapper.writeValueAsString(body);
        log.debug("Claude 请求: {}", requestBody.length() > 500 ? requestBody.substring(0, 500) + "..." : requestBody);

        HttpEntity<String> httpEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/v1/messages",
                HttpMethod.POST,
                httpEntity,
                String.class);

        return objectMapper.readTree(response.getBody());
    }

    private String extractTextFromContent(JsonNode contentArray) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : contentArray) {
            if ("text".equals(block.path("type").asText())) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(block.path("text").asText());
            }
        }
        return sb.isEmpty() ? "我暂时没有更多信息可提供。" : sb.toString();
    }

    // ========== Tool 执行分发 ==========

    private String executeTool(String toolName, JsonNode input) {
        try {
            return switch (toolName) {
                case "query_params" -> toolQueryParams(input);
                case "get_param_detail" -> toolGetParamDetail(input);
                case "create_param_application" -> toolCreateParamApplication(input);
                default -> "{\"error\": \"未知工具: " + toolName + "\"}";
            };
        } catch (Exception e) {
            log.error("工具执行异常: {}", toolName, e);
            return "{\"error\": \"工具执行失败: " + e.getMessage() + "\"}";
        }
    }

    // ========== Tool 1: query_params ==========

    private String toolQueryParams(JsonNode input) throws Exception {
        String keyword = input.has("keyword") ? input.get("keyword").asText("") : "";
        String category = input.has("category") ? input.get("category").asText("") : "";

        LambdaQueryWrapper<PlacementParam> wrapper = new LambdaQueryWrapper<PlacementParam>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(PlacementParam::getParamName, keyword)
                        .or()
                        .like(PlacementParam::getParamKey, keyword))
                .eq(StringUtils.hasText(category), PlacementParam::getCategory, category)
                .orderByDesc(PlacementParam::getUpdatedAt);

        List<PlacementParam> list = placementParamMapper.selectList(wrapper);

        ArrayNode result = objectMapper.createArrayNode();
        for (PlacementParam p : list) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", p.getId());
            node.put("param_name_cn", p.getParamName());
            node.put("param_name_en", p.getParamKey());
            node.put("category", p.getCategory());
            node.put("param_usage", p.getParamUsage());
            node.put("manager", p.getManager());
            result.add(node);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("total", list.size());
        response.set("params", result);
        return objectMapper.writeValueAsString(response);
    }

    // ========== Tool 2: get_param_detail ==========

    private String toolGetParamDetail(JsonNode input) throws Exception {
        PlacementParam param = null;

        if (input.has("param_id") && !input.get("param_id").isNull()) {
            param = placementParamMapper.selectById(input.get("param_id").asLong());
        } else if (input.has("param_key") && StringUtils.hasText(input.get("param_key").asText())) {
            param = placementParamMapper.selectOne(
                    new LambdaQueryWrapper<PlacementParam>()
                            .eq(PlacementParam::getParamKey, input.get("param_key").asText()));
        }

        if (param == null) {
            return "{\"error\": \"未找到该参数\"}";
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("id", param.getId());
        result.put("param_name_cn", param.getParamName());
        result.put("param_name_en", param.getParamKey());
        result.put("param_value_example", param.getParamValue());
        result.put("category", param.getCategory());
        result.put("param_usage", param.getParamUsage());
        result.put("manager", param.getManager());
        result.put("status", param.getStatus() == 1 ? "启用" : "禁用");

        String auditLabel = switch (param.getAuditStatus() != null ? param.getAuditStatus() : -1) {
            case 0 -> "待审核";
            case 1 -> "已审核";
            case 2 -> "已驳回";
            default -> "无审核记录";
        };
        result.put("audit_status", auditLabel);
        result.put("auditor", param.getAuditor());
        if (param.getUpdatedAt() != null) {
            result.put("updated_at", param.getUpdatedAt().toString());
        }

        return objectMapper.writeValueAsString(result);
    }

    // ========== Tool 3: create_param_application ==========

    private String toolCreateParamApplication(JsonNode input) throws Exception {
        String paramNameCn = input.get("param_name_cn").asText();
        String paramNameEn = input.get("param_name_en").asText();
        String category = input.get("category").asText();
        String description = input.get("description").asText();
        String applicant = input.get("applicant").asText();

        // 检查英文名是否已存在
        Long existing = placementParamMapper.selectCount(
                new LambdaQueryWrapper<PlacementParam>()
                        .eq(PlacementParam::getParamKey, paramNameEn));
        if (existing > 0) {
            return "{\"error\": \"参数英文名 " + paramNameEn + " 已存在，请更换\"}";
        }

        // 写入 my_parameter 表，走审核流程
        MyParameter myParam = new MyParameter();
        myParam.setParameterName(paramNameCn + " | " + paramNameEn);
        myParam.setScene(category);
        myParam.setValue(paramNameEn);
        myParam.setUsage(description);
        myParam.setApplicant(applicant);
        myParam.setStatus(1);  // 直接提交（已提交状态）
        myParam.setAuditResult(0);  // 待审核
        myParam.setSubmittedAt(LocalDateTime.now());
        myParameterMapper.insert(myParam);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("application_id", myParam.getId());
        result.put("param_name_cn", paramNameCn);
        result.put("param_name_en", paramNameEn);
        result.put("category", category);
        result.put("applicant", applicant);
        result.put("status", "已提交，待审核");
        result.put("message", "申请单已创建，编号 " + myParam.getId() + "，等待管理员审核");
        return objectMapper.writeValueAsString(result);
    }
}
