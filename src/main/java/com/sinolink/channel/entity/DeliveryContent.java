package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("delivery_content")
@Schema(description = "投放内容")
public class DeliveryContent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "投放内容名称")
    private String name;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
