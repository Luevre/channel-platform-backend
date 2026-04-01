package com.sinolink.channel.service;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.MyParameterAuditRequest;
import com.sinolink.channel.dto.request.MyParameterQueryRequest;
import com.sinolink.channel.dto.request.MyParameterRequest;
import com.sinolink.channel.entity.MyParameter;

public interface MyParameterService {

    PageResult<MyParameter> page(MyParameterQueryRequest query);

    MyParameter getById(Long id);

    void create(MyParameterRequest request);

    void update(Long id, MyParameterRequest request);

    void delete(Long id);

    void submit(Long id);

    void archive(Long id);

    void audit(MyParameterAuditRequest request, String auditor);
}
