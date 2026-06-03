package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sinolink.channel.dto.request.AgentChatRequest;
import com.sinolink.channel.dto.response.AgentChatResponse;
import com.sinolink.channel.entity.ConvNode;
import com.sinolink.channel.entity.ConvStrategy;
import com.sinolink.channel.entity.ConvStrategyNode;
import com.sinolink.channel.entity.DeliveryLink;
import com.sinolink.channel.entity.MasterLink;
import com.sinolink.channel.entity.MyParameter;
import com.sinolink.channel.entity.PlacementParam;
import com.sinolink.channel.mapper.ConvNodeMapper;
import com.sinolink.channel.mapper.ConvStrategyMapper;
import com.sinolink.channel.mapper.ConvStrategyNodeMapper;
import com.sinolink.channel.mapper.DeliveryLinkMapper;
import com.sinolink.channel.mapper.MasterLinkMapper;
import com.sinolink.channel.mapper.MyParameterMapper;
import com.sinolink.channel.mapper.PlacementParamMapper;
import com.sinolink.channel.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PlacementParamMapper placementParamMapper;
    private final MyParameterMapper myParameterMapper;
    private final ConvNodeMapper convNodeMapper;
    private final ConvStrategyMapper convStrategyMapper;
    private final ConvStrategyNodeMapper convStrategyNodeMapper;
    private final DeliveryLinkMapper deliveryLinkMapper;
    private final MasterLinkMapper masterLinkMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    private static final int MAX_TOOL_ROUNDS = 8;

    // ========== Skill 常量 ==========

    private static final String SKILL_PARAM_APPLY    = "param_apply";
    private static final String SKILL_DELIVERY       = "delivery_config";
    private static final String SKILL_STRATEGY       = "strategy_config";
    private static final String SKILL_SEARCH         = "search";
    private static final String SKILL_GENERAL        = "general";

    private static final String DISPATCHER_PROMPT = """
            你是意图分类器。根据用户最新一条消息判断意图，只输出以下值之一，不附带任何解释或标点符号：
            param_apply
            delivery_config
            strategy_config
            search
            general

            分类规则（优先级从高到低）：
            search          —— 消息中含有以下任意词：搜索、查找、查询、找一下、有没有、帮我找、查一查、查看、找找
                               【重要】只要用户有"查询/查找/搜索"动作，无论查什么（参数/节点/策略/链接/渠道），一律选 search
            param_apply     —— 消息中含有"申请参数"/"配置参数"/"新增参数"/"我需要一个参数"/"帮我配参"/"注册参数"
                               【注意】必须是创建新内容，纯查询意图不选此项
            delivery_config —— 消息中含有"配置链接"/"生成投放链接"/"投放链接"/"配置投放"
            strategy_config —— 消息中含有"回传策略"/"回传节点"/"配置策略"/"设置回传"
            general         —— 以上均不匹配，或消息只是单个名词（如渠道名、平台名）

            判断示例：
            "查询快手的投放参数"     → search（含"查询"）
            "搜索一下京东金融的策略" → search（含"搜索"）
            "有没有渠道编码这个参数" → search（含"有没有"）
            "帮我申请一个新参数"     → param_apply
            "配置一条快手投放链接"   → delivery_config
            "快手"（单词）           → general
            """;

    private static final String SKILL_PROMPT_SEARCH = """
            你是国金证券渠道投放平台的【全局搜索助手】。

            【格式要求】禁止任何 Markdown 语法。唯一例外：必须用 JSON 代码块包裹搜索结果卡片。

            【核心目标】根据用户关键词，搜索四类数据并输出 search_result_card JSON。

            执行流程：
            Step 1 —— 并发调用以下全部工具（keyword 均填用户关键词）：
              - query_params：参数库搜索
              - query_conv_strategies：回传策略搜索（channel_name 填关键词）
              - query_delivery_links：投放链接搜索
              - query_conv_nodes：回传节点搜索

            Step 2 —— 输出 search_result_card JSON（每类最多 5 条）：

            ```json
            {
              "type": "search_result_card",
              "keyword": "用户输入的关键词",
              "params": [
                {"name": "参数中文名", "key": "paramKey", "scene": "应用场景", "usage": "用途简述"}
              ],
              "strategies": [
                {"name": "策略名称", "channel": "渠道", "model": "策略模型", "nodes": "节点概览"}
              ],
              "links": [
                {"name": "链接名称", "channel": "渠道", "content": "投放内容", "status": "短链状态"}
              ],
              "nodes": [
                {"name": "节点名称", "type": "转化类型", "desc": "节点说明"}
              ]
            }
            ```

            约束：
            - 无结果的类别填空数组 []，禁止省略字段
            - 用户未提供关键词时只回复：请告诉我您想搜索的关键词，如渠道名称、参数名、策略名等
            - 禁止在 JSON 外使用 Markdown 格式
            """;

    private static final String SKILL_PROMPT_PARAM_APPLY = """
            你是国金证券渠道投放平台的【参数申请专员】，只处理投放参数的申请与配置，其他任何业务一律拒绝。

            【格式要求】回复使用纯文本，禁止 Markdown 语法（**加粗**、# 标题、- 列表等一律禁止）。唯一例外：必须用 JSON 代码块包裹预填卡。

            【绝对禁止】
            - 禁止在回复中出现"回传策略"、"回传节点"、"策略模型"等词汇
            - 禁止输出 strategy_prefill_card、delivery_prefill_card 或任何非 param_prefill_card 的 JSON 块
            - 禁止推荐节点、分析渠道策略、查询回传数据

            【核心目标】用户描述参数需求后，立即搜索参数库，然后输出 param_prefill_card，不得逐个追问字段。

            执行流程：

            Step 1 —— 搜索参数库
            调用 query_params，用用户描述中的关键词搜索（如"统计"、"注册"、"渠道"等）。

            Step 2 —— 自动提取字段并输出预填卡
            无论搜索有无结果，立即从用户的原始描述中提取以下字段，在回复末尾输出 param_prefill_card JSON 块：
              - applyType：有高度匹配的参数用 "existing"，否则用 "new"
              - parameterName：existing 时填写匹配参数的"中文名 | 英文名"，new 时填空字符串
              - scene：参数实际使用的业务场景描述，用简短短语说明在哪里/什么场景下使用该参数，如"百度网盘搜索入口"、"京东金融首页Banner"、"抖音下载落地页"。禁止填写"场景参数"/"活动参数"等分类词
              - usage：从用户描述中提炼 15-30 字的使用说明
              - value：existing 且用户提到具体值时填写，否则填空字符串

            Step 3 —— 用户确认后提交
            用户前端确认后发消息告知，根据 applyType：
              - existing：调用 create_my_param_config 写入草稿
              - new：调用 create_param_application 提交审核
            完成后告知用户可在「投放参数配置」或「参数申请管理」查看进度。

            约束：
            - Step 2 必须立即输出 param_prefill_card，不得先问问题
            - 搜索无结果时不编造参数名，parameterName 留空
            - 只处理参数申请相关问题，其他问题回复"该问题超出参数申请助手的服务范围"

            === param_prefill_card JSON 格式（唯一允许输出的 JSON 类型）===
            ```json
            {
              "type": "param_prefill_card",
              "applyType": "new",
              "parameterName": "",
              "scene": "用户描述的具体业务场景，如'百度网盘搜索入口'",
              "usage": "从用户描述中提炼的使用说明",
              "value": ""
            }
            ```
            """;

    private static final String SKILL_PROMPT_DELIVERY = """
            你是国金证券渠道投放平台的【投放链接配置专员】。

            【格式要求】回复使用纯文本，禁止 Markdown。唯一例外：预填卡必须用 JSON 代码块包裹。

            【核心目标】用户描述投放需求后，你必须立即搜索母链库、提取字段并输出预填卡，不得逐个追问字段。

            执行流程：

            Step 1 —— 搜索母链库
            立即调用 search_master_links，用用户描述中的产品名称或关键词搜索（如"AI选好股"、"财经课"等）。

            Step 2 —— 自动提取字段并输出预填卡
            无论搜索有无结果，立即从用户描述中提取以下字段，在回复末尾输出 delivery_prefill_card JSON 块。
            不得再追问用户任何字段，一律从描述中推断：
              - linkName：渠道-位置-产品，如"京东金融搜索入口-AI选好股"
              - channel：用户提到的投放渠道平台名称，如"京东金融"、"抖音"
              - channelCode：渠道英文编码（规则：抖音=douyin、京东/京东金融=jingdong、快手=kuaishou、雪球=xueqiu、腾讯自选股=txzxg、华为=huawei、小米=xiaomi）
              - content：投放内容分类，优先取母链 productCategory，否则推断（AI投顾产品/财经课/下载投放）
              - promoMode：默认"网销线上推广"，用户明确提到线下则用"网销线下推广"
              - originUrl：search_master_links 返回的 baseUrl，若未找到则留空字符串
              - motherLinkFound：是否在母链库找到（true/false）
              - motherLinkName：找到时填写母链产品名称

            Step 3 —— 用户在弹窗中确认后提交
            用户确认表单后会发送消息告知，此时调用 create_delivery_link 写入数据库。
            完成后告知用户可在「投放链接管理」列表查看。

            约束：
            - Step 2 必须立即输出 delivery_prefill_card，不得先问问题再输出
            - 未找到母链时在回复文本中提示用户手动填入原始链接，originUrl 填空字符串
            - 不处理与投放链接配置无关的问题

            === delivery_prefill_card JSON 格式（必须严格遵守）===
            ```json
            {
              "type": "delivery_prefill_card",
              "linkName": "渠道-位置-产品",
              "channel": "投放渠道名称",
              "channelCode": "渠道英文编码",
              "content": "AI投顾产品",
              "promoMode": "网销线上推广",
              "originUrl": "母链URL或空字符串",
              "motherLinkFound": true,
              "motherLinkName": "AI选好股"
            }
            ```
            """;

    private static final String SKILL_PROMPT_STRATEGY = """
            你是国金证券渠道投放平台的【回传策略配置专员】。

            【格式要求】回复使用纯文本，禁止 Markdown。唯一例外：预填卡和确认卡必须用 JSON 代码块包裹。

            【两种触发场景】

            场景A：用户通过弹窗填写好策略信息后，发送了已包含「策略名称/应用渠道/节点ID列表」等结构化字段的消息
            → 直接调用 create_conv_strategy_application 提交，无需再确认
            → 提交完成后告知用户策略编号，可在「回传策略配置」列表查看

            场景B：用户以自然语言描述需求（如"帮我配一个京东金融的下载回传策略"）
            → 按以下流程执行：

            Step 1 —— 查询现有策略与节点
            调用 query_conv_strategies 搜索渠道是否已有历史策略。
            调用 query_conv_nodes 获取推荐节点列表（用"下载"、"注册"等关键词搜索）。

            Step 2 —— 输出预填卡（不追问，立即输出）
            从用户描述中提取字段，输出 strategy_prefill_card JSON 块：
              - strategyName：渠道+模型名，如"京东金融-下载回传策略"
              - channel：用户提到的渠道
              - strategyRule：推断的策略模型（下载回传模型/下载留存模型/增值产品签约模型/自定义模型）
              - nodeIds：query_conv_nodes 返回的推荐节点 id 列表（最多取前6个匹配的）
              - callbackRatio：默认 100

            Step 3 —— 用户确认后提交（场景B最终汇入场景A处理）

            === strategy_prefill_card JSON 格式（必须严格遵守）===
            ```json
            {
              "type": "strategy_prefill_card",
              "strategyName": "渠道-策略名称",
              "channel": "投放渠道名称",
              "strategyRule": "下载回传模型",
              "nodeIds": [1, 2, 3],
              "callbackRatio": 100
            }
            ```
            """;

    private static final String SYSTEM_PROMPT = """
            你是"投放助手"，国金证券渠道投放中台的 AI 助手。

            ！！【最高优先级格式规则，必须严格遵守】！！
            回复全程只允许使用纯文本和数字序号。以下格式永远禁止：
            - 禁止 **文字** 或 __文字__ 加粗
            - 禁止 # 标题
            - 禁止 - 或 * 做列表前缀
            - 禁止 *斜体*
            - 禁止任何 Markdown 符号
            正确写法：用数字（1. 2. 3.）和换行表示列表，用文字本身表达重要性，不要用任何符号装饰。
            唯一例外：参数申请确认和投放链接生成结果必须用 JSON 代码块包裹。

            你可以帮用户完成以下操作：
            1. 查询现有投放参数（按关键词、分类搜索）
            2. 查看某个参数的详细信息和审核状态
            3. 配置我的参数：从参数库选一个已有参数，填写本次使用的场景、说明和取值（applyType=existing）
            4. 申请新参数：这个参数在系统中完全不存在，需要新建定义并进入审核（applyType=new）
            5. 查询回传节点（按关键词、事件分类搜索，或按 convType 值精确查找）
            6. 查询某个渠道的回传策略配置（查看渠道关联了哪些回传节点）
            7. 查询投放场景列表（按关键词、渠道搜索）
            8. 根据投放场景和参数生成完整的投放链接
            9. 配置投放链接：从描述中解析需求，匹配母链库，拼接参数，生成可投放 URL

            【重要】"配置/添加我的参数" 与 "申请新参数" 的区别——必须在第一句话就判断清楚：
            - 以下说法全部走"配置我的参数"流程（操作3），参数库中已存在该参数，只需选参数 + 填使用信息：
              "配置参数"、"配一条参数"、"给某个场景配参数"、"添加我的参数"、"添加一个我的参数"、"新增我的参数"、"我要加一条我的参数"、"帮我配参"
            - 以下说法走"申请新参数"流程（操作4），参数尚未在库中，需要创建全新定义：
              "申请参数"、"新建参数"、"这个参数系统里没有"、"我要申请一个新参数"
            - 如果用户说"配置/添加一条新的参数"，优先走操作3，调 query_params 查询，若确实不存在再询问是否要申请新参数
            - 不得在未判断清楚意图时就同时提示两种流程，也不得在用户说"添加我的参数"时直接问中文名/英文名（那是申请流程）

            使用规则：
            - 用户想搜索参数时，调用 query_params
            - 用户想看某个参数详情时，调用 get_param_detail
            - 用户想配置参数/添加我的参数（操作3）时：第一句回复说明将从参数库中选择参数，然后调用 query_params 展示可选列表，引导用户从中选择一个，再依次收集应用场景（场景参数/活动参数/人员参数/产品参数）、使用说明（AI辅助生成15-30字供用户确认）、参数值，全部确认后调用 create_my_param_config
            - 用户想申请新参数（操作4）时：明确告知这是新建参数申请流程，然后收集必要信息（中文名、英文名、分类、说明、申请人）
            - 当你收集完参数申请的所有信息、准备让用户最终确认时，必须在回复末尾附加如下格式的 JSON 代码块，除此之外不得再用文字列出参数清单。JSON 块前可以有一句简短引导语，例如"以下是申请信息，请确认："，但不得再用数字列表重复展示字段内容
            - 用户确认后调用 create_param_application 创建新参数申请，若用户取消则不创建
            - 用户想查询回传节点（按名称、convType、事件类型等）时，调用 query_conv_nodes
            - 用户想查看某个渠道配置了哪些回传节点时，调用 query_conv_strategy
            - 用户想查询投放场景时，调用 query_launch_scenes
            - 用户想生成投放链接时，调用 generate_launch_link，需要提供 scene_id，可选提供参数覆盖值
            - 分类只能是：场景参数、人员参数、活动参数、产品参数
            - 回复格式严格遵守最高优先级格式规则：纯文本，不得使用 **加粗**、# 标题、- 列表前缀、*斜体*，直接用数字序号和换行表达层级即可
            - 申请参数前务必和用户确认信息，不要自己编造参数名

            === 参数申请确认 JSON 格式（必须严格遵守，字段名区分大小写）===
            {
              "type": "param_confirm_card",
              "paramName": "参数中文名",
              "paramKey": "参数英文Key",
              "category": "参数分类",
              "description": "参数说明",
              "applicant": "申请人姓名"
            }

            === 投放链接配置规则 ===
            当用户意图是「配置投放链接」或「生成投放链接」时，按以下顺序调用工具：
            1. 调用 query_delivery_links 搜索母链，向用户确认使用哪条
            2. 调用 query_params_for_link 获取该链接所需参数，引导用户提供参数值
            3. 调用 generate_delivery_url 执行拼接，返回结构化结果
            4. 若 generate_delivery_url 返回的 missingParams 不为空，告知用户哪些参数尚未申请，并询问是否需要切换到参数申请流程
            5. 不得在用户未确认参数值的情况下直接调用 generate_delivery_url
            6. 若 query_delivery_links 返回 total=0，不得再次尝试其他关键词，改为调用 query_delivery_links(keyword="") 获取全部链接列表展示给用户选择，最多调用 query_delivery_links 共2次

            === 投放链接生成 JSON 格式（generate_delivery_url 返回后，必须在回复末尾附加此结构）===
            {
              "type": "delivery_url_card",
              "longUrl": "完整长链",
              "shortUrl": "短链",
              "params": { "key1": "value1" },
              "missingParams": []
            }

            === 回传策略配置规则 ===
            当用户意图是「配置回传策略」或「设置回传节点」时，按以下顺序处理：

            场景一（找到历史记录）：
            1. 调用 query_conv_strategies 搜索现有策略
            2. 若找到匹配结果，向用户展示策略详情和关联节点，询问是否在此基础上修改或直接使用
            3. 用户确认使用现有策略时，回复策略详情即可，无需再次写入

            场景二（未找到历史记录）：
            1. 调用 query_conv_strategies 搜索，结果为空
            2. 调用 query_conv_nodes 推荐合适的节点，引导用户选择
            3. 收集完整信息后（策略名、渠道、描述、选定节点列表、申请人），在回复末尾附加如下格式的 JSON 代码块：
            {
              "type": "conv_strategy_confirm_card",
              "strategyName": "策略名称",
              "channel": "投放渠道",
              "description": "策略描述",
              "nodes": [
                { "nodeId": 1, "nodeName": "节点名", "convType": "convType值" }
              ],
              "applicant": "申请人"
            }
            4. 用户回复「确认提交」后，调用 create_conv_strategy_application
            5. 不得在用户未确认节点列表的情况下直接调用 create_conv_strategy_application
            """;

    // ========== Tool 定义（Jackson 动态构建）==========

    private JsonNode buildToolDefinitions() {
        ArrayNode tools = objectMapper.createArrayNode();

        tools.add(buildFunctionTool("query_params",
                "查询现有投放参数列表，支持按关键词和分类筛选",
                schema1()));

        tools.add(buildFunctionTool("get_param_detail",
                "查询单个投放参数的完整详情，包括审核状态",
                schema2()));

        tools.add(buildFunctionTool("create_my_param_config",
                "配置我的参数：从参数库选一个已有参数，记录本次使用的应用场景、使用说明和参数取值。用户说「配置参数」时调用此工具，而非 create_param_application。",
                schemaMyParamConfig()));

        tools.add(buildFunctionTool("create_param_application",
                "申请全新的投放参数：该参数在系统中不存在，需要新建参数定义并进入审核流程。用户明确说「申请参数」「新建参数」时才调用，不得用于「配置已有参数」场景。",
                schema3()));

        tools.add(buildFunctionTool("query_conv_nodes",
                "查询回传节点列表，支持按关键词模糊搜索节点名称和说明，支持按事件分类筛选，也可按 convType 值精确查找",
                schema4()));

        tools.add(buildFunctionTool("query_conv_strategy",
                "查询某个渠道的回传策略配置，返回该渠道已关联的回传节点列表",
                schema5()));

        tools.add(buildFunctionTool("query_launch_scenes",
                "查询投放场景（投放链接）列表，支持按关键词和渠道名称筛选",
                schema6()));

        tools.add(buildFunctionTool("generate_launch_link",
                "根据投放场景ID和可选参数生成完整投放链接，会基于场景的原始链接拼接参数并生成短链",
                schema7()));

        tools.add(buildFunctionTool("query_delivery_links",
                "根据关键词搜索母链库，返回匹配的投放链接列表。用于用户描述投放平台/场景时检索可用母链。",
                schema8()));

        tools.add(buildFunctionTool("query_params_for_link",
                "给定母链 id，查询该链接需要拼接的参数列表，包含参数键名、参数说明和当前可选值。",
                schema9()));

        tools.add(buildFunctionTool("generate_delivery_url",
                "根据选定的母链和参数键值对，拼接完整投放链接。若发现必要参数用户尚未申请，返回缺失参数信息而非报错。",
                schema10()));

        tools.add(buildFunctionTool("query_conv_strategies",
                "根据关键词搜索现有回传策略列表，用于判断用户描述的场景是否已有历史配置",
                schema11()));

        tools.add(buildFunctionTool("create_conv_strategy_application",
                "用户确认后，创建回传策略配置申请。写入 conv_strategy 表和 conv_strategy_node 关联表",
                schema12()));

        tools.add(buildFunctionTool("create_delivery_link",
                "在母链库中新建投放链接，需提供链接名称、投放渠道、渠道编码和原始链接。渠道编码映射：抖音=douyin、京东/京东金融=jingdong、快手=kuaishou、雪球=xueqiu、腾讯自选股=txzxg、华为=huawei、小米=xiaomi",
                schema13()));

        tools.add(buildFunctionTool("search_master_links",
                "在母链库（master_link 表）中按产品名称关键词搜索基础 URL，用于 delivery_config skill 的 Step1 搜索",
                schema14()));

        return tools;
    }

    private ObjectNode buildFunctionTool(String name, String description, ObjectNode parameters) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");
        ObjectNode func = objectMapper.createObjectNode();
        func.put("name", name);
        func.put("description", description);
        func.set("parameters", parameters);
        tool.set("function", func);
        return tool;
    }

    private ObjectNode schema1() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode k = objectMapper.createObjectNode();
        k.put("type", "string");
        k.put("description", "搜索关键词，匹配参数中文名或英文名，可选");
        props.set("keyword", k);
        ObjectNode c = objectMapper.createObjectNode();
        c.put("type", "string");
        c.put("description", "参数分类筛选");
        ArrayNode en = objectMapper.createArrayNode();
        en.add("场景参数"); en.add("人员参数");
        en.add("活动参数"); en.add("产品参数");
        c.set("enum", en);
        props.set("category", c);
        schema.set("properties", props);
        return schema;
    }

    private ObjectNode schema2() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode pid = objectMapper.createObjectNode();
        pid.put("type", "integer");
        pid.put("description", "参数ID，与 param_key 二选一");
        props.set("param_id", pid);
        ObjectNode pk = objectMapper.createObjectNode();
        pk.put("type", "string");
        pk.put("description", "参数英文名，与 param_id 二选一");
        props.set("param_key", pk);
        schema.set("properties", props);
        return schema;
    }

    private ObjectNode schema3() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode nc = objectMapper.createObjectNode();
        nc.put("type", "string");
        nc.put("description", "参数中文名称");
        props.set("param_name_cn", nc);
        ObjectNode ne = objectMapper.createObjectNode();
        ne.put("type", "string");
        ne.put("description", "参数英文名称，仅支持英文、下划线、数字");
        props.set("param_name_en", ne);
        ObjectNode cat = objectMapper.createObjectNode();
        cat.put("type", "string");
        cat.put("description", "参数分类");
        ArrayNode en = objectMapper.createArrayNode();
        en.add("场景参数"); en.add("人员参数");
        en.add("活动参数"); en.add("产品参数");
        cat.set("enum", en);
        props.set("category", cat);
        ObjectNode desc = objectMapper.createObjectNode();
        desc.put("type", "string");
        desc.put("description", "参数应用说明");
        props.set("description", desc);
        ObjectNode app = objectMapper.createObjectNode();
        app.put("type", "string");
        app.put("description", "申请人姓名");
        props.set("applicant", app);
        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("param_name_cn"); req.add("param_name_en");
        req.add("category"); req.add("description"); req.add("applicant");
        schema.set("required", req);
        return schema;
    }

    private ObjectNode schemaMyParamConfig() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();

        ObjectNode pn = objectMapper.createObjectNode();
        pn.put("type", "string");
        pn.put("description", "参数名称，格式为「中文名 | 英文名」，如「推荐人 | recommend_code」，从 query_params 结果中取得");
        props.set("parameter_name", pn);

        ObjectNode sc = objectMapper.createObjectNode();
        sc.put("type", "string");
        sc.put("description", "参数应用场景分类");
        ArrayNode scEnum = objectMapper.createArrayNode();
        scEnum.add("场景参数"); scEnum.add("活动参数");
        scEnum.add("人员参数"); scEnum.add("产品参数");
        sc.set("enum", scEnum);
        props.set("scene", sc);

        ObjectNode us = objectMapper.createObjectNode();
        us.put("type", "string");
        us.put("description", "参数使用说明，15-30字，描述该参数在本次投放中的用途");
        props.set("usage", us);

        ObjectNode val = objectMapper.createObjectNode();
        val.put("type", "string");
        val.put("description", "参数取值，如具体的渠道代码、活动ID等");
        props.set("value", val);

        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("parameter_name"); req.add("scene");
        req.add("usage"); req.add("value");
        schema.set("required", req);
        return schema;
    }

    private ObjectNode schema4() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode kw = objectMapper.createObjectNode();
        kw.put("type", "string");
        kw.put("description", "搜索关键词，模糊匹配节点名称或事件分类，可选");
        props.set("keyword", kw);
        ObjectNode ct = objectMapper.createObjectNode();
        ct.put("type", "string");
        ct.put("description", "按 convType 值精确查找节点，可选");
        props.set("convType", ct);
        schema.set("properties", props);
        return schema;
    }

    private ObjectNode schema5() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode cn = objectMapper.createObjectNode();
        cn.put("type", "string");
        cn.put("description", "渠道名称，模糊匹配，如 京东金融、抖音、快手，与 channel_code 二选一");
        props.set("channel_name", cn);
        ObjectNode cc = objectMapper.createObjectNode();
        cc.put("type", "string");
        cc.put("description", "渠道代码，精确匹配，如 douyin、kuaishou，与 channel_name 二选一");
        props.set("channel_code", cc);
        schema.set("properties", props);
        return schema;
    }

    private ObjectNode schema6() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode kw = objectMapper.createObjectNode();
        kw.put("type", "string");
        kw.put("description", "搜索关键词，模糊匹配场景名称，可选");
        props.set("keyword", kw);
        ObjectNode cn = objectMapper.createObjectNode();
        cn.put("type", "string");
        cn.put("description", "渠道名称筛选，模糊匹配，可选");
        props.set("channel_name", cn);
        schema.set("properties", props);
        return schema;
    }

    private ObjectNode schema7() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode sid = objectMapper.createObjectNode();
        sid.put("type", "integer");
        sid.put("description", "投放场景ID（delivery_link 表的 id）");
        props.set("scene_id", sid);
        ObjectNode pv = objectMapper.createObjectNode();
        pv.put("type", "object");
        pv.put("description", "要覆盖或追加的参数键值对，可选");
        props.set("param_values", pv);
        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("scene_id");
        schema.set("required", req);
        return schema;
    }

    private ObjectNode schema8() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode kw = objectMapper.createObjectNode();
        kw.put("type", "string");
        kw.put("description", "搜索关键词，匹配链接名称、渠道或内容描述");
        props.set("keyword", kw);
        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("keyword");
        schema.set("required", req);
        return schema;
    }

    private ObjectNode schema9() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode lid = objectMapper.createObjectNode();
        lid.put("type", "integer");
        lid.put("description", "母链ID（delivery_link 表的 id）");
        props.set("linkId", lid);
        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("linkId");
        schema.set("required", req);
        return schema;
    }

    private ObjectNode schema10() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode lid = objectMapper.createObjectNode();
        lid.put("type", "integer");
        lid.put("description", "母链ID（delivery_link 表的 id）");
        props.set("linkId", lid);
        ObjectNode pm = objectMapper.createObjectNode();
        pm.put("type", "object");
        pm.put("description", "参数键值对，如 {\"param1\": \"value1\", \"param2\": \"value2\"}");
        props.set("paramMap", pm);
        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("linkId");
        req.add("paramMap");
        schema.set("required", req);
        return schema;
    }

    private ObjectNode schema11() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode kw = objectMapper.createObjectNode();
        kw.put("type", "string");
        kw.put("description", "搜索关键词，模糊匹配策略名称或渠道名称");
        props.set("keyword", kw);
        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("keyword");
        schema.set("required", req);
        return schema;
    }

    private ObjectNode schema12() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode sn = objectMapper.createObjectNode();
        sn.put("type", "string");
        sn.put("description", "策略名称");
        props.set("strategyName", sn);
        ObjectNode ch = objectMapper.createObjectNode();
        ch.put("type", "string");
        ch.put("description", "投放渠道名称");
        props.set("channel", ch);
        ObjectNode desc = objectMapper.createObjectNode();
        desc.put("type", "string");
        desc.put("description", "策略描述");
        props.set("description", desc);
        ObjectNode nids = objectMapper.createObjectNode();
        nids.put("type", "array");
        ObjectNode items = objectMapper.createObjectNode();
        items.put("type", "integer");
        nids.set("items", items);
        nids.put("description", "选定的回传节点ID列表");
        props.set("nodeIds", nids);
        ObjectNode app = objectMapper.createObjectNode();
        app.put("type", "string");
        app.put("description", "申请人姓名");
        props.set("applicant", app);
        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("strategyName"); req.add("channel");
        req.add("description"); req.add("nodeIds"); req.add("applicant");
        schema.set("required", req);
        return schema;
    }

    private ObjectNode schema13() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();

        ObjectNode ln = objectMapper.createObjectNode();
        ln.put("type", "string");
        ln.put("description", "链接名称");
        props.set("linkName", ln);

        ObjectNode ch = objectMapper.createObjectNode();
        ch.put("type", "string");
        ch.put("description", "投放渠道名称，如抖音、京东金融、快手等");
        props.set("channel", ch);

        ObjectNode cc = objectMapper.createObjectNode();
        cc.put("type", "string");
        cc.put("description", "渠道编码，如抖音对应 douyin，京东对应 jingdong，腾讯自选股对应 txzxg，快手对应 kuaishou");
        props.set("channelCode", cc);

        ObjectNode ou = objectMapper.createObjectNode();
        ou.put("type", "string");
        ou.put("description", "原始投放链接URL");
        props.set("originUrl", ou);

        ObjectNode ct = objectMapper.createObjectNode();
        ct.put("type", "string");
        ct.put("description", "投放内容描述");
        props.set("content", ct);

        ObjectNode pm = objectMapper.createObjectNode();
        pm.put("type", "string");
        pm.put("description", "推广模式，如信息流、短视频、横幅等");
        props.set("promoMode", pm);

        ObjectNode cn = objectMapper.createObjectNode();
        cn.put("type", "string");
        cn.put("description", "创建人姓名");
        props.set("creatorName", cn);

        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("linkName"); req.add("channel");
        req.add("channelCode"); req.add("originUrl");
        schema.set("required", req);
        return schema;
    }

    private ObjectNode schema14() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode kw = objectMapper.createObjectNode();
        kw.put("type", "string");
        kw.put("description", "产品名称关键词，如「AI选好股」「财经课」，模糊匹配 product_name");
        props.set("keyword", kw);
        schema.set("properties", props);
        ArrayNode req = objectMapper.createArrayNode();
        req.add("keyword");
        schema.set("required", req);
        return schema;
    }

    public AgentServiceImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            PlacementParamMapper placementParamMapper,
            MyParameterMapper myParameterMapper,
            ConvNodeMapper convNodeMapper,
            ConvStrategyMapper convStrategyMapper,
            ConvStrategyNodeMapper convStrategyNodeMapper,
            DeliveryLinkMapper deliveryLinkMapper,
            MasterLinkMapper masterLinkMapper,
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.model:deepseek-chat}") String model,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.placementParamMapper = placementParamMapper;
        this.myParameterMapper = myParameterMapper;
        this.convNodeMapper = convNodeMapper;
        this.convStrategyMapper = convStrategyMapper;
        this.convStrategyNodeMapper = convStrategyNodeMapper;
        this.deliveryLinkMapper = deliveryLinkMapper;
        this.masterLinkMapper = masterLinkMapper;
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
            // 先跑 Dispatcher 识别最新用户意图
            // 若 dispatcher 识别到明确 skill，优先使用（用户可能在会话中途切换了意图）
            // 若 dispatcher 返回 general，再 fallback 到前端传来的 skillType（维持会话连续性）
            String dispatched = dispatchSkill(request.getMessages());
            String skillType = !SKILL_GENERAL.equals(dispatched)
                    ? dispatched
                    : (StringUtils.hasText(request.getSkillType()) ? request.getSkillType() : SKILL_GENERAL);
            log.info("Skill 路由: {}", skillType);

            String systemPrompt = getSystemPromptForSkill(skillType);
            JsonNode tools = buildToolDefinitionsForSkill(skillType);

            List<Object> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            for (AgentChatRequest.Message msg : request.getMessages()) {
                if ("system".equals(msg.getRole())) continue;
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }

            String finalText = runAgentLoop(messages, tools);

            AgentChatResponse chatResponse = new AgentChatResponse();
            chatResponse.setContent(finalText);
            chatResponse.setSessionId(sessionId);
            chatResponse.setSkillType(skillType);
            return chatResponse;

        } catch (Exception e) {
            log.error("AI 对话失败", e);
            AgentChatResponse fallback = new AgentChatResponse();
            fallback.setContent("抱歉，AI 助手暂时无法响应，请稍后重试。");
            fallback.setSessionId(sessionId);
            return fallback;
        }
    }

    // ========== Dispatcher：轻量意图分类 ==========

    private String dispatchSkill(List<AgentChatRequest.Message> messages) {
        try {
            // 取最后一条用户消息作为分类依据
            String lastUser = messages.stream()
                    .filter(m -> "user".equals(m.getRole()))
                    .map(AgentChatRequest.Message::getContent)
                    .reduce("", (a, b) -> b);
            if (!StringUtils.hasText(lastUser)) return SKILL_GENERAL;

            List<Object> dispatchMessages = List.of(
                    Map.of("role", "system", "content", DISPATCHER_PROMPT),
                    Map.of("role", "user", "content", lastUser)
            );

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", 20);
            body.put("messages", dispatchMessages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + "/v1/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                    String.class);

            String result = objectMapper.readTree(resp.getBody())
                    .path("choices").get(0)
                    .path("message").path("content").asText().trim().toLowerCase();

            if (result.contains("param_apply"))    return SKILL_PARAM_APPLY;
            if (result.contains("delivery_config")) return SKILL_DELIVERY;
            if (result.contains("strategy_config")) return SKILL_STRATEGY;
            if (result.contains("search"))          return SKILL_SEARCH;
            return SKILL_GENERAL;

        } catch (Exception e) {
            log.warn("Dispatcher 调用失败，降级为 general: {}", e.getMessage());
            return SKILL_GENERAL;
        }
    }

    // ========== Skill 路由辅助 ==========

    private String getSystemPromptForSkill(String skillType) {
        return switch (skillType) {
            case SKILL_PARAM_APPLY -> SKILL_PROMPT_PARAM_APPLY;
            case SKILL_DELIVERY    -> SKILL_PROMPT_DELIVERY;
            case SKILL_STRATEGY    -> SKILL_PROMPT_STRATEGY;
            case SKILL_SEARCH      -> SKILL_PROMPT_SEARCH;
            default                -> SYSTEM_PROMPT;
        };
    }

    private JsonNode buildToolDefinitionsForSkill(String skillType) {
        Set<String> allowed = switch (skillType) {
            case SKILL_PARAM_APPLY -> Set.of(
                    "query_params", "get_param_detail",
                    "create_my_param_config", "create_param_application");
            case SKILL_DELIVERY    -> Set.of(
                    "search_master_links", "create_delivery_link",
                    "query_delivery_links", "query_params_for_link",
                    "generate_delivery_url", "query_params");
            case SKILL_STRATEGY    -> Set.of(
                    "query_conv_nodes", "query_conv_strategies",
                    "create_conv_strategy_application");
            case SKILL_SEARCH      -> Set.of(
                    "query_params", "query_conv_strategies",
                    "query_delivery_links", "query_conv_nodes");
            default                -> null; // null = 全部工具
        };

        JsonNode allTools = buildToolDefinitions();
        if (allowed == null) return allTools;

        ArrayNode filtered = objectMapper.createArrayNode();
        for (JsonNode tool : allTools) {
            String name = tool.path("function").path("name").asText();
            if (allowed.contains(name)) filtered.add(tool);
        }
        return filtered;
    }

    // ========== Agent 循环：调用 DeepSeek → 处理 tool_calls → 回传 tool 消息 → 再调 DeepSeek ==========

    private String runAgentLoop(List<Object> messages, JsonNode tools) throws Exception {
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            JsonNode response = callDeepSeek(messages, tools);

            JsonNode choice = response.path("choices").get(0);
            String finishReason = choice.path("finish_reason").asText();
            JsonNode message = choice.path("message");

            if (!"tool_calls".equals(finishReason)) {
                if (message.has("content") && !message.get("content").isNull()) {
                    return message.get("content").asText();
                }
                return "好的，已处理完成。";
            }

            // 1. 把 assistant 消息（含 tool_calls）追加到 messages
            JsonNode tcArray = message.path("tool_calls");
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            List<Map<String, Object>> toolMessages = new ArrayList<>();

            for (JsonNode tc : tcArray) {
                String toolCallId = tc.path("id").asText();
                String toolName = tc.path("function").path("name").asText();
                String arguments = tc.path("function").path("arguments").asText();

                log.info("执行工具: {} 参数: {}", toolName, arguments);
                JsonNode input = objectMapper.readTree(arguments);
                String result = executeTool(toolName, input);
                log.info("工具结果: {}", result.length() > 200 ? result.substring(0, 200) + "..." : result);

                toolCalls.add(Map.of(
                        "id", toolCallId,
                        "type", "function",
                        "function", Map.of(
                                "name", toolName,
                                "arguments", arguments
                        )));

                toolMessages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", toolCallId,
                        "content", result));
            }

            String assistantContent = message.has("content") && !message.get("content").isNull()
                    ? message.get("content").asText() : "";

            Map<String, Object> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            if (!assistantContent.isEmpty()) {
                assistantMsg.put("content", assistantContent);
            }
            assistantMsg.put("tool_calls", toolCalls);
            messages.add(assistantMsg);
            messages.addAll(toolMessages);
        }

        return "处理轮次过多，请简化您的问题后重试。";
    }

    private static final int MAX_RETRIES = 2;

    private JsonNode callDeepSeek(List<Object> messages, JsonNode tools) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 2048);
        body.put("messages", messages);
        body.put("tools", tools);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        String requestBody = objectMapper.writeValueAsString(body);
        log.debug("DeepSeek 请求: {}", requestBody.length() > 500 ? requestBody.substring(0, 500) + "..." : requestBody);

        HttpEntity<String> httpEntity = new HttpEntity<>(requestBody, headers);

        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    log.warn("DeepSeek API 第{}次重试...", attempt);
                    Thread.sleep(attempt * 2000L);
                }
                ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl + "/v1/chat/completions",
                        HttpMethod.POST,
                        httpEntity,
                        String.class);
                return objectMapper.readTree(response.getBody());
            } catch (org.springframework.web.client.ResourceAccessException e) {
                lastException = e;
                log.warn("DeepSeek API 网络异常 (attempt {}): {}", attempt + 1, e.getMessage());
            }
        }
        throw lastException;
    }

    // ========== Tool 执行分发 ==========

    private String executeTool(String toolName, JsonNode input) {
        try {
            return switch (toolName) {
                case "query_params" -> toolQueryParams(input);
                case "get_param_detail" -> toolGetParamDetail(input);
                case "create_my_param_config" -> toolCreateMyParamConfig(input);
                case "create_param_application" -> toolCreateParamApplication(input);
                case "query_conv_nodes" -> toolQueryConvNodes(input);
                case "query_conv_strategy" -> toolQueryConvStrategy(input);
                case "query_launch_scenes" -> toolQueryLaunchScenes(input);
                case "generate_launch_link" -> toolGenerateLaunchLink(input);
                case "query_delivery_links" -> toolQueryDeliveryLinks(input);
                case "query_params_for_link" -> toolQueryParamsForLink(input);
                case "generate_delivery_url" -> toolGenerateDeliveryUrl(input);
                case "query_conv_strategies" -> toolQueryConvStrategies(input);
                case "create_conv_strategy_application" -> toolCreateConvStrategyApplication(input);
                case "create_delivery_link" -> toolCreateDeliveryLink(input);
                case "search_master_links" -> toolSearchMasterLinks(input);
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

    // ========== Tool: create_my_param_config ==========

    private String toolCreateMyParamConfig(JsonNode input) throws Exception {
        String parameterName = input.get("parameter_name").asText();
        String scene = input.get("scene").asText();
        String usage = input.get("usage").asText();
        String value = input.get("value").asText();
        MyParameter myParam = new MyParameter();
        myParam.setParameterName(parameterName);
        myParam.setScene(scene);
        myParam.setUsage(usage);
        myParam.setValue(value);
        myParam.setStatus(0);  // 待提交（草稿状态，用户可在列表页手动提交审核）
        myParameterMapper.insert(myParam);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("id", myParam.getId());
        result.put("parameter_name", parameterName);
        result.put("scene", scene);
        result.put("value", value);
        result.put("status", "已保存（待提交）");
        result.put("message", "参数配置已保存，编号 " + myParam.getId() + "。如需进入审核，请在「投放参数配置」列表中点击「提交」。");
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

    // ========== Tool 4: query_conv_nodes ==========

    private String toolQueryConvNodes(JsonNode input) throws Exception {
        String keyword = input.has("keyword") ? input.get("keyword").asText("") : "";
        String convTypeStr = input.has("convType") && !input.get("convType").isNull()
                ? input.get("convType").asText() : "";

        LambdaQueryWrapper<ConvNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(ConvNode::getNodeName, keyword)
                        .or()
                        .like(ConvNode::getEventCategory, keyword)
                        .or()
                        .like(ConvNode::getEventDesc, keyword));
        if (StringUtils.hasText(convTypeStr)) {
            try {
                wrapper.eq(ConvNode::getConvType, Integer.parseInt(convTypeStr));
            } catch (NumberFormatException ignored) {
                // ignore invalid convType
            }
        }
        wrapper.orderByAsc(ConvNode::getConvType);

        List<ConvNode> list = convNodeMapper.selectList(wrapper);

        ArrayNode nodes = objectMapper.createArrayNode();
        for (ConvNode n : list) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", n.getId());
            node.put("nodeName", n.getNodeName());
            node.put("convType", n.getConvType());
            node.put("eventCategory", n.getEventCategory());
            node.put("description", n.getEventDesc());
            node.put("sampleValue", n.getEventJudge() != null ? n.getEventJudge() : "");
            nodes.add(node);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("total", list.size());
        response.set("convNodes", nodes);
        return objectMapper.writeValueAsString(response);
    }

    // ========== Tool 5: query_conv_strategy ==========

    private String toolQueryConvStrategy(JsonNode input) throws Exception {
        String channelName = input.has("channel_name") ? input.get("channel_name").asText("") : "";
        String channelCode = input.has("channel_code") ? input.get("channel_code").asText("") : "";

        if (!StringUtils.hasText(channelName) && !StringUtils.hasText(channelCode)) {
            return "{\"error\": \"请提供渠道名称(channel_name)或渠道代码(channel_code)\"}";
        }

        LambdaQueryWrapper<ConvStrategy> wrapper = new LambdaQueryWrapper<ConvStrategy>()
                .like(StringUtils.hasText(channelName), ConvStrategy::getChannel, channelName)
                .eq(StringUtils.hasText(channelCode), ConvStrategy::getChannelCode, channelCode)
                .orderByDesc(ConvStrategy::getUpdatedAt);

        List<ConvStrategy> strategies = convStrategyMapper.selectList(wrapper);

        if (strategies.isEmpty()) {
            return "{\"error\": \"未找到该渠道的回传策略配置\"}";
        }

        ArrayNode strategyArray = objectMapper.createArrayNode();
        for (ConvStrategy strategy : strategies) {
            ObjectNode strategyNode = objectMapper.createObjectNode();
            strategyNode.put("strategy_id", strategy.getId());
            strategyNode.put("strategy_name", strategy.getStrategyName());
            strategyNode.put("channel", strategy.getChannel());
            strategyNode.put("channel_code", strategy.getChannelCode());
            strategyNode.put("callback_ratio", strategy.getCallbackRatio());
            strategyNode.put("node_overview", strategy.getNodeOverview());

            List<ConvStrategyNode> links = convStrategyNodeMapper.selectList(
                    new LambdaQueryWrapper<ConvStrategyNode>()
                            .eq(ConvStrategyNode::getStrategyId, strategy.getId())
                            .orderByAsc(ConvStrategyNode::getSortOrder));

            ArrayNode nodeArray = objectMapper.createArrayNode();
            for (ConvStrategyNode link : links) {
                ConvNode convNode = convNodeMapper.selectById(link.getNodeId());
                if (convNode != null) {
                    ObjectNode n = objectMapper.createObjectNode();
                    n.put("node_name", convNode.getNodeName());
                    n.put("conv_type", convNode.getConvType());
                    n.put("event_category", convNode.getEventCategory());
                    n.put("event_desc", convNode.getEventDesc());
                    n.put("sort_order", link.getSortOrder());
                    nodeArray.add(n);
                }
            }
            strategyNode.put("node_count", nodeArray.size());
            strategyNode.set("nodes", nodeArray);
            strategyArray.add(strategyNode);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("total", strategies.size());
        response.set("strategies", strategyArray);
        return objectMapper.writeValueAsString(response);
    }

    // ========== Tool 6: query_launch_scenes ==========

    private String toolQueryLaunchScenes(JsonNode input) throws Exception {
        String keyword = input.has("keyword") ? input.get("keyword").asText("") : "";
        String channelName = input.has("channel_name") ? input.get("channel_name").asText("") : "";

        LambdaQueryWrapper<DeliveryLink> wrapper = new LambdaQueryWrapper<DeliveryLink>()
                .like(StringUtils.hasText(keyword), DeliveryLink::getLinkName, keyword)
                .like(StringUtils.hasText(channelName), DeliveryLink::getChannel, channelName)
                .orderByDesc(DeliveryLink::getUpdatedAt);

        List<DeliveryLink> list = deliveryLinkMapper.selectList(wrapper);

        ArrayNode scenes = objectMapper.createArrayNode();
        for (DeliveryLink dl : list) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("scene_id", dl.getId());
            node.put("link_name", dl.getLinkName());
            node.put("channel", dl.getChannel());
            node.put("content", dl.getContent());
            node.put("promo_mode", dl.getPromoMode());
            node.put("origin_url", dl.getOriginUrl());
            node.put("full_url", dl.getFullUrl());
            node.put("short_url", dl.getShortUrl());
            node.put("link_status", dl.getLinkStatus());
            scenes.add(node);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("total", list.size());
        response.set("scenes", scenes);
        return objectMapper.writeValueAsString(response);
    }

    // ========== Tool 7: generate_launch_link ==========

    private String toolGenerateLaunchLink(JsonNode input) throws Exception {
        Long sceneId = input.get("scene_id").asLong();
        DeliveryLink link = deliveryLinkMapper.selectById(sceneId);

        if (link == null) {
            return "{\"error\": \"未找到该投放场景，scene_id: " + sceneId + "\"}";
        }

        Map<String, String> params = new LinkedHashMap<>();

        // 先加载场景已有的参数
        if (StringUtils.hasText(link.getParamsJson())) {
            JsonNode existing = objectMapper.readTree(link.getParamsJson());
            existing.fields().forEachRemaining(e -> {
                if (e.getValue().isTextual()) {
                    params.put(e.getKey(), e.getValue().asText());
                } else {
                    params.put(e.getKey(), e.getValue().toString());
                }
            });
        }

        // 用用户传入的参数覆盖
        if (input.has("param_values") && !input.get("param_values").isNull()) {
            JsonNode overrides = input.get("param_values");
            overrides.fields().forEachRemaining(e -> {
                if (e.getValue().isTextual()) {
                    params.put(e.getKey(), e.getValue().asText());
                } else {
                    params.put(e.getKey(), e.getValue().toString());
                }
            });
        }

        String fullUrl = buildAgentFullUrl(link.getOriginUrl(), params);
        String shortUrl = link.getShortUrl();

        ObjectNode result = objectMapper.createObjectNode();
        result.put("scene_id", link.getId());
        result.put("link_name", link.getLinkName());
        result.put("channel", link.getChannel());
        result.put("origin_url", link.getOriginUrl());
        result.put("full_url", fullUrl);
        result.put("short_url", shortUrl != null ? shortUrl : "暂无短链");

        ArrayNode paramList = objectMapper.createArrayNode();
        params.forEach((k, v) -> {
            ObjectNode p = objectMapper.createObjectNode();
            p.put("key", k);
            p.put("value", v);
            paramList.add(p);
        });
        result.set("params", paramList);

        return objectMapper.writeValueAsString(result);
    }

    private String buildAgentFullUrl(String originUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) return originUrl;
        String query = params.entrySet().stream()
                .filter(e -> StringUtils.hasText(e.getValue()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        if (!StringUtils.hasText(query)) return originUrl;
        return originUrl + (originUrl.contains("?") ? "&" : "?") + query;
    }

    // ========== 投放链接配置 Tool 8: query_delivery_links ==========

    private String toolQueryDeliveryLinks(JsonNode input) throws Exception {
        String keyword = input.has("keyword") && !input.get("keyword").isNull()
                ? input.get("keyword").asText() : "";

        LambdaQueryWrapper<DeliveryLink> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(DeliveryLink::getLinkName, keyword)
                    .or()
                    .like(DeliveryLink::getChannel, keyword)
                    .or()
                    .like(DeliveryLink::getContent, keyword));
        }
        wrapper.orderByDesc(DeliveryLink::getUpdatedAt)
                .last("LIMIT 20");

        List<DeliveryLink> list = deliveryLinkMapper.selectList(wrapper);

        ArrayNode links = objectMapper.createArrayNode();
        for (DeliveryLink dl : list) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", dl.getId());
            node.put("linkName", dl.getLinkName());
            node.put("channel", dl.getChannel());
            node.put("content", dl.getContent());
            node.put("originUrl", dl.getOriginUrl());
            node.put("shortUrl", dl.getShortUrl());
            node.put("linkStatus", dl.getLinkStatus());
            links.add(node);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("total", list.size());
        response.set("links", links);
        return objectMapper.writeValueAsString(response);
    }

    // ========== 投放链接配置 Tool 9: query_params_for_link ==========

    private String toolQueryParamsForLink(JsonNode input) throws Exception {
        Long linkId = input.get("linkId").asLong();

        DeliveryLink link = deliveryLinkMapper.selectById(linkId);
        if (link == null) {
            return "{\"error\": \"未找到该母链，linkId: " + linkId + "\"}";
        }

        // 查询该渠道下已审核通过的参数
        List<PlacementParam> params = placementParamMapper.selectList(
                new LambdaQueryWrapper<PlacementParam>()
                        .eq(PlacementParam::getAuditStatus, 1)
                        .and(w -> w
                                .eq(PlacementParam::getChannelCode, link.getChannel())
                                .or()
                                .isNull(PlacementParam::getChannelCode))
                        .eq(PlacementParam::getStatus, 1)
                        .orderByDesc(PlacementParam::getUpdatedAt));

        ArrayNode paramArray = objectMapper.createArrayNode();
        for (PlacementParam p : params) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("paramId", p.getId());
            node.put("paramKey", p.getParamKey());
            node.put("paramName", p.getParamName());
            node.put("description", p.getParamUsage());
            node.put("sampleValue", p.getParamValue());
            node.put("category", p.getCategory());
            paramArray.add(node);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("linkId", linkId);
        response.put("linkName", link.getLinkName());
        response.put("channel", link.getChannel());
        response.put("originUrl", link.getOriginUrl());
        response.put("total", paramArray.size());
        response.set("params", paramArray);
        return objectMapper.writeValueAsString(response);
    }

    // ========== 投放链接配置 Tool 10: generate_delivery_url ==========

    private String toolGenerateDeliveryUrl(JsonNode input) throws Exception {
        Long linkId = input.get("linkId").asLong();

        DeliveryLink link = deliveryLinkMapper.selectById(linkId);
        if (link == null) {
            return "{\"error\": \"未找到该母链，linkId: " + linkId + "\"}";
        }

        // 解析用户传入的参数
        Map<String, String> providedParams = new LinkedHashMap<>();
        if (input.has("paramMap") && !input.get("paramMap").isNull()) {
            JsonNode paramMap = input.get("paramMap");
            paramMap.fields().forEachRemaining(e -> {
                String val = e.getValue().isTextual()
                        ? e.getValue().asText()
                        : e.getValue().toString();
                providedParams.put(e.getKey(), val);
            });
        }

        // 查询该渠道下所有需要拼接的参数
        List<PlacementParam> requiredParams = placementParamMapper.selectList(
                new LambdaQueryWrapper<PlacementParam>()
                        .eq(PlacementParam::getAuditStatus, 1)
                        .eq(PlacementParam::getStatus, 1)
                        .and(w -> w
                                .eq(PlacementParam::getChannelCode, link.getChannel())
                                .or()
                                .isNull(PlacementParam::getChannelCode)));

        // 检查缺失参数
        ArrayNode missingArr = objectMapper.createArrayNode();
        for (PlacementParam p : requiredParams) {
            if (!providedParams.containsKey(p.getParamKey())) {
                ObjectNode m = objectMapper.createObjectNode();
                m.put("paramKey", p.getParamKey());
                m.put("paramName", p.getParamName());
                missingArr.add(m);
            }
        }

        // 拼接完整链接
        String longUrl = buildAgentFullUrl(link.getOriginUrl(), providedParams);

        // 生成模拟短链
        String shortUrl = link.getShortUrl();
        if (!StringUtils.hasText(shortUrl)) {
            String mockId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            shortUrl = "https://s.sinolink.com/" + mockId;
        }

        // 构建参数展示对象
        ObjectNode paramsNode = objectMapper.createObjectNode();
        providedParams.forEach(paramsNode::put);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("type", "delivery_url_card");
        result.put("longUrl", longUrl);
        result.put("shortUrl", shortUrl);
        result.set("params", paramsNode);
        result.set("missingParams", missingArr);

        return objectMapper.writeValueAsString(result);
    }

    // ========== 回传策略配置 Tool 11: query_conv_strategies ==========

    private String toolQueryConvStrategies(JsonNode input) throws Exception {
        String keyword = input.has("keyword") ? input.get("keyword").asText("") : "";

        // 查询匹配的策略，并关联节点名称
        LambdaQueryWrapper<ConvStrategy> wrapper = new LambdaQueryWrapper<ConvStrategy>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(ConvStrategy::getStrategyName, keyword)
                        .or()
                        .like(ConvStrategy::getChannel, keyword))
                .orderByDesc(ConvStrategy::getUpdatedAt)
                .last("LIMIT 10");

        List<ConvStrategy> strategies = convStrategyMapper.selectList(wrapper);

        ArrayNode strategyArray = objectMapper.createArrayNode();
        for (ConvStrategy cs : strategies) {
            // 查询关联节点
            List<ConvStrategyNode> links = convStrategyNodeMapper.selectList(
                    new LambdaQueryWrapper<ConvStrategyNode>()
                            .eq(ConvStrategyNode::getStrategyId, cs.getId()));

            List<String> nodeNames = new ArrayList<>();
            for (ConvStrategyNode link : links) {
                ConvNode cn = convNodeMapper.selectById(link.getNodeId());
                if (cn != null) {
                    nodeNames.add(cn.getNodeName());
                }
            }

            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", cs.getId());
            node.put("strategyName", cs.getStrategyName());
            node.put("channel", cs.getChannel());
            node.put("description", cs.getDescription() != null ? cs.getDescription() : "");
            node.put("nodeNames", String.join("、", nodeNames));
            strategyArray.add(node);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("total", strategyArray.size());
        response.set("strategies", strategyArray);
        return objectMapper.writeValueAsString(response);
    }

    // ========== 回传策略配置 Tool 12: create_conv_strategy_application ==========

    private String toolCreateConvStrategyApplication(JsonNode input) throws Exception {
        String strategyName = input.get("strategyName").asText();
        String channel = input.get("channel").asText();
        String description = input.get("description").asText();
        String applicant = input.get("applicant").asText();

        ArrayNode nodeIdsArray = (ArrayNode) input.get("nodeIds");
        List<Long> nodeIds = new ArrayList<>();
        for (JsonNode nid : nodeIdsArray) {
            nodeIds.add(nid.asLong());
        }

        // 1. 插入 conv_strategy 主表
        LocalDateTime now = LocalDateTime.now();
        ConvStrategy cs = new ConvStrategy();
        cs.setStrategyName(strategyName);
        cs.setChannel(channel);
        cs.setDescription(description);
        cs.setCreatorName(applicant);
        cs.setAuditStatus(0);
        cs.setCreatedAt(now);
        cs.setUpdatedAt(now);
        cs.setDeleted(0);
        convStrategyMapper.insert(cs);

        // 2. 逐条插入关联节点
        int sortOrder = 0;
        for (Long nodeId : nodeIds) {
            ConvStrategyNode csn = new ConvStrategyNode();
            csn.setStrategyId(cs.getId());
            csn.setNodeId(nodeId);
            csn.setSortOrder(sortOrder++);
            convStrategyNodeMapper.insert(csn);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("strategyId", cs.getId());
        result.put("strategyName", strategyName);
        result.put("nodeCount", nodeIds.size());
        result.put("status", "已提交，待审核");
        result.put("message", "回传策略申请已创建，编号 " + cs.getId() + "，等待管理员审核");
        return objectMapper.writeValueAsString(result);
    }

    // ========== 投放链接配置 Tool 13: create_delivery_link ==========

    /** 渠道名称 → 英文编码兜底映射 */
    private static String deriveChannelCode(String channel, String provided) {
        if (org.springframework.util.StringUtils.hasText(provided)) return provided;
        if (!org.springframework.util.StringUtils.hasText(channel)) return "";
        String c = channel.toLowerCase();
        if (c.contains("抖音"))    return "douyin";
        if (c.contains("京东"))    return "jingdong";
        if (c.contains("快手"))    return "kuaishou";
        if (c.contains("雪球"))    return "xueqiu";
        if (c.contains("腾讯自选股") || c.contains("txzxg")) return "txzxg";
        if (c.contains("华为"))    return "huawei";
        if (c.contains("小米"))    return "xiaomi";
        // 默认取首个非中文词作为编码
        return channel.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private String toolCreateDeliveryLink(JsonNode input) throws Exception {
        String linkName = input.get("linkName").asText();
        String channel = input.get("channel").asText();
        String channelCode = deriveChannelCode(channel,
                input.has("channelCode") ? input.get("channelCode").asText("") : "");
        String originUrl = input.has("originUrl") ? input.get("originUrl").asText("") : "";

        DeliveryLink link = new DeliveryLink();
        link.setLinkName(linkName);
        link.setChannel(channel);
        link.setChannelCode(channelCode);
        link.setOriginUrl(originUrl);

        if (input.has("content") && !input.get("content").isNull()) {
            link.setContent(input.get("content").asText());
        }
        if (input.has("promoMode") && !input.get("promoMode").isNull()) {
            link.setPromoMode(input.get("promoMode").asText());
        }
        if (input.has("creatorName") && !input.get("creatorName").isNull()) {
            link.setCreatorName(input.get("creatorName").asText());
        } else {
            link.setCreatorName(getCurrentUsername());
        }
        link.setLinkStatus("active");

        deliveryLinkMapper.insert(link);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("linkId", link.getId());
        result.put("linkName", linkName);
        result.put("channel", channel);
        result.put("channelCode", channelCode);
        result.put("originUrl", originUrl);
        result.put("message", "投放链接已创建，编号 " + link.getId());
        return objectMapper.writeValueAsString(result);
    }

    // ========== 母链库 Tool: search_master_links ==========

    private String toolSearchMasterLinks(JsonNode input) throws Exception {
        String keyword = input.has("keyword") ? input.get("keyword").asText("") : "";

        LambdaQueryWrapper<MasterLink> wrapper = new LambdaQueryWrapper<MasterLink>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(MasterLink::getProductName, keyword)
                        .or()
                        .like(MasterLink::getDescription, keyword))
                .orderByDesc(MasterLink::getUpdatedAt)
                .last("LIMIT 10");

        List<MasterLink> list = masterLinkMapper.selectList(wrapper);

        ArrayNode links = objectMapper.createArrayNode();
        for (MasterLink ml : list) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", ml.getId());
            node.put("productName", ml.getProductName());
            node.put("baseUrl", ml.getBaseUrl());
            node.put("productCategory", ml.getProductCategory() != null ? ml.getProductCategory() : "");
            node.put("description", ml.getDescription() != null ? ml.getDescription() : "");
            links.add(node);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("total", list.size());
        response.set("masterLinks", links);
        return objectMapper.writeValueAsString(response);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
