package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "投放参数审核请求")
public class PlacementParamAuditRequest {

    @Schema(description = "参数ID", required = true)
    private Long id;

    @Schema(description = "审核结果: 1=通过 2=驳回", required = true)
    private Integer auditStatus;

    @Schema(description = "驳回备注（驳回时必填）")
    private String auditRemark;
}
