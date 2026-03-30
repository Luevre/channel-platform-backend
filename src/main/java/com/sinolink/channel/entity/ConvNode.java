package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 回传节点实体
 */
@Data
@TableName("conv_node")
@Schema(description = "回传节点")
public class ConvNode {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
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

    @Schema(description = "创建人ID")
    private Long creatorId;

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
