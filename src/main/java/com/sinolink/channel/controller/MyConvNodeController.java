package com.sinolink.channel.controller;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.dto.request.MyConvNodeAuditRequest;
import com.sinolink.channel.dto.request.MyConvNodeQueryRequest;
import com.sinolink.channel.dto.request.MyConvNodeRequest;
import com.sinolink.channel.entity.MyConvNode;
import com.sinolink.channel.service.MyConvNodeService;
import com.sinolink.channel.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "我的回传节点管理", description = "用户自定义回传节点 CRUD + 审核接口")
@RestController
@RequestMapping("/channel/my-conv-nodes")
@RequiredArgsConstructor
public class MyConvNodeController {

    private final MyConvNodeService myConvNodeService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "分页查询我的回传节点")
    @GetMapping
    public Result<PageResult<MyConvNode>> page(MyConvNodeQueryRequest query) {
        return Result.success(myConvNodeService.page(query));
    }

    @Operation(summary = "查询单个节点")
    @GetMapping("/{id}")
    public Result<MyConvNode> getById(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        return Result.success(myConvNodeService.getById(id));
    }

    @Operation(summary = "新增节点")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MyConvNodeRequest request) {
        myConvNodeService.create(request);
        return Result.success();
    }

    @Operation(summary = "编辑节点")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "节点ID") @PathVariable Long id,
            @Valid @RequestBody MyConvNodeRequest request) {
        myConvNodeService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除节点")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        myConvNodeService.delete(id);
        return Result.success();
    }

    @Operation(summary = "提交节点", description = "将草稿状态的节点提交审核")
    @PostMapping("/{id}/submit")
    public Result<Void> submit(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        myConvNodeService.submit(id);
        return Result.success();
    }

    @Operation(summary = "归档节点")
    @PutMapping("/{id}/archive")
    public Result<Void> archive(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        myConvNodeService.archive(id);
        return Result.success();
    }

    @Operation(summary = "审核节点", description = "审核员对提交的节点进行通过或驳回")
    @PostMapping("/review/audit")
    public Result<Void> audit(
            @Valid @RequestBody MyConvNodeAuditRequest request,
            HttpServletRequest httpRequest) {
        String auditor = null;
        String authHeader = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            auditor = jwtUtil.getUsernameFromToken(token);
        }
        myConvNodeService.audit(request, auditor);
        return Result.success();
    }
}
