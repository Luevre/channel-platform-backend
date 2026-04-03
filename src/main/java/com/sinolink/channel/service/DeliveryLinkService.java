package com.sinolink.channel.service;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.DeliveryLinkQueryRequest;
import com.sinolink.channel.dto.request.DeliveryLinkRequest;
import com.sinolink.channel.dto.response.DeliveryLinkVO;
import com.sinolink.channel.dto.response.DeliveryPlatformVO;
import com.sinolink.channel.entity.DeliveryContent;
import com.sinolink.channel.entity.DeliveryLink;

import java.util.List;

public interface DeliveryLinkService {

    // 投放链接
    PageResult<DeliveryLink> page(DeliveryLinkQueryRequest query);
    DeliveryLink getById(Long id);
    DeliveryLinkVO create(DeliveryLinkRequest request);
    void update(Long id, DeliveryLinkRequest request);
    void archive(Long id);

    // 投放内容
    List<DeliveryContent> listContents();
    void createContent(String name);
    void updateContent(Long id, String name);
    void deleteContent(Long id);

    // 投放平台
    List<DeliveryPlatformVO> listPlatforms();
    void createPlatform(String name);
    void updatePlatform(Long id, String name);
    void deletePlatform(Long id);
    void createPosition(Long platformId, String name);
    void updatePosition(Long id, String name);
    void deletePosition(Long id);
}
