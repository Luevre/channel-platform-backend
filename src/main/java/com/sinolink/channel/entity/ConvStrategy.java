package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conv_strategy")
@Schema(description = "回传策略")
public class ConvStrategy {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "策略名称")
    private String strategyName;

    @Schema(description = "应用渠道显示名（如 抖音 douyin/cule）")
    private String channel;

    @Schema(description = "渠道代码（如 douyin）")
    private String channelCode;

    @Schema(description = "业务编号")
    private String bizNo;

    @Schema(description = "策略规则")
    private String strategyRule;

    @Schema(description = "回传比例（0-100）")
    private Integer callbackRatio;

    @Schema(description = "广告商")
    private String adInfo;

    @Schema(description = "策略参数字符串")
    private String strategyParams;

    @Schema(description = "监测链接")
    private String monitorUrl;

    @Schema(description = "创建人姓名")
    private String creatorName;

    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "最近操作时间")
    private LocalDateTime updatedAt;
}
