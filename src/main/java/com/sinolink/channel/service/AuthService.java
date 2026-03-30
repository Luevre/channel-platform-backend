package com.sinolink.channel.service;

import com.sinolink.channel.dto.request.LoginRequest;
import com.sinolink.channel.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
