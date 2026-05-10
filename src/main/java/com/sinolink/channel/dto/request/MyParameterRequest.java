package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "新增/编辑我的参数请求")
public class MyParameterRequest {

    @NotBlank(message = "参数应用场景不能为空")
    @Schema(description = "参数应用场景", example = "抖音下载链路")
    private String scene;

    @NotBlank(message = "参数值不能为空")
    @Schema(description = "参数值", example = "2400000000000")
    private String value;

    @NotBlank(message = "参数名不能为空")
    @Schema(description = "参数名", example = "channel_type | 渠道编码")
    private String parameterName;

    @Schema(description = "参数使用说明")
    private String usage;

    @Schema(description = "申请类型: new=新参数申请, existing=已有参数录入")
    private String applyType;
}
