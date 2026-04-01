package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "回传策略查询请求")
public class ConvStrategyQueryRequest {

    @Schema(description = "策略名称（模糊）")
    private String strategyName;

    @Schema(description = "应用渠道（模糊）")
    private String channel;

    @Schema(description = "策略规则精确匹配")
    private String strategyRule;

    @Schema(description = "创建人（模糊）")
    private String creatorName;

    @Schema(description = "提交时间起始（yyyy-MM-dd HH:mm:ss）")
    private String startTime;

    @Schema(description = "提交时间截止（yyyy-MM-dd HH:mm:ss）")
    private String endTime;

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private Integer size = 20;
}
