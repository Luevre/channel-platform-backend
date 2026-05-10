package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "我的参数审核请求")
public class MyParameterAuditRequest {

    @Schema(description = "申请单ID", required = true)
    private Long id;

    @Schema(description = "审核结果: 0=待审核 1=已通过 2=已驳回", required = true)
    private Integer auditResult;

    @Schema(description = "驳回备注（驳回时必填）")
    private String auditRemark;
}