package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinolink.channel.common.exception.BusinessException;
import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.MyDeliverySceneAuditRequest;
import com.sinolink.channel.dto.request.MyDeliverySceneQueryRequest;
import com.sinolink.channel.dto.request.MyDeliverySceneRequest;
import com.sinolink.channel.entity.DeliveryLink;
import com.sinolink.channel.entity.MyDeliveryScene;
import com.sinolink.channel.mapper.DeliveryLinkMapper;
import com.sinolink.channel.mapper.MyDeliverySceneMapper;
import com.sinolink.channel.service.MyDeliverySceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyDeliverySceneServiceImpl implements MyDeliverySceneService {

    private final MyDeliverySceneMapper myDeliverySceneMapper;
    private final DeliveryLinkMapper deliveryLinkMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<MyDeliveryScene> page(MyDeliverySceneQueryRequest query) {
        LambdaQueryWrapper<MyDeliveryScene> wrapper = new LambdaQueryWrapper<MyDeliveryScene>()
                .like(StringUtils.hasText(query.getLinkName()),
                        MyDeliveryScene::getLinkName, query.getLinkName())
                .eq(StringUtils.hasText(query.getChannel()),
                        MyDeliveryScene::getChannel, query.getChannel())
                .eq(StringUtils.hasText(query.getContent()),
                        MyDeliveryScene::getContent, query.getContent())
                .eq(StringUtils.hasText(query.getPromoMode()),
                        MyDeliveryScene::getPromoMode, query.getPromoMode())
                .like(StringUtils.hasText(query.getCreatorName()),
                        MyDeliveryScene::getCreatorName, query.getCreatorName())
                .ge(StringUtils.hasText(query.getStartTime()),
                        MyDeliveryScene::getCreatedAt, parseTime(query.getStartTime()))
                .le(StringUtils.hasText(query.getEndTime()),
                        MyDeliveryScene::getCreatedAt, parseTime(query.getEndTime()))
                .eq(query.getAuditResult() != null,
                        MyDeliveryScene::getAuditResult, query.getAuditResult())
                .eq(query.getStatus() != null,
                        MyDeliveryScene::getStatus, query.getStatus())
                .orderByDesc(MyDeliveryScene::getCreatedAt);

        Page<MyDeliveryScene> page = myDeliverySceneMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper
        );
        return PageResult.of(page);
    }

    @Override
    public MyDeliveryScene getById(Long id) {
        MyDeliveryScene scene = myDeliverySceneMapper.selectById(id);
        if (scene == null) {
            throw new BusinessException("投放场景不存在");
        }
        return scene;
    }

    @Override
    public void create(MyDeliverySceneRequest request) {
        String paramsJson = toJson(request.getParams());
        String fullUrl = buildFullUrl(request.getOriginUrl(), request.getParams());

        MyDeliveryScene scene = new MyDeliveryScene();
        scene.setLinkName(request.getLinkName());
        scene.setChannel(request.getChannel());
        scene.setContent(request.getContent());
        scene.setPromoMode(request.getPromoMode());
        scene.setOriginUrl(request.getOriginUrl());
        scene.setFullUrl(fullUrl);
        scene.setParamsJson(paramsJson);
        scene.setLinkStatus("未生效");
        scene.setCreatorName(getCurrentUsername());
        scene.setApplicant(getCurrentUsername());
        scene.setStatus(0);
        scene.setAuditResult(0);
        myDeliverySceneMapper.insert(scene);
    }

    @Override
    public void update(Long id, MyDeliverySceneRequest request) {
        MyDeliveryScene scene = getById(id);
        if (scene.getStatus() != 0) {
            throw new BusinessException("只有草稿状态的场景可以编辑");
        }
        scene.setLinkName(request.getLinkName());
        scene.setChannel(request.getChannel());
        scene.setContent(request.getContent());
        scene.setPromoMode(request.getPromoMode());
        scene.setOriginUrl(request.getOriginUrl());
        scene.setFullUrl(buildFullUrl(request.getOriginUrl(), request.getParams()));
        scene.setParamsJson(toJson(request.getParams()));
        myDeliverySceneMapper.updateById(scene);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        myDeliverySceneMapper.deleteById(id);
    }

    @Override
    public void submit(Long id) {
        MyDeliveryScene scene = getById(id);
        if (scene.getStatus() != 0) {
            throw new BusinessException("只有草稿状态的场景可以提交");
        }
        scene.setStatus(1);
        scene.setAuditResult(0);
        scene.setSubmittedAt(LocalDateTime.now());
        myDeliverySceneMapper.updateById(scene);
    }

    @Override
    public void archive(Long id) {
        MyDeliveryScene scene = getById(id);
        scene.setStatus(2);
        myDeliverySceneMapper.updateById(scene);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(MyDeliverySceneAuditRequest request, String auditor) {
        MyDeliveryScene scene = getById(request.getId());

        scene.setAuditResult(request.getAuditResult());
        scene.setAuditor(auditor);
        scene.setAuditTime(LocalDateTime.now());
        scene.setAuditRemark(request.getAuditRemark());
        myDeliverySceneMapper.updateById(scene);

        // 审核通过 → 写入正式投放链接表
        if (request.getAuditResult() == 1) {
            String shortUrl = generateShortUrl();

            DeliveryLink link = new DeliveryLink();
            link.setLinkName(scene.getLinkName());
            link.setChannel(scene.getChannel());
            link.setContent(scene.getContent());
            link.setPromoMode(scene.getPromoMode());
            link.setOriginUrl(scene.getOriginUrl());
            link.setFullUrl(scene.getFullUrl());
            link.setShortUrl(shortUrl);
            link.setLinkStatus("生效中");
            link.setParamsJson(scene.getParamsJson());
            link.setCreatorName(scene.getCreatorName());
            deliveryLinkMapper.insert(link);

            // 回写短链信息到场景表
            scene.setShortUrl(shortUrl);
            scene.setLinkStatus("生效中");
            myDeliverySceneMapper.updateById(scene);
        }
    }

    private String buildFullUrl(String originUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) return originUrl;
        String query = params.entrySet().stream()
                .filter(e -> StringUtils.hasText(e.getValue()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return StringUtils.hasText(query) ? originUrl + "?" + query : originUrl;
    }

    private String generateShortUrl() {
        String code = Long.toHexString(System.currentTimeMillis()).substring(4).toUpperCase();
        return "http://g.gjyjb.com/rd/" + code;
    }

    private String toJson(Map<String, String> params) {
        if (params == null || params.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private LocalDateTime parseTime(String s) {
        if (!StringUtils.hasText(s)) return null;
        return LocalDateTime.parse(s, DTF);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
