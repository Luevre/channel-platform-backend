package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("my_parameter")
@Schema(description = "我的参数")
public class MyParameter {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "参数应用场景")
    private String scene;

    @TableField("`value`")
    @Schema(description = "参数值")
    private String value;

    @Schema(description = "参数名")
    private String parameterName;

    @TableField("`usage`")
    @Schema(description = "参数使用说明")
    private String usage;

    @Schema(description = "申请人")
    private String applicant;

    @TableField("`status`")
    @Schema(description = "状态: 0=草稿 1=已提交 2=已归档")
    private Integer status;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ========== 审核相关字段 ==========

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "驳回备注")
    private String auditRemark;

    @Schema(description = "审核结果: 0=待审核 1=已通过 2=已驳回")
    private Integer auditResult;

    @Schema(description = "当前处理人")
    private String handler;
}