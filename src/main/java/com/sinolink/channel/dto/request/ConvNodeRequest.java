package com.sinolink.channel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "新增/编辑回传节点请求")
public class ConvNodeRequest {

    @NotBlank(message = "节点名不能为空")
    @Schema(description = "节点名", example = "APP下载")
    private String nodeName;

    @NotNull(message = "convType不能为空")
    @Schema(description = "convType值", example = "0")
    private Integer convType;

    @NotBlank(message = "节点事件分类不能为空")
    @Schema(description = "节点事件分类", example = "下载开户节点")
    private String eventCategory;

    @Schema(description = "节点事件说明")
    private String eventDesc;

    @Schema(description = "节点事件判断", example = "客户端finder")
    private String eventJudge;
}
