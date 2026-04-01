package com.sinolink.channel.service;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.ConvStrategyQueryRequest;
import com.sinolink.channel.dto.request.ConvStrategyRequest;
import com.sinolink.channel.dto.response.ConvStrategyVO;
import com.sinolink.channel.entity.ConvStrategy;

public interface ConvStrategyService {

    PageResult<ConvStrategy> page(ConvStrategyQueryRequest query);

    ConvStrategyVO getById(Long id);

    void create(ConvStrategyRequest request);

    void update(Long id, ConvStrategyRequest request);

    void delete(Long id);

    void archive(Long id);
}
