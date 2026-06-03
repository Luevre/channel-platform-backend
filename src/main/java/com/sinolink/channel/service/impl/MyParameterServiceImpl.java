package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sinolink.channel.common.exception.BusinessException;
import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.MyParameterAuditRequest;
import com.sinolink.channel.dto.request.MyParameterQueryRequest;
import com.sinolink.channel.dto.request.MyParameterRequest;
import com.sinolink.channel.entity.MyParameter;
import com.sinolink.channel.mapper.MyParameterMapper;
import com.sinolink.channel.service.MyParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MyParameterServiceImpl implements MyParameterService {

    private final MyParameterMapper myParameterMapper;

    @Override
    public PageResult<MyParameter> page(MyParameterQueryRequest query) {
        LambdaQueryWrapper<MyParameter> wrapper = new LambdaQueryWrapper<MyParameter>()
                .like(StringUtils.hasText(query.getScene()),
                        MyParameter::getScene, query.getScene())
                .like(StringUtils.hasText(query.getValue()),
                        MyParameter::getValue, query.getValue())
                .eq(StringUtils.hasText(query.getParameterName()),
                        MyParameter::getParameterName, query.getParameterName())
                .eq(query.getStatus() != null,
                        MyParameter::getStatus, query.getStatus())
                .orderByDesc(MyParameter::getCreatedAt);

        Page<MyParameter> page = myParameterMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper
        );
        return PageResult.of(page);
    }

    @Override
    public MyParameter getById(Long id) {
        MyParameter param = myParameterMapper.selectById(id);
        if (param == null) {
            throw new BusinessException("参数不存在");
        }
        return param;
    }

    @Override
    public void create(MyParameterRequest request) {
        MyParameter param = new MyParameter();
        param.setScene(request.getScene());
        param.setValue(request.getValue() != null ? request.getValue() : "");
        param.setParameterName(request.getParameterName());
        param.setUsage(request.getUsage());
        param.setApplicant(getCurrentUsername());
        param.setStatus(0);
        // 新参数申请走审核流程，已有参数录入直接通过
        if ("existing".equals(request.getApplyType())) {
            param.setAuditResult(1); // 已通过
        } else {
            param.setAuditResult(0); // 待审核
        }
        myParameterMapper.insert(param);
    }

    @Override
    public void update(Long id, MyParameterRequest request) {
        MyParameter param = getById(id);
        if (param.getStatus() != 0) {
            throw new BusinessException("只有草稿状态的参数可以编辑");
        }
        param.setScene(request.getScene());
        param.setValue(request.getValue());
        param.setParameterName(request.getParameterName());
        param.setUsage(request.getUsage());
        myParameterMapper.updateById(param);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        myParameterMapper.deleteById(id);
    }

    @Override
    public void submit(Long id) {
        MyParameter param = getById(id);
        if (param.getStatus() != 0) {
            throw new BusinessException("只有草稿状态的参数可以提交");
        }
        param.setStatus(1);
        param.setAuditResult(0); // 提交后重置为待审核
        param.setSubmittedAt(LocalDateTime.now());
        myParameterMapper.updateById(param);
    }

    @Override
    public void archive(Long id) {
        MyParameter param = getById(id);
        param.setStatus(2);
        myParameterMapper.updateById(param);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(MyParameterAuditRequest request, String auditor) {
        MyParameter param = getById(request.getId());

        // 更新审核结果（独立字段，不影响 status）
        // auditResult: 0=待审核 1=已通过 2=已驳回
        param.setAuditResult(request.getAuditResult());
        param.setAuditor(auditor);
        param.setAuditTime(LocalDateTime.now());
        param.setAuditRemark(request.getAuditRemark());
        myParameterMapper.updateById(param);

        // 审核通过仅更新 my_parameter 自身状态，不同步写入 placement_param
        // 投放参数管理（placement_param）由管理员在"投放参数配置"页面单独维护
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}