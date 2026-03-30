package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sinolink.channel.common.exception.BusinessException;
import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.ConvNodeQueryRequest;
import com.sinolink.channel.dto.request.ConvNodeRequest;
import com.sinolink.channel.entity.ConvNode;
import com.sinolink.channel.mapper.ConvNodeMapper;
import com.sinolink.channel.service.ConvNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConvNodeServiceImpl implements ConvNodeService {

    private final ConvNodeMapper convNodeMapper;

    @Override
    public PageResult<ConvNode> page(ConvNodeQueryRequest query) {
        LambdaQueryWrapper<ConvNode> wrapper = new LambdaQueryWrapper<ConvNode>()
            .like(StringUtils.hasText(query.getNodeName()),
                  ConvNode::getNodeName, query.getNodeName())
            .eq(query.getConvType() != null,
                ConvNode::getConvType, query.getConvType())
            .eq(StringUtils.hasText(query.getEventCategory()),
                ConvNode::getEventCategory, query.getEventCategory())
            .orderByAsc(ConvNode::getConvType);

        Page<ConvNode> page = convNodeMapper.selectPage(
            new Page<>(query.getCurrent(), query.getSize()), wrapper
        );
        return PageResult.of(page);
    }

    @Override
    public ConvNode getById(Long id) {
        ConvNode node = convNodeMapper.selectById(id);
        if (node == null) {
            throw new BusinessException("回传节点不存在");
        }
        return node;
    }

    @Override
    public void create(ConvNodeRequest request) {
        // 校验 convType 唯一性
        Long count = convNodeMapper.selectCount(
            new LambdaQueryWrapper<ConvNode>()
                .eq(ConvNode::getConvType, request.getConvType())
        );
        if (count > 0) {
            throw new BusinessException("convType值 " + request.getConvType() + " 已存在");
        }

        ConvNode node = new ConvNode();
        node.setNodeName(request.getNodeName());
        node.setConvType(request.getConvType());
        node.setEventCategory(request.getEventCategory());
        node.setEventDesc(request.getEventDesc());
        node.setEventJudge(request.getEventJudge());
        node.setCreatorName(getCurrentUsername());

        convNodeMapper.insert(node);
    }

    @Override
    public void update(Long id, ConvNodeRequest request) {
        ConvNode existing = getById(id);

        // 如果 convType 有变更，校验新值唯一性
        if (!existing.getConvType().equals(request.getConvType())) {
            Long count = convNodeMapper.selectCount(
                new LambdaQueryWrapper<ConvNode>()
                    .eq(ConvNode::getConvType, request.getConvType())
                    .ne(ConvNode::getId, id)
            );
            if (count > 0) {
                throw new BusinessException("convType值 " + request.getConvType() + " 已被其他节点使用");
            }
        }

        existing.setNodeName(request.getNodeName());
        existing.setConvType(request.getConvType());
        existing.setEventCategory(request.getEventCategory());
        existing.setEventDesc(request.getEventDesc());
        existing.setEventJudge(request.getEventJudge());

        convNodeMapper.updateById(existing);
    }

    @Override
    public void delete(Long id) {
        getById(id); // 确认存在
        convNodeMapper.deleteById(id); // 逻辑删除（MyBatis-Plus 自动处理）
    }

    @Override
    public void archive(Long id) {
        // 归档逻辑：可扩展为写入归档表，当前先标记删除
        delete(id);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
