package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sinolink.channel.common.exception.BusinessException;
import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.PlacementParamAuditRequest;
import com.sinolink.channel.dto.request.PlacementParamQueryRequest;
import com.sinolink.channel.dto.request.PlacementParamRequest;
import com.sinolink.channel.entity.PlacementParam;
import com.sinolink.channel.mapper.PlacementParamMapper;
import com.sinolink.channel.service.PlacementParamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlacementParamServiceImpl implements PlacementParamService {

    private final PlacementParamMapper placementParamMapper;

    @Override
    public PageResult<PlacementParam> page(PlacementParamQueryRequest query) {
        LambdaQueryWrapper<PlacementParam> wrapper = new LambdaQueryWrapper<PlacementParam>()
                .like(StringUtils.hasText(query.getParamName()),
                        PlacementParam::getParamName, query.getParamName())
                .eq(StringUtils.hasText(query.getChannelCode()),
                        PlacementParam::getChannelCode, query.getChannelCode())
                .eq(query.getStatus() != null,
                        PlacementParam::getStatus, query.getStatus())
                .orderByDesc(PlacementParam::getUpdatedAt);

        Page<PlacementParam> page = placementParamMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper
        );
        return PageResult.of(page);
    }

    @Override
    public PlacementParam getById(Long id) {
        PlacementParam param = placementParamMapper.selectById(id);
        if (param == null) {
            throw new BusinessException("投放参数不存在");
        }
        return param;
    }

    @Override
    public void create(PlacementParamRequest request) {
        PlacementParam param = new PlacementParam();
        param.setParamName(request.getParamName());
        param.setParamKey(request.getParamKey());
        param.setParamValue(request.getParamValue());
        param.setChannelCode(request.getChannelCode());
        param.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        param.setRemark(request.getRemark());
        param.setCategory(request.getCategory());
        param.setParamUsage(request.getParamUsage());
        param.setManager(request.getManager());
        param.setCreatorName(getCurrentUsername());
        placementParamMapper.insert(param);
    }

    @Override
    public void update(Long id, PlacementParamRequest request) {
        PlacementParam param = getById(id);
        param.setParamName(request.getParamName());
        param.setParamKey(request.getParamKey());
        param.setParamValue(request.getParamValue());
        param.setChannelCode(request.getChannelCode());
        param.setStatus(request.getStatus());
        param.setRemark(request.getRemark());
        param.setCategory(request.getCategory());
        param.setParamUsage(request.getParamUsage());
        param.setManager(request.getManager());
        placementParamMapper.updateById(param);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        placementParamMapper.deleteById(id);
    }

    @Override
    public void audit(PlacementParamAuditRequest request, String auditor) {
        PlacementParam param = new PlacementParam();
        param.setId(request.getId());
        param.setAuditStatus(request.getAuditStatus());
        param.setAuditor(auditor);
        param.setAuditTime(LocalDateTime.now());
        param.setAuditRemark(request.getAuditRemark());
        placementParamMapper.updateById(param);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}