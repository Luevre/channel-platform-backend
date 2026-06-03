package com.sinolink.channel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "回传策略详情（含节点列表）")
public class ConvStrategyVO {

    private Long id;
    private String strategyName;
    private String channel;
    private String channelCode;
    private String bizNo;
    private String strategyRule;
    private Integer callbackRatio;
    private String adInfo;
    private String strategyParams;
    private String monitorUrl;
    private String description;
    private String applicant;
    private Integer auditStatus;
    private String auditRemark;
    private String nodeOverview;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "关联节点列表（有序）")
    private List<NodeItem> nodes;

    @Data
    public static class NodeItem {
        private Long nodeId;
        private String nodeName;
        private Integer convType;
        private String eventCategory;
        private String eventDesc;
        private String convValue;
        private String adParam;
        private Integer sortOrder;
    }
}
