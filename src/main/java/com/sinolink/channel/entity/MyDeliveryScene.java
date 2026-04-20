package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("my_delivery_scene")
@Schema(description = "我的投放场景")
public class MyDeliveryScene {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    @Schema(description = "拼接后完整渠道地址")
    private String fullUrl;

    @Schema(description = "短链地址")
    private String shortUrl;

    @Schema(description = "短链失效时间")
    private LocalDateTime shortUrlExpireAt;

    @Schema(description = "短链状态")
    private String linkStatus;

    @Schema(description = "投放参数JSON")
    private String paramsJson;

    @Schema(description = "创建人")
    private String creatorName;

    @Schema(description = "申请人")
    private String applicant;

    @TableField("`status`")
    @Schema(description = "状态: 0=草稿 1=已提交 2=已归档")
    private Integer status;

    @Schema(description = "审核结果: 0=待审核 1=已通过 2=已驳回")
    private Integer auditResult;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "驳回备注")
    private String auditRemark;

    @Schema(description = "当前处理人")
    private String handler;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
