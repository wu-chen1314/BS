package com.example.demo.interceptor;

import com.example.demo.util.JwtUtil;
import com.example.demo.util.RequestAuthUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RequestAuthUtil requestAuthUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = requestAuthUtil.extractToken(request);
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            Claims claims = jwtUtil.getClaimsFromToken(token);
            if (claims != null) {
                requestAuthUtil.cacheClaims(request, claims);
                return true;
            }
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\": 401, \"msg\": \"未授权，请先登录\"}");
        return false;
    }
}
