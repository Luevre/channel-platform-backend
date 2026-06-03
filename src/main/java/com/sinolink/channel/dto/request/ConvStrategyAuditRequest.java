package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "回传策略审核请求")
public class ConvStrategyAuditRequest {

    @Schema(description = "驳回原因（驳回时必填）")
    private String auditRemark;
}
