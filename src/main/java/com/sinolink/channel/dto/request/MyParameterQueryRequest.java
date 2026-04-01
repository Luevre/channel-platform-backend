package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "我的参数查询请求")
public class MyParameterQueryRequest {

    @Schema(description = "参数应用场景（模糊）")
    private String scene;

    @Schema(description = "参数值（模糊）")
    private String value;

    @Schema(description = "参数名（精确）")
    private String parameterName;

    @Schema(description = "状态: 0=草稿 1=已提交 2=已归档")
    private Integer status;

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private Integer size = 20;
}
