package com.example.demo.util;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

@Component
public class RequestAuthUtil {

    public static final String CLAIMS_ATTRIBUTE = "jwtClaims";
    public static final String USER_ID_ATTRIBUTE = "currentUserId";
    public static final String USERNAME_ATTRIBUTE = "username";
    public static final String ROLE_ATTRIBUTE = "currentUserRole";

    @Autowired
    private JwtUtil jwtUtil;

    public Claims getClaims(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Object cachedClaims = request.getAttribute(CLAIMS_ATTRIBUTE);
        if (cachedClaims instanceof Claims) {
            return (Claims) cachedClaims;
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }

        Claims claims = jwtUtil.getClaimsFromToken(token);
        if (claims != null) {
            cacheClaims(request, claims);
        }
        return claims;
    }

    public Long getCurrentUserId(HttpServletRequest request) {
        Object cachedUserId = request == null ? null : request.getAttribute(USER_ID_ATTRIBUTE);
        if (cachedUserId instanceof Number) {
            return ((Number) cachedUserId).longValue();
        }

        Claims claims = getClaims(request);
        if (claims == null) {
            return null;
        }

        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        if (userId != null) {
            try {
                return Long.parseLong(userId.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public String getCurrentUsername(HttpServletRequest request) {
        Object cachedUsername = request == null ? null : request.getAttribute(USERNAME_ATTRIBUTE);
        if (cachedUsername instanceof String && StringUtils.hasText((String) cachedUsername)) {
            return (String) cachedUsername;
        }

        Claims claims = getClaims(request);
        if (claims == null) {
            return null;
        }
        Object username = claims.get("username");
        return username == null ? claims.getSubject() : username.toString();
    }

    public String getCurrentRole(HttpServletRequest request) {
        Object cachedRole = request == null ? null : request.getAttribute(ROLE_ATTRIBUTE);
        if (cachedRole instanceof String && StringUtils.hasText((String) cachedRole)) {
            return (String) cachedRole;
        }

        Claims claims = getClaims(request);
        if (claims == null) {
            return null;
        }
        Object role = claims.get("role");
        return role == null ? null : role.toString();
    }

    public boolean isAdmin(HttpServletRequest request) {
        return "admin".equals(getCurrentRole(request));
    }

    public boolean isCurrentUser(HttpServletRequest request, Long targetUserId) {
        Long currentUserId = getCurrentUserId(request);
        return currentUserId != null && currentUserId.equals(targetUserId);
    }

    public String extractToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }

        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return StringUtils.hasText(token) ? token : null;
    }

    public void cacheClaims(HttpServletRequest request, Claims claims) {
        if (request == null || claims == null) {
            return;
        }

        request.setAttribute(CLAIMS_ATTRIBUTE, claims);

        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            request.setAttribute(USER_ID_ATTRIBUTE, ((Number) userId).longValue());
        } else if (userId != null) {
            try {
                request.setAttribute(USER_ID_ATTRIBUTE, Long.parseLong(userId.toString()));
            } catch (NumberFormatException ignored) {
                // Ignore invalid userId claims and leave the attribute unset.
            }
        }

        Object username = claims.get("username");
        request.setAttribute(USERNAME_ATTRIBUTE, username == null ? claims.getSubject() : username.toString());

        Object role = claims.get("role");
        if (role != null) {
            request.setAttribute(ROLE_ATTRIBUTE, role.toString());
        }
    }
}
