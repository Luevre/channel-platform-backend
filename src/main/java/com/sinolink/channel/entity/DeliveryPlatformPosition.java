package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("delivery_platform_position")
@Schema(description = "投放平台位置")
public class DeliveryPlatformPosition {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "平台ID")
    private Long platformId;

    @Schema(description = "位置名称")
    private String name;

    @TableLogic
    private Integer deleted;
}
