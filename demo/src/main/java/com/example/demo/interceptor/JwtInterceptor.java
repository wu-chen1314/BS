package com.example.demo.interceptor;

import com.example.demo.util.JwtUtil;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取 Token
        String token = request.getHeader("Authorization");
        
        // 如果没有 Token，尝试从参数中获取
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }

        // 去掉 Bearer 前缀
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证 Token
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            // Token 有效，将用户名存入请求属性
            String username = jwtUtil.getUsernameFromToken(token);
            request.setAttribute("username", username);
            return true;
        }

        // Token 无效或不存在
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\": 401, \"msg\": \"未授权，请先登录\"}");
        return false;
    }
}
