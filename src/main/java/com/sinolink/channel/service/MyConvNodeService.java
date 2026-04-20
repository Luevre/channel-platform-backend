package com.sinolink.channel.service;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.MyConvNodeAuditRequest;
import com.sinolink.channel.dto.request.MyConvNodeQueryRequest;
import com.sinolink.channel.dto.request.MyConvNodeRequest;
import com.sinolink.channel.entity.MyConvNode;

public interface MyConvNodeService {

    PageResult<MyConvNode> page(MyConvNodeQueryRequest query);

    MyConvNode getById(Long id);

    void create(MyConvNodeRequest request);

    void update(Long id, MyConvNodeRequest request);

    void delete(Long id);

    void submit(Long id);

    void archive(Long id);

    void audit(MyConvNodeAuditRequest request, String auditor);
}
