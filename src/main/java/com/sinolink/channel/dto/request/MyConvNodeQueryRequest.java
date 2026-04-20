package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "我的回传节点查询请求")
public class MyConvNodeQueryRequest {

    @Schema(description = "节点名称（模糊）")
    private String nodeName;

    @Schema(description = "convType值（精确）")
    private Integer convType;

    @Schema(description = "节点事件分类")
    private String eventCategory;

    @Schema(description = "审核结果")
    private Integer auditResult;

    @Schema(description = "状态: 0=草稿 1=已提交 2=已归档")
    private Integer status;

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer size = 10;
}
