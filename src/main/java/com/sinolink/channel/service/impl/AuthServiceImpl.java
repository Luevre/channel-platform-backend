package com.sinolink.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sinolink.channel.common.exception.BusinessException;
import com.sinolink.channel.common.result.ResultCode;
import com.sinolink.channel.dto.request.LoginRequest;
import com.sinolink.channel.dto.response.LoginResponse;
import com.sinolink.channel.entity.SysUser;
import com.sinolink.channel.mapper.SysUserMapper;
import com.sinolink.channel.service.AuthService;
import com.sinolink.channel.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        SysUser user = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
        );

        String token = jwtUtil.generateToken(
            user.getUsername(),
            Map.of("role", user.getRole(), "userId", user.getId())
        );

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .expiresAt(System.currentTimeMillis() + jwtExpiration)
                .build();
    }
}
