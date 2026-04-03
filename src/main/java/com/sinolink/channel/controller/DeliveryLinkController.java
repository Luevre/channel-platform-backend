package com.sinolink.channel.controller;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.dto.request.DeliveryLinkQueryRequest;
import com.sinolink.channel.dto.request.DeliveryLinkRequest;
import com.sinolink.channel.dto.response.DeliveryLinkVO;
import com.sinolink.channel.dto.response.DeliveryPlatformVO;
import com.sinolink.channel.entity.DeliveryContent;
import com.sinolink.channel.entity.DeliveryLink;
import com.sinolink.channel.service.DeliveryLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "投放链接管理")
@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
public class DeliveryLinkController {

    private final DeliveryLinkService deliveryLinkService;

    // -------- 投放链接 --------

    @Operation(summary = "分页查询投放链接")
    @GetMapping("/links")
    public Result<PageResult<DeliveryLink>> page(DeliveryLinkQueryRequest query) {
        return Result.success(deliveryLinkService.page(query));
    }

    @Operation(summary = "查询单个投放链接")
    @GetMapping("/links/{id}")
    public Result<DeliveryLink> getById(@PathVariable Long id) {
        return Result.success(deliveryLinkService.getById(id));
    }

    @Operation(summary = "新增投放链接")
    @PostMapping("/links")
    public Result<DeliveryLinkVO> create(@Valid @RequestBody DeliveryLinkRequest request) {
        return Result.success(deliveryLinkService.create(request));
    }

    @Operation(summary = "编辑投放链接")
    @PutMapping("/links/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeliveryLinkRequest request) {
        deliveryLinkService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "归档投放链接")
    @PutMapping("/links/{id}/archive")
    public Result<Void> archive(@PathVariable Long id) {
        deliveryLinkService.archive(id);
        return Result.success();
    }

    // -------- 投放内容 --------

    @Operation(summary = "查询投放内容列表")
    @GetMapping("/contents")
    public Result<List<DeliveryContent>> listContents() {
        return Result.success(deliveryLinkService.listContents());
    }

    @Operation(summary = "新增投放内容")
    @PostMapping("/contents")
    public Result<Void> createContent(@RequestBody NameRequest req) {
        deliveryLinkService.createContent(req.getName());
        return Result.success();
    }

    @Operation(summary = "编辑投放内容")
    @PutMapping("/contents/{id}")
    public Result<Void> updateContent(@PathVariable Long id, @RequestBody NameRequest req) {
        deliveryLinkService.updateContent(id, req.getName());
        return Result.success();
    }

    @Operation(summary = "删除投放内容")
    @DeleteMapping("/contents/{id}")
    public Result<Void> deleteContent(@PathVariable Long id) {
        deliveryLinkService.deleteContent(id);
        return Result.success();
    }

    // -------- 投放平台 --------

    @Operation(summary = "查询投放平台列表（含位置）")
    @GetMapping("/platforms")
    public Result<List<DeliveryPlatformVO>> listPlatforms() {
        return Result.success(deliveryLinkService.listPlatforms());
    }

    @Operation(summary = "新增投放平台")
    @PostMapping("/platforms")
    public Result<Void> createPlatform(@RequestBody NameRequest req) {
        deliveryLinkService.createPlatform(req.getName());
        return Result.success();
    }

    @Operation(summary = "编辑投放平台")
    @PutMapping("/platforms/{id}")
    public Result<Void> updatePlatform(@PathVariable Long id, @RequestBody NameRequest req) {
        deliveryLinkService.updatePlatform(id, req.getName());
        return Result.success();
    }

    @Operation(summary = "删除投放平台")
    @DeleteMapping("/platforms/{id}")
    public Result<Void> deletePlatform(@PathVariable Long id) {
        deliveryLinkService.deletePlatform(id);
        return Result.success();
    }

    @Operation(summary = "新增平台位置")
    @PostMapping("/platforms/{platformId}/positions")
    public Result<Void> createPosition(@PathVariable Long platformId, @RequestBody NameRequest req) {
        deliveryLinkService.createPosition(platformId, req.getName());
        return Result.success();
    }

    @Operation(summary = "编辑平台位置")
    @PutMapping("/platforms/positions/{id}")
    public Result<Void> updatePosition(@PathVariable Long id, @RequestBody NameRequest req) {
        deliveryLinkService.updatePosition(id, req.getName());
        return Result.success();
    }

    @Operation(summary = "删除平台位置")
    @DeleteMapping("/platforms/positions/{id}")
    public Result<Void> deletePosition(@PathVariable Long id) {
        deliveryLinkService.deletePosition(id);
        return Result.success();
    }

    @Data
    static class NameRequest {
        private String name;
    }
}
