package com.sinolink.channel.service;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.PlacementParamQueryRequest;
import com.sinolink.channel.dto.request.PlacementParamRequest;
import com.sinolink.channel.entity.PlacementParam;

public interface PlacementParamService {
    PageResult<PlacementParam> page(PlacementParamQueryRequest query);
    PlacementParam getById(Long id);
    void create(PlacementParamRequest request);
    void update(Long id, PlacementParamRequest request);
    void delete(Long id);
}
