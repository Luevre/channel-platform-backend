package com.sinolink.channel.controller;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.dto.request.MyDeliverySceneAuditRequest;
import com.sinolink.channel.dto.request.MyDeliverySceneQueryRequest;
import com.sinolink.channel.dto.request.MyDeliverySceneRequest;
import com.sinolink.channel.entity.MyDeliveryScene;
import com.sinolink.channel.service.MyDeliverySceneService;
import com.sinolink.channel.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "我的投放场景管理", description = "用户自定义投放场景 CRUD + 审核接口")
@RestController
@RequestMapping("/delivery/my-scenes")
@RequiredArgsConstructor
public class MyDeliverySceneController {

    private final MyDeliverySceneService myDeliverySceneService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "分页查询我的投放场景")
    @GetMapping
    public Result<PageResult<MyDeliveryScene>> page(MyDeliverySceneQueryRequest query) {
        return Result.success(myDeliverySceneService.page(query));
    }

    @Operation(summary = "查询单个投放场景")
    @GetMapping("/{id}")
    public Result<MyDeliveryScene> getById(
            @Parameter(description = "场景ID") @PathVariable Long id) {
        return Result.success(myDeliverySceneService.getById(id));
    }

    @Operation(summary = "新增投放场景")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MyDeliverySceneRequest request) {
        myDeliverySceneService.create(request);
        return Result.success();
    }

    @Operation(summary = "编辑投放场景")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "场景ID") @PathVariable Long id,
            @Valid @RequestBody MyDeliverySceneRequest request) {
        myDeliverySceneService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除投放场景")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "场景ID") @PathVariable Long id) {
        myDeliverySceneService.delete(id);
        return Result.success();
    }

    @Operation(summary = "提交投放场景", description = "将草稿状态的场景提交审核")
    @PostMapping("/{id}/submit")
    public Result<Void> submit(
            @Parameter(description = "场景ID") @PathVariable Long id) {
        myDeliverySceneService.submit(id);
        return Result.success();
    }

    @Operation(summary = "归档投放场景")
    @PutMapping("/{id}/archive")
    public Result<Void> archive(
            @Parameter(description = "场景ID") @PathVariable Long id) {
        myDeliverySceneService.archive(id);
        return Result.success();
    }

    @Operation(summary = "审核投放场景", description = "审核员对提交的场景进行通过或驳回")
    @PostMapping("/review/audit")
    public Result<Void> audit(
            @Valid @RequestBody MyDeliverySceneAuditRequest request,
            HttpServletRequest httpRequest) {
        String auditor = null;
        String authHeader = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            auditor = jwtUtil.getUsernameFromToken(token);
        }
        myDeliverySceneService.audit(request, auditor);
        return Result.success();
    }
}
