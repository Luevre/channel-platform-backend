package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sinolink.channel.entity.ParamDict;
import com.sinolink.channel.mapper.ParamDictMapper;
import com.sinolink.channel.service.ParamDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParamDictServiceImpl implements ParamDictService {

    private final ParamDictMapper paramDictMapper;

    @Override
    public List<ParamDict> listByPlacementParamId(Long placementParamId) {
        LambdaQueryWrapper<ParamDict> wrapper = new LambdaQueryWrapper<ParamDict>()
                .eq(ParamDict::getPlacementParamId, placementParamId)
                .orderByAsc(ParamDict::getId);
        return paramDictMapper.selectList(wrapper);
    }
}
