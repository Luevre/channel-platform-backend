package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sinolink.channel.common.exception.BusinessException;
import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.MyConvNodeAuditRequest;
import com.sinolink.channel.dto.request.MyConvNodeQueryRequest;
import com.sinolink.channel.dto.request.MyConvNodeRequest;
import com.sinolink.channel.entity.ConvNode;
import com.sinolink.channel.entity.MyConvNode;
import com.sinolink.channel.mapper.ConvNodeMapper;
import com.sinolink.channel.mapper.MyConvNodeMapper;
import com.sinolink.channel.service.MyConvNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MyConvNodeServiceImpl implements MyConvNodeService {

    private final MyConvNodeMapper myConvNodeMapper;
    private final ConvNodeMapper convNodeMapper;

    @Override
    public PageResult<MyConvNode> page(MyConvNodeQueryRequest query) {
        LambdaQueryWrapper<MyConvNode> wrapper = new LambdaQueryWrapper<MyConvNode>()
                .like(StringUtils.hasText(query.getNodeName()),
                        MyConvNode::getNodeName, query.getNodeName())
                .eq(query.getConvType() != null,
                        MyConvNode::getConvType, query.getConvType())
                .eq(StringUtils.hasText(query.getEventCategory()),
                        MyConvNode::getEventCategory, query.getEventCategory())
                .eq(query.getAuditResult() != null,
                        MyConvNode::getAuditResult, query.getAuditResult())
                .eq(query.getStatus() != null,
                        MyConvNode::getStatus, query.getStatus())
                .orderByDesc(MyConvNode::getCreatedAt);

        Page<MyConvNode> page = myConvNodeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper
        );
        return PageResult.of(page);
    }

    @Override
    public MyConvNode getById(Long id) {
        MyConvNode node = myConvNodeMapper.selectById(id);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }
        return node;
    }

    @Override
    public void create(MyConvNodeRequest request) {
        MyConvNode node = new MyConvNode();
        node.setNodeName(request.getNodeName());
        node.setConvType(request.getConvType());
        node.setEventCategory(request.getEventCategory());
        node.setEventDesc(request.getEventDesc());
        node.setEventJudge(request.getEventJudge());
        node.setApplicant(getCurrentUsername());
        node.setStatus(0);
        node.setAuditResult(0);
        myConvNodeMapper.insert(node);
    }

    @Override
    public void update(Long id, MyConvNodeRequest request) {
        MyConvNode node = getById(id);
        if (node.getStatus() != 0) {
            throw new BusinessException("只有草稿状态的节点可以编辑");
        }
        node.setNodeName(request.getNodeName());
        node.setConvType(request.getConvType());
        node.setEventCategory(request.getEventCategory());
        node.setEventDesc(request.getEventDesc());
        node.setEventJudge(request.getEventJudge());
        myConvNodeMapper.updateById(node);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        myConvNodeMapper.deleteById(id);
    }

    @Override
    public void submit(Long id) {
        MyConvNode node = getById(id);
        if (node.getStatus() != 0) {
            throw new BusinessException("只有草稿状态的节点可以提交");
        }
        node.setStatus(1);
        node.setAuditResult(0);
        node.setSubmittedAt(LocalDateTime.now());
        myConvNodeMapper.updateById(node);
    }

    @Override
    public void archive(Long id) {
        MyConvNode node = getById(id);
        node.setStatus(2);
        myConvNodeMapper.updateById(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(MyConvNodeAuditRequest request, String auditor) {
        MyConvNode node = getById(request.getId());

        node.setAuditResult(request.getAuditResult());
        node.setAuditor(auditor);
        node.setAuditTime(LocalDateTime.now());
        node.setAuditRemark(request.getAuditRemark());
        myConvNodeMapper.updateById(node);

        // 审核通过 → 写入正式回传节点表
        if (request.getAuditResult() == 1) {
            ConvNode convNode = new ConvNode();
            convNode.setNodeName(node.getNodeName());
            convNode.setConvType(node.getConvType());
            convNode.setEventCategory(node.getEventCategory());
            convNode.setEventDesc(node.getEventDesc());
            convNode.setEventJudge(node.getEventJudge());
            convNode.setCreatorName(node.getApplicant());
            convNodeMapper.insert(convNode);
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
