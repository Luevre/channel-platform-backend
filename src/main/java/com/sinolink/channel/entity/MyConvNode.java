package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("my_conv_node")
@Schema(description = "我的回传节点")
public class MyConvNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "节点名")
    private String nodeName;

    @Schema(description = "convType值")
    private Integer convType;

    @Schema(description = "节点事件分类")
    private String eventCategory;

    @Schema(description = "节点事件说明")
    private String eventDesc;

    @Schema(description = "节点事件判断")
    private String eventJudge;

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
