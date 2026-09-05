package com.erp.auth;

import com.erp.common.UnauthorizedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录态拦截：除 /api/auth/** 外，所有 /api/** 必须携带有效 JWT。
 *
 * <p>校验通过后把 userId / username 写入 request attribute，
 * Controller 用 @RequestAttribute 直接取用。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USERNAME = "username";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new UnauthorizedException("请先登录");
        }
        Claims claims = jwtUtil.parse(auth.substring(7).trim());
        if (claims == null) {
            throw new UnauthorizedException("登录已过期，请重新登录");
        }
        request.setAttribute(ATTR_USER_ID, claims.get("uid", Integer.class));
        request.setAttribute(ATTR_USERNAME, claims.getSubject());
        return true;
    }
}
