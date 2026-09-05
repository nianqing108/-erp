package com.erp.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.auth.JwtUtil;
import com.erp.auth.dto.LoginDTO;
import com.erp.auth.dto.RegisterDTO;
import com.erp.auth.dto.UserVO;
import com.erp.auth.entity.SysUser;
import com.erp.auth.mapper.SysUserMapper;
import com.erp.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 注册 / 登录 / 当前用户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;

    /** 生产建议配置注册邀请码（erp.auth.invite-code），为空则开放注册 */
    @Value("${erp.auth.invite-code:}")
    private String inviteCode;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserVO register(RegisterDTO dto) {
        // 服务端配置了邀请码则强制校验，防止公网环境被任意注册
        if (inviteCode != null && !inviteCode.isBlank()
                && !inviteCode.equals(trimOrNull(dto.getInviteCode()))) {
            throw new BusinessException("注册邀请码不正确");
        }
        String username = dto.getUsername().trim();
        if (sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username)) > 0) {
            throw new BusinessException("登录名「" + username + "」已被注册");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setRealName(trimOrNull(dto.getRealName()));
        sysUserMapper.insert(user);
        log.info("新用户注册：id={}, username={}", user.getId(), username);
        return toVO(user);
    }

    public String login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, dto.getUsername().trim()));
        // 统一提示，不暴露「账号存在与否」的区分信息
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("登录失败：username={}", dto.getUsername());
            throw new BusinessException("登录名或密码不正确");
        }
        return jwtUtil.issue(user.getId(), user.getUsername());
    }

    public UserVO currentUser(Integer userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在或已被删除");
        }
        return toVO(user);
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        return vo;
    }

    private String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
