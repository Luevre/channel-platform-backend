package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "我的回传节点新增/编辑请求")
public class MyConvNodeRequest {

    @NotBlank(message = "节点名不能为空")
    @Schema(description = "节点名")
    private String nodeName;

    @NotNull(message = "convType值不能为空")
    @Schema(description = "convType值")
    private Integer convType;

    @Schema(description = "节点事件分类")
    private String eventCategory;

    @Schema(description = "节点事件说明")
    private String eventDesc;

    @Schema(description = "节点事件判断")
    private String eventJudge;
}
