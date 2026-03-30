package com.sinolink.channel.service;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.ConvNodeQueryRequest;
import com.sinolink.channel.dto.request.ConvNodeRequest;
import com.sinolink.channel.entity.ConvNode;

public interface ConvNodeService {

    PageResult<ConvNode> page(ConvNodeQueryRequest query);

    ConvNode getById(Long id);

    void create(ConvNodeRequest request);

    void update(Long id, ConvNodeRequest request);

    void delete(Long id);

    void archive(Long id);
}
