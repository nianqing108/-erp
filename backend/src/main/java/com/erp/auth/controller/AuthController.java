package com.erp.auth.controller;

import com.erp.auth.dto.LoginDTO;
import com.erp.auth.dto.RegisterDTO;
import com.erp.auth.dto.UserVO;
import com.erp.auth.service.AuthService;
import com.erp.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 注册 / 登录接口（拦截器放行路径）。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "注册", description = "服务端配置了注册邀请码时须携带 inviteCode")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "返回 JWT，后续请求经 Authorization: Bearer 携带")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        String token = authService.login(dto);
        return Result.success(Map.of("token", token));
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户")
    public Result<UserVO> me(@RequestAttribute("userId") Integer userId) {
        return Result.success(authService.currentUser(userId));
    }
}
