package com.sinolink.channel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "投放平台（含位置列表）")
public class DeliveryPlatformVO {

    private Long id;
    private String name;

    @Schema(description = "平台位置列表")
    private List<PositionItem> positions;

    @Data
    public static class PositionItem {
        private Long id;
        private String name;
    }
}
