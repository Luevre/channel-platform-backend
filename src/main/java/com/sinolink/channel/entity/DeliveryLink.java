package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("delivery_link")
@Schema(description = "投放链接")
public class DeliveryLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "链接名称")
    private String linkName;

    @Schema(description = "投放渠道")
    private String channel;

    @Schema(description = "渠道代码（如 douyin / jingdong / txzxg）")
    private String channelCode;

    @Schema(description = "投放内容")
    private String content;

    @Schema(description = "推广模式")
    private String promoMode;

    @Schema(description = "原始投放链接")
    private String originUrl;

    @Schema(description = "拼接后完整渠道地址")
    private String fullUrl;

    @Schema(description = "短链地址")
    private String shortUrl;

    @Schema(description = "短链失效时间")
    private LocalDateTime shortUrlExpireAt;

    @Schema(description = "短链状态")
    private String linkStatus;

    @Schema(description = "投放参数JSON")
    private String paramsJson;

    @Schema(description = "创建人姓名")
    private String creatorName;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
