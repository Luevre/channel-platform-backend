package com.sinolink.channel.controller;

import com.sinolink.channel.common.result.PageResult;
import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.dto.request.MyParameterAuditRequest;
import com.sinolink.channel.dto.request.MyParameterQueryRequest;
import com.sinolink.channel.dto.request.MyParameterRequest;
import com.sinolink.channel.entity.MyParameter;
import com.sinolink.channel.entity.PlacementParam;
import com.sinolink.channel.mapper.PlacementParamMapper;
import com.sinolink.channel.service.MyParameterService;
import com.sinolink.channel.util.JwtUtil;
//import com.sinolink.channel.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "我的参数管理", description = "用户自定义参数 CRUD 接口")
@RestController
@RequestMapping("/channel/my-params")
@RequiredArgsConstructor
public class MyParameterController {

    private final MyParameterService myParameterService;
    private final JwtUtil jwtUtil;
    private final PlacementParamMapper placementParamMapper;

    @Operation(summary = "分页查询我的参数")
    @GetMapping
    public Result<PageResult<MyParameter>> page(MyParameterQueryRequest query) {
        return Result.success(myParameterService.page(query));
    }

    @Operation(summary = "查询单个参数")
    @GetMapping("/{id}")
    public Result<MyParameter> getById(
            @Parameter(description = "参数ID") @PathVariable Long id) {
        return Result.success(myParameterService.getById(id));
    }

    @Operation(summary = "新增参数")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MyParameterRequest request) {
        myParameterService.create(request);
        return Result.success();
    }

    @Operation(summary = "编辑参数")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "参数ID") @PathVariable Long id,
            @Valid @RequestBody MyParameterRequest request) {
        myParameterService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除参数")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "参数ID") @PathVariable Long id) {
        myParameterService.delete(id);
        return Result.success();
    }

    @Operation(summary = "提交参数", description = "将草稿状态的参数提交审核")
    @PostMapping("/{id}/submit")
    public Result<Void> submit(
            @Parameter(description = "参数ID") @PathVariable Long id) {
        myParameterService.submit(id);
        return Result.success();
    }

    @Operation(summary = "归档参数")
    @PutMapping("/{id}/archive")
    public Result<Void> archive(
            @Parameter(description = "参数ID") @PathVariable Long id) {
        myParameterService.archive(id);
        return Result.success();
    }

    @Operation(summary = "审核参数", description = "审核员对提交的参数进行通过或驳回")
    @PostMapping("/review/audit")
    public Result<Void> audit(
            @Valid @RequestBody MyParameterAuditRequest request,
            HttpServletRequest httpRequest) {

        // 从 JWT token 解析当前审核人用户名
        String auditor = null;
        String authHeader = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            auditor = jwtUtil.getUsernameFromToken(token);
        }

        myParameterService.audit(request, auditor);
        return Result.success();
    }

    @Operation(summary = "获取投放参数选项", description = "用于添加参数时的下拉选择")
    @GetMapping("/placement-options")
    public Result<List<PlacementParam>> getPlacementOptions() {
        List<PlacementParam> list = placementParamMapper.selectList(null);
        return Result.success(list);
    }
}
