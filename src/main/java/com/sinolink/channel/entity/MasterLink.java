package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("master_link")
public class MasterLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private String baseUrl;

    private String productCategory;

    private String description;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
