package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinolink.channel.common.exception.BusinessException;
import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.DeliveryLinkQueryRequest;
import com.sinolink.channel.dto.request.DeliveryLinkRequest;
import com.sinolink.channel.dto.response.DeliveryLinkVO;
import com.sinolink.channel.dto.response.DeliveryPlatformVO;
import com.sinolink.channel.entity.*;
import com.sinolink.channel.mapper.*;
import com.sinolink.channel.service.DeliveryLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryLinkServiceImpl implements DeliveryLinkService {

    private final DeliveryLinkMapper deliveryLinkMapper;
    private final DeliveryContentMapper deliveryContentMapper;
    private final DeliveryPlatformMapper deliveryPlatformMapper;
    private final DeliveryPlatformPositionMapper deliveryPlatformPositionMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 投放链接 ====================

    @Override
    public PageResult<DeliveryLink> page(DeliveryLinkQueryRequest query) {
        LambdaQueryWrapper<DeliveryLink> wrapper = new LambdaQueryWrapper<DeliveryLink>()
                .like(StringUtils.hasText(query.getLinkName()), DeliveryLink::getLinkName, query.getLinkName())
                .eq(StringUtils.hasText(query.getChannel()), DeliveryLink::getChannel, query.getChannel())
                .eq(StringUtils.hasText(query.getContent()), DeliveryLink::getContent, query.getContent())
                .eq(StringUtils.hasText(query.getPromoMode()), DeliveryLink::getPromoMode, query.getPromoMode())
                .like(StringUtils.hasText(query.getCreatorName()), DeliveryLink::getCreatorName, query.getCreatorName())
                .ge(StringUtils.hasText(query.getStartTime()), DeliveryLink::getCreatedAt, parseTime(query.getStartTime()))
                .le(StringUtils.hasText(query.getEndTime()), DeliveryLink::getCreatedAt, parseTime(query.getEndTime()))
                .orderByDesc(DeliveryLink::getCreatedAt);

        Page<DeliveryLink> page = deliveryLinkMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public DeliveryLink getById(Long id) {
        DeliveryLink link = deliveryLinkMapper.selectById(id);
        if (link == null) throw new BusinessException("投放链接不存在");
        return link;
    }

    @Override
    @Transactional
    public DeliveryLinkVO create(DeliveryLinkRequest request) {
        String paramsJson = toJson(request.getParams());
        String fullUrl = buildFullUrl(request.getOriginUrl(), request.getParams());
        String shortUrl = generateShortUrl();

        DeliveryLink link = new DeliveryLink();
        link.setLinkName(request.getLinkName());
        link.setChannel(request.getChannel());
        link.setContent(request.getContent());
        link.setPromoMode(request.getPromoMode());
        link.setOriginUrl(request.getOriginUrl());
        link.setFullUrl(fullUrl);
        link.setShortUrl(shortUrl);
        link.setLinkStatus("生效中");
        link.setParamsJson(paramsJson);
        link.setCreatorName(getCurrentUsername());
        deliveryLinkMapper.insert(link);

        DeliveryLinkVO vo = new DeliveryLinkVO();
        vo.setId(link.getId());
        vo.setFullUrl(fullUrl);
        vo.setShortUrl(shortUrl);
        return vo;
    }

    @Override
    @Transactional
    public void update(Long id, DeliveryLinkRequest request) {
        DeliveryLink link = getById(id);
        link.setLinkName(request.getLinkName());
        link.setChannel(request.getChannel());
        link.setContent(request.getContent());
        link.setPromoMode(request.getPromoMode());
        link.setOriginUrl(request.getOriginUrl());
        link.setFullUrl(buildFullUrl(request.getOriginUrl(), request.getParams()));
        link.setParamsJson(toJson(request.getParams()));
        deliveryLinkMapper.updateById(link);
    }

    @Override
    public void archive(Long id) {
        getById(id);
        deliveryLinkMapper.deleteById(id);
    }

    // ==================== 投放内容 ====================

    @Override
    public List<DeliveryContent> listContents() {
        return deliveryContentMapper.selectList(
                new LambdaQueryWrapper<DeliveryContent>().orderByAsc(DeliveryContent::getId));
    }

    @Override
    public void createContent(String name) {
        DeliveryContent c = new DeliveryContent();
        c.setName(name);
        deliveryContentMapper.insert(c);
    }

    @Override
    public void updateContent(Long id, String name) {
        DeliveryContent c = deliveryContentMapper.selectById(id);
        if (c == null) throw new BusinessException("投放内容不存在");
        c.setName(name);
        deliveryContentMapper.updateById(c);
    }

    @Override
    public void deleteContent(Long id) {
        deliveryContentMapper.deleteById(id);
    }

    // ==================== 投放平台 ====================

    @Override
    public List<DeliveryPlatformVO> listPlatforms() {
        List<DeliveryPlatform> platforms = deliveryPlatformMapper.selectList(
                new LambdaQueryWrapper<DeliveryPlatform>().orderByAsc(DeliveryPlatform::getId));

        List<DeliveryPlatformPosition> allPositions = deliveryPlatformPositionMapper.selectList(null);
        Map<Long, List<DeliveryPlatformPosition>> posMap = allPositions.stream()
                .collect(Collectors.groupingBy(DeliveryPlatformPosition::getPlatformId));

        return platforms.stream().map(p -> {
            DeliveryPlatformVO vo = new DeliveryPlatformVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            List<DeliveryPlatformVO.PositionItem> positions = posMap
                    .getOrDefault(p.getId(), List.of())
                    .stream().map(pos -> {
                        DeliveryPlatformVO.PositionItem item = new DeliveryPlatformVO.PositionItem();
                        item.setId(pos.getId());
                        item.setName(pos.getName());
                        return item;
                    }).collect(Collectors.toList());
            vo.setPositions(positions);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void createPlatform(String name) {
        DeliveryPlatform p = new DeliveryPlatform();
        p.setName(name);
        deliveryPlatformMapper.insert(p);
    }

    @Override
    public void updatePlatform(Long id, String name) {
        DeliveryPlatform p = deliveryPlatformMapper.selectById(id);
        if (p == null) throw new BusinessException("投放平台不存在");
        p.setName(name);
        deliveryPlatformMapper.updateById(p);
    }

    @Override
    public void deletePlatform(Long id) {
        deliveryPlatformMapper.deleteById(id);
        deliveryPlatformPositionMapper.delete(
                new LambdaQueryWrapper<DeliveryPlatformPosition>()
                        .eq(DeliveryPlatformPosition::getPlatformId, id));
    }

    @Override
    public void createPosition(Long platformId, String name) {
        DeliveryPlatformPosition pos = new DeliveryPlatformPosition();
        pos.setPlatformId(platformId);
        pos.setName(name);
        deliveryPlatformPositionMapper.insert(pos);
    }

    @Override
    public void updatePosition(Long id, String name) {
        DeliveryPlatformPosition pos = deliveryPlatformPositionMapper.selectById(id);
        if (pos == null) throw new BusinessException("平台位置不存在");
        pos.setName(name);
        deliveryPlatformPositionMapper.updateById(pos);
    }

    @Override
    public void deletePosition(Long id) {
        deliveryPlatformPositionMapper.deleteById(id);
    }

    // ==================== 私有方法 ====================

    private String buildFullUrl(String originUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) return originUrl;
        String query = params.entrySet().stream()
                .filter(e -> StringUtils.hasText(e.getValue()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return StringUtils.hasText(query) ? originUrl + "?" + query : originUrl;
    }

    private String generateShortUrl() {
        // 实际项目接入短链服务；此处生成占位短链
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
