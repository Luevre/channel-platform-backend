package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("placement_param")
@Schema(description = "投放参数")
public class PlacementParam {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "参数名称")
    private String paramName;

    @Schema(description = "参数Key")
    private String paramKey;

    @Schema(description = "参数Value")
    private String paramValue;

    @Schema(description = "渠道代码: honor/kuaishou/douyin/wechat")
    private String channelCode;

    @Schema(description = "状态: 1=启用 0=禁用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "参数分类（场景参数/人员参数/活动参数）")
    private String category;

    @Schema(description = "参数应用说明")
    private String paramUsage;

    @Schema(description = "参数管理员")
    private String manager;

    private Long creatorId;
    private String creatorName;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ========== 审核相关字段 ==========

    @Schema(description = "审核状态: 0=待审核 1=已审核 2=驳回")
    private Integer auditStatus;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "驳回备注")
    private String auditRemark;
}
