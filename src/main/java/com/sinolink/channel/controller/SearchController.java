package com.sinolink.channel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.entity.ConvNode;
import com.sinolink.channel.entity.ConvStrategy;
import com.sinolink.channel.entity.DeliveryLink;
import com.sinolink.channel.entity.PlacementParam;
import com.sinolink.channel.mapper.ConvNodeMapper;
import com.sinolink.channel.mapper.ConvStrategyMapper;
import com.sinolink.channel.mapper.DeliveryLinkMapper;
import com.sinolink.channel.mapper.PlacementParamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局搜索接口（直接查库，无需 AI，公开访问）
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final PlacementParamMapper placementParamMapper;
    private final ConvStrategyMapper   convStrategyMapper;
    private final DeliveryLinkMapper   deliveryLinkMapper;
    private final ConvNodeMapper       convNodeMapper;

    @GetMapping
    public Result<Map<String, Object>> search(@RequestParam(defaultValue = "") String keyword) {

        // ---- 1. 投放参数 ----
        List<Map<String, String>> params = new ArrayList<>();
        List<PlacementParam> paramList = placementParamMapper.selectList(
                new LambdaQueryWrapper<PlacementParam>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(PlacementParam::getParamName, keyword)
                                .or()
                                .like(PlacementParam::getParamKey,  keyword)
                                .or()
                                .like(PlacementParam::getParamUsage, keyword))
                        .last("LIMIT 5"));
        for (PlacementParam p : paramList) {
            Map<String, String> item = new HashMap<>();
            item.put("name",  p.getParamName());
            item.put("key",   p.getParamKey());
            item.put("scene", p.getCategory());
            item.put("usage", p.getParamUsage());
            params.add(item);
        }

        // ---- 2. 回传策略 ----
        List<Map<String, String>> strategies = new ArrayList<>();
        List<ConvStrategy> strategyList = convStrategyMapper.selectList(
                new LambdaQueryWrapper<ConvStrategy>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(ConvStrategy::getStrategyName, keyword)
                                .or()
                                .like(ConvStrategy::getChannel, keyword))
                        .last("LIMIT 5"));
        for (ConvStrategy s : strategyList) {
            Map<String, String> item = new HashMap<>();
            item.put("name",    s.getStrategyName());
            item.put("channel", s.getChannel());
            item.put("model",   s.getStrategyRule());
            item.put("nodes",   s.getNodeOverview());
            strategies.add(item);
        }

        // ---- 3. 投放链接 ----
        List<Map<String, String>> links = new ArrayList<>();
        List<DeliveryLink> linkList = deliveryLinkMapper.selectList(
                new LambdaQueryWrapper<DeliveryLink>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(DeliveryLink::getLinkName, keyword)
                                .or()
                                .like(DeliveryLink::getChannel,  keyword)
                                .or()
                                .like(DeliveryLink::getContent,  keyword))
                        .last("LIMIT 5"));
        for (DeliveryLink dl : linkList) {
            Map<String, String> item = new HashMap<>();
            item.put("name",    dl.getLinkName());
            item.put("channel", dl.getChannel());
            item.put("content", dl.getContent());
            item.put("status",  dl.getLinkStatus());
            links.add(item);
        }

        // ---- 4. 回传节点 ----
        List<Map<String, String>> nodes = new ArrayList<>();
        List<ConvNode> nodeList = convNodeMapper.selectList(
                new LambdaQueryWrapper<ConvNode>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(ConvNode::getNodeName,      keyword)
                                .or()
                                .like(ConvNode::getEventCategory, keyword)
                                .or()
                                .like(ConvNode::getEventDesc,     keyword))
                        .last("LIMIT 5"));
        for (ConvNode n : nodeList) {
            Map<String, String> item = new HashMap<>();
            item.put("name", n.getNodeName());
            item.put("type", n.getEventCategory());
            item.put("desc", n.getEventDesc());
            nodes.add(item);
        }

        // ---- 组装结果 ----
        Map<String, Object> result = new HashMap<>();
        result.put("type",       "search_result_card");
        result.put("keyword",    keyword);
        result.put("params",     params);
        result.put("strategies", strategies);
        result.put("links",      links);
        result.put("nodes",      nodes);

        return Result.success(result);
    }
}
