package com.sinolink.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("conv_strategy_node")
@Schema(description = "回传策略节点关联")
public class ConvStrategyNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "策略ID")
    private Long strategyId;

    @Schema(description = "回传节点ID")
    private Long nodeId;

    @Schema(description = "节点顺序")
    private Integer sortOrder;

    @Schema(description = "广告商匹配参数")
    private String adParam;
}
