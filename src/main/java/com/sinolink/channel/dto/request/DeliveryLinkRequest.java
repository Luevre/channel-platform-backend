package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "新增/编辑投放链接请求")
public class DeliveryLinkRequest {

    @NotBlank(message = "链接名称不能为空")
    @Schema(description = "链接名称")
    private String linkName;

    @NotBlank(message = "投放渠道不能为空")
    @Schema(description = "投放渠道")
    private String channel;

    @NotBlank(message = "投放内容不能为空")
    @Schema(description = "投放内容")
    private String content;

    @NotBlank(message = "推广模式不能为空")
    @Schema(description = "推广模式")
    private String promoMode;

    @NotBlank(message = "原始投放链接不能为空")
    @Schema(description = "原始投放链接")
    private String originUrl;

    @Schema(description = "投放参数 key-value，key为参数名，value为参数值")
    private Map<String, String> params;
}
