package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("delivery_platform")
@Schema(description = "投放平台")
public class DeliveryPlatform {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "平台名称")
    private String name;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
