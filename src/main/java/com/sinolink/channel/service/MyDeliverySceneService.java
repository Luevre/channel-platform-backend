package com.sinolink.channel.service;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.dto.request.MyDeliverySceneAuditRequest;
import com.sinolink.channel.dto.request.MyDeliverySceneQueryRequest;
import com.sinolink.channel.dto.request.MyDeliverySceneRequest;
import com.sinolink.channel.entity.MyDeliveryScene;

public interface MyDeliverySceneService {

    PageResult<MyDeliveryScene> page(MyDeliverySceneQueryRequest query);

    MyDeliveryScene getById(Long id);

    void create(MyDeliverySceneRequest request);

    void update(Long id, MyDeliverySceneRequest request);

    void delete(Long id);

    void submit(Long id);

    void archive(Long id);

    void audit(MyDeliverySceneAuditRequest request, String auditor);
}
