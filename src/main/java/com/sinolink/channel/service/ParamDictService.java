package com.sinolink.channel.service;

import com.sinolink.channel.entity.ParamDict;

import java.util.List;

public interface ParamDictService {
    List<ParamDict> listByPlacementParamId(Long placementParamId);
}
