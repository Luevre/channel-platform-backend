package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "回传节点查询请求")
public class ConvNodeQueryRequest {

    @Schema(description = "节点名称（模糊搜索）")
    private String nodeName;

    @Schema(description = "convType值精确匹配")
    private Integer convType;

    @Schema(description = "节点事件分类")
    private String eventCategory;

    @Schema(description = "当前页码", defaultValue = "1", example = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", defaultValue = "20", example = "20")
    private Integer size = 20;
}
