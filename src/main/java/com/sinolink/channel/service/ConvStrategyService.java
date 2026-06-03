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

    /** 审核列表（待审核） */
    PageResult<ConvStrategy> listReview(ConvStrategyQueryRequest query);

    /** 审核详情 */
    ConvStrategyVO getReviewById(Long id);

    /** 审核通过 */
    void passReview(Long id);

    /** 审核驳回 */
    void rejectReview(Long id, String auditRemark);
}
