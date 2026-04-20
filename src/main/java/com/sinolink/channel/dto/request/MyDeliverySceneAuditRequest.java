package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "我的投放场景审核请求")
public class MyDeliverySceneAuditRequest {

    @NotNull(message = "场景ID不能为空")
    @Schema(description = "场景ID")
    private Long id;

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果: 1=通过 2=驳回")
    private Integer auditResult;

    @Schema(description = "驳回备注")
    private String auditRemark;
}
