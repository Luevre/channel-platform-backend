package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "投放链接查询请求")
public class DeliveryLinkQueryRequest {

    @Schema(description = "链接名称（模糊）")
    private String linkName;

    @Schema(description = "投放渠道")
    private String channel;

    @Schema(description = "投放内容")
    private String content;

    @Schema(description = "推广模式")
    private String promoMode;

    @Schema(description = "创建人（模糊）")
    private String creatorName;

    @Schema(description = "提交时间起始")
    private String startTime;

    @Schema(description = "提交时间截止")
    private String endTime;

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private Integer size = 20;
}
