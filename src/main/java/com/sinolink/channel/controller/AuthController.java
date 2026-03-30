package com.sinolink.channel.controller;

import com.sinolink.channel.common.result.Result;
import com.sinolink.channel.dto.request.LoginRequest;
import com.sinolink.channel.dto.response.LoginResponse;
import com.sinolink.channel.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "登录/登出接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录", description = "返回 JWT Token，后续请求在 Header 中携带 Authorization: Bearer {token}")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "退出登录", description = "客户端清除本地 Token 即可（无状态）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，退出登录由前端清除 Token 处理
        // 如需服务端 Token 黑名单，可扩展 Redis 实现
        return Result.success();
    }
}
