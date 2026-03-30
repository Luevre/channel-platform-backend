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

    private Long creatorId;
    private String creatorName;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
