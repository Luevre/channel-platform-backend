package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sinolink.channel.common.exception.BusinessException;
import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.ConvStrategyQueryRequest;
import com.sinolink.channel.dto.request.ConvStrategyRequest;
import com.sinolink.channel.dto.response.ConvStrategyVO;
import com.sinolink.channel.entity.ConvNode;
import com.sinolink.channel.entity.ConvStrategy;
import com.sinolink.channel.entity.ConvStrategyNode;
import com.sinolink.channel.mapper.ConvNodeMapper;
import com.sinolink.channel.mapper.ConvStrategyMapper;
import com.sinolink.channel.mapper.ConvStrategyNodeMapper;
import com.sinolink.channel.service.ConvStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConvStrategyServiceImpl implements ConvStrategyService {

    private final ConvStrategyMapper convStrategyMapper;
    private final ConvStrategyNodeMapper convStrategyNodeMapper;
    private final ConvNodeMapper convNodeMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<ConvStrategy> page(ConvStrategyQueryRequest query) {
        LambdaQueryWrapper<ConvStrategy> wrapper = new LambdaQueryWrapper<ConvStrategy>()
                .like(StringUtils.hasText(query.getStrategyName()),
                        ConvStrategy::getStrategyName, query.getStrategyName())
                .like(StringUtils.hasText(query.getChannel()),
                        ConvStrategy::getChannel, query.getChannel())
                .eq(StringUtils.hasText(query.getStrategyRule()),
                        ConvStrategy::getStrategyRule, query.getStrategyRule())
                .like(StringUtils.hasText(query.getCreatorName()),
                        ConvStrategy::getCreatorName, query.getCreatorName())
                .ge(StringUtils.hasText(query.getStartTime()),
                        ConvStrategy::getCreatedAt, parseTime(query.getStartTime()))
                .le(StringUtils.hasText(query.getEndTime()),
                        ConvStrategy::getCreatedAt, parseTime(query.getEndTime()))
                .orderByDesc(ConvStrategy::getCreatedAt);

        Page<ConvStrategy> page = convStrategyMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public ConvStrategyVO getById(Long id) {
        ConvStrategy strategy = convStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new BusinessException("回传策略不存在");
        }

        // 查询关联节点
        List<ConvStrategyNode> strategyNodes = convStrategyNodeMapper.selectList(
                new LambdaQueryWrapper<ConvStrategyNode>()
                        .eq(ConvStrategyNode::getStrategyId, id)
                        .orderByAsc(ConvStrategyNode::getSortOrder));

        List<ConvStrategyVO.NodeItem> nodeItems = Collections.emptyList();
        if (!strategyNodes.isEmpty()) {
            List<Long> nodeIds = strategyNodes.stream()
                    .map(ConvStrategyNode::getNodeId).collect(Collectors.toList());
            Map<Long, ConvNode> nodeMap = convNodeMapper.selectBatchIds(nodeIds).stream()
                    .collect(Collectors.toMap(ConvNode::getId, n -> n));

            nodeItems = strategyNodes.stream().map(sn -> {
                ConvStrategyVO.NodeItem item = new ConvStrategyVO.NodeItem();
                item.setNodeId(sn.getNodeId());
                item.setAdParam(sn.getAdParam());
                item.setSortOrder(sn.getSortOrder());
                ConvNode node = nodeMap.get(sn.getNodeId());
                if (node != null) {
                    item.setNodeName(node.getNodeName());
                    item.setConvType(node.getConvType());
                    item.setEventDesc(node.getEventDesc());
                    item.setEventJudge(node.getEventJudge());
                }
                return item;
            }).collect(Collectors.toList());
        }

        ConvStrategyVO vo = new ConvStrategyVO();
        vo.setId(strategy.getId());
        vo.setStrategyName(strategy.getStrategyName());
        vo.setChannel(strategy.getChannel());
        vo.setChannelCode(strategy.getChannelCode());
        vo.setBizNo(strategy.getBizNo());
        vo.setStrategyRule(strategy.getStrategyRule());
        vo.setCallbackRatio(strategy.getCallbackRatio());
        vo.setAdInfo(strategy.getAdInfo());
        vo.setStrategyParams(strategy.getStrategyParams());
        vo.setMonitorUrl(strategy.getMonitorUrl());
        vo.setCreatorName(strategy.getCreatorName());
        vo.setCreatedAt(strategy.getCreatedAt());
        vo.setUpdatedAt(strategy.getUpdatedAt());
        vo.setNodes(nodeItems);
        return vo;
    }

    @Override
    @Transactional
    public void create(ConvStrategyRequest request) {
        ConvStrategy strategy = new ConvStrategy();
        fillStrategy(strategy, request);
        strategy.setCreatorName(getCurrentUsername());
        convStrategyMapper.insert(strategy);
        saveNodes(strategy.getId(), request);
        updateNodeOverview(strategy.getId(), request);
    }

    @Override
    @Transactional
    public void update(Long id, ConvStrategyRequest request) {
        ConvStrategy strategy = convStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new BusinessException("回传策略不存在");
        }
        fillStrategy(strategy, request);
        convStrategyMapper.updateById(strategy);

        convStrategyNodeMapper.delete(
                new LambdaQueryWrapper<ConvStrategyNode>()
                        .eq(ConvStrategyNode::getStrategyId, id));
        saveNodes(id, request);
        updateNodeOverview(id, request);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ConvStrategy strategy = convStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new BusinessException("回传策略不存在");
        }
        convStrategyMapper.deleteById(id);
        convStrategyNodeMapper.delete(
                new LambdaQueryWrapper<ConvStrategyNode>()
                        .eq(ConvStrategyNode::getStrategyId, id));
    }

    @Override
    public void archive(Long id) {
        delete(id);
    }

    // ---------- 私有方法 ----------

    private void fillStrategy(ConvStrategy strategy, ConvStrategyRequest request) {
        strategy.setStrategyName(request.getStrategyName());
        strategy.setChannel(request.getChannel());
        strategy.setChannelCode(request.getChannelCode());
        strategy.setBizNo(request.getBizNo());
        strategy.setStrategyRule(request.getStrategyRule());
        strategy.setCallbackRatio(request.getCallbackRatio());
        strategy.setAdInfo(request.getAdInfo());
        strategy.setStrategyParams(request.getStrategyParams());
        strategy.setMonitorUrl(request.getMonitorUrl());
    }

    private void saveNodes(Long strategyId, ConvStrategyRequest request) {
        if (request.getNodes() == null || request.getNodes().isEmpty()) return;
        for (int i = 0; i < request.getNodes().size(); i++) {
            ConvStrategyRequest.NodeItem item = request.getNodes().get(i);
            ConvStrategyNode sn = new ConvStrategyNode();
            sn.setStrategyId(strategyId);
            sn.setNodeId(item.getNodeId());
            sn.setAdParam(item.getAdParam());
            sn.setSortOrder(i);
            convStrategyNodeMapper.insert(sn);
        }
    }

    private void updateNodeOverview(Long strategyId, ConvStrategyRequest request) {
        if (request.getNodes() == null || request.getNodes().isEmpty()) return;
        List<Long> nodeIds = request.getNodes().stream()
                .map(ConvStrategyRequest.NodeItem::getNodeId)
                .collect(Collectors.toList());
        List<ConvNode> nodes = convNodeMapper.selectBatchIds(nodeIds);
        Map<Long, String> nameMap = nodes.stream()
                .collect(Collectors.toMap(ConvNode::getId, ConvNode::getNodeName));
        String overview = request.getNodes().stream()
                .map(n -> nameMap.getOrDefault(n.getNodeId(), ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("-"));
        ConvStrategy strategy = convStrategyMapper.selectById(strategyId);
        strategy.setNodeOverview(overview);
        convStrategyMapper.updateById(strategy);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private LocalDateTime parseTime(String s) {
        if (!StringUtils.hasText(s)) return null;
        return LocalDateTime.parse(s, DTF);
    }
}
