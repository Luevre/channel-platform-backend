package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "新增/编辑投放参数请求")
public class PlacementParamRequest {

    @NotBlank(message = "参数名称不能为空")
    @Schema(description = "参数名称")
    private String paramName;

    @NotBlank(message = "参数Key不能为空")
    @Schema(description = "参数Key")
    private String paramKey;

    @Schema(description = "参数Value")
    private String paramValue;

    @Schema(description = "渠道代码: honor/kuaishou/douyin/wechat")
    private String channelCode;

    @Schema(description = "状态: 1=启用 0=禁用", defaultValue = "1")
    private Integer status = 1;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "参数分类")
    private String category;

    @Schema(description = "参数应用说明")
    private String paramUsage;

    @Schema(description = "参数管理员")
    private String manager;
}
