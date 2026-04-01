package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "新增/编辑回传策略请求")
public class ConvStrategyRequest {

    @NotBlank(message = "策略名称不能为空")
    @Schema(description = "策略名称")
    private String strategyName;

    @NotBlank(message = "应用渠道不能为空")
    @Schema(description = "应用渠道显示名")
    private String channel;

    @NotBlank(message = "渠道代码不能为空")
    @Schema(description = "渠道代码")
    private String channelCode;

    @Schema(description = "业务编号")
    private String bizNo;

    @NotBlank(message = "策略规则不能为空")
    @Schema(description = "策略规则")
    private String strategyRule;

    @NotNull(message = "回传比例不能为空")
    @Schema(description = "回传比例（0-100）")
    private Integer callbackRatio;

    @Schema(description = "广告商")
    private String adInfo;

    @Schema(description = "策略参数字符串")
    private String strategyParams;

    @Schema(description = "监测链接")
    private String monitorUrl;

    @Schema(description = "节点列表")
    private List<NodeItem> nodes;

    @Data
    public static class NodeItem {
        @NotNull(message = "节点ID不能为空")
        private Long nodeId;
        private String adParam;
    }
}
