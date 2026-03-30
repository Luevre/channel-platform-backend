package com.sinolink.channel.controller;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.dto.request.PlacementParamQueryRequest;
import com.sinolink.channel.dto.request.PlacementParamRequest;
import com.sinolink.channel.entity.PlacementParam;
import com.sinolink.channel.service.PlacementParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "投放参数管理", description = "投放参数 CRUD 接口")
@RestController
@RequestMapping("/channel/placement-params")
@RequiredArgsConstructor
public class PlacementParamController {

    private final PlacementParamService placementParamService;

    @Operation(summary = "分页查询投放参数")
    @GetMapping
    public Result<PageResult<PlacementParam>> page(PlacementParamQueryRequest query) {
        return Result.success(placementParamService.page(query));
    }

    @Operation(summary = "查询单个投放参数")
    @GetMapping("/{id}")
    public Result<PlacementParam> getById(@PathVariable Long id) {
        return Result.success(placementParamService.getById(id));
    }

    @Operation(summary = "新增投放参数")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PlacementParamRequest request) {
        placementParamService.create(request);
        return Result.success();
    }

    @Operation(summary = "编辑投放参数")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "参数ID") @PathVariable Long id,
            @Valid @RequestBody PlacementParamRequest request) {
        placementParamService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除投放参数")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        placementParamService.delete(id);
        return Result.success();
    }
}
