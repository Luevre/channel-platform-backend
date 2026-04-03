package com.sinolink.channel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "投放链接创建结果（含生成的链接地址）")
public class DeliveryLinkVO {

    private Long id;

    @Schema(description = "拼接后完整渠道地址")
    private String fullUrl;

    @Schema(description = "短链地址")
    private String shortUrl;
}
