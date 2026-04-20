package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "我的投放场景新增/编辑请求")
public class MyDeliverySceneRequest {

    @NotBlank(message = "链接名称不能为空")
    @Schema(description = "链接名称")
    private String linkName;

    @Schema(description = "投放渠道")
    private String channel;

    @Schema(description = "投放内容")
    private String content;

    @Schema(description = "推广模式")
    private String promoMode;

    @Schema(description = "原始投放链接")
    private String originUrl;

    @Schema(description = "投放参数")
    private Map<String, String> params;
}
