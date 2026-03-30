package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "投放参数查询请求")
public class PlacementParamQueryRequest {

    @Schema(description = "参数名称（模糊）")
    private String paramName;

    @Schema(description = "渠道代码")
    private String channelCode;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private Integer size = 20;
}
