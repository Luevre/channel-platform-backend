package com.sinolink.channel.controller;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.dto.request.ConvNodeQueryRequest;
import com.sinolink.channel.dto.request.ConvNodeRequest;
import com.sinolink.channel.entity.ConvNode;
import com.sinolink.channel.service.ConvNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "回传节点管理", description = "回传节点 CRUD 接口")
@RestController
@RequestMapping("/channel/conv-nodes")
@RequiredArgsConstructor
public class ConvNodeController {

    private final ConvNodeService convNodeService;

    @Operation(summary = "分页查询回传节点")
    @GetMapping
    public Result<PageResult<ConvNode>> page(ConvNodeQueryRequest query) {
        return Result.success(convNodeService.page(query));
    }

    @Operation(summary = "查询单个回传节点")
    @GetMapping("/{id}")
    public Result<ConvNode> getById(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        return Result.success(convNodeService.getById(id));
    }

    @Operation(summary = "新增回传节点")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ConvNodeRequest request) {
        convNodeService.create(request);
        return Result.success();
    }

    @Operation(summary = "编辑回传节点")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "节点ID") @PathVariable Long id,
            @Valid @RequestBody ConvNodeRequest request) {
        convNodeService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除回传节点")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        convNodeService.delete(id);
        return Result.success();
    }

    @Operation(summary = "归档回传节点", description = "将节点标记为归档状态")
    @PutMapping("/{id}/archive")
    public Result<Void> archive(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        convNodeService.archive(id);
        return Result.success();
    }
}
