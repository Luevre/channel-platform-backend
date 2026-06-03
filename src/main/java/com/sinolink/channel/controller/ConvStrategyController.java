package com.sinolink.channel.controller;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.dto.request.ConvStrategyAuditRequest;
import com.sinolink.channel.dto.request.ConvStrategyQueryRequest;
import com.sinolink.channel.dto.request.ConvStrategyRequest;
import com.sinolink.channel.dto.response.ConvStrategyVO;
import com.sinolink.channel.entity.ConvStrategy;
import com.sinolink.channel.service.ConvStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "回传策略管理", description = "回传策略 CRUD 及审核接口")
@RestController
@RequestMapping("/channel/conv-strategies")
@RequiredArgsConstructor
public class ConvStrategyController {

    private final ConvStrategyService convStrategyService;

    @Operation(summary = "分页查询回传策略")
    @GetMapping
    public Result<PageResult<ConvStrategy>> page(ConvStrategyQueryRequest query) {
        return Result.success(convStrategyService.page(query));
    }

    @Operation(summary = "查询单个回传策略（含节点）")
    @GetMapping("/{id}")
    public Result<ConvStrategyVO> getById(
            @Parameter(description = "策略ID") @PathVariable Long id) {
        return Result.success(convStrategyService.getById(id));
    }

    @Operation(summary = "新增回传策略")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ConvStrategyRequest request) {
        convStrategyService.create(request);
        return Result.success();
    }

    @Operation(summary = "编辑回传策略")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "策略ID") @PathVariable Long id,
            @Valid @RequestBody ConvStrategyRequest request) {
        convStrategyService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除回传策略")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "策略ID") @PathVariable Long id) {
        convStrategyService.delete(id);
        return Result.success();
    }

    @Operation(summary = "归档回传策略")
    @PutMapping("/{id}/archive")
    public Result<Void> archive(
            @Parameter(description = "策略ID") @PathVariable Long id) {
        convStrategyService.archive(id);
        return Result.success();
    }

    // ========== 审核接口 ==========

    @Operation(summary = "待审核列表")
    @GetMapping("/review/list")
    public Result<PageResult<ConvStrategy>> reviewList(ConvStrategyQueryRequest query) {
        return Result.success(convStrategyService.listReview(query));
    }

    @Operation(summary = "审核详情")
    @GetMapping("/review/{id}")
    public Result<ConvStrategyVO> reviewDetail(
            @Parameter(description = "策略ID") @PathVariable Long id) {
        return Result.success(convStrategyService.getReviewById(id));
    }

    @Operation(summary = "审核通过")
    @PostMapping("/review/{id}/pass")
    public Result<Void> passReview(
            @Parameter(description = "策略ID") @PathVariable Long id) {
        convStrategyService.passReview(id);
        return Result.success();
    }

    @Operation(summary = "审核驳回")
    @PostMapping("/review/{id}/reject")
    public Result<Void> rejectReview(
            @Parameter(description = "策略ID") @PathVariable Long id,
            @Valid @RequestBody ConvStrategyAuditRequest request) {
        convStrategyService.rejectReview(id, request.getAuditRemark());
        return Result.success();
    }
}
