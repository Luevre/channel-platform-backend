package com.sinolink.channel.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sinolink.channel.entity.SysUser;
import com.sinolink.channel.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getStatus, 1)
        );

        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在或已禁用: " + username);
        }

        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + sysUser.getRole())))
                .build();
    }
}
