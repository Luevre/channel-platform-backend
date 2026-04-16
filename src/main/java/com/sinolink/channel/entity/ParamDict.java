package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("param_dict")
@Schema(description = "参数字典")
public class ParamDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联投放参数ID")
    private Long placementParamId;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "对应tabKey")
    private String tabKey;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
