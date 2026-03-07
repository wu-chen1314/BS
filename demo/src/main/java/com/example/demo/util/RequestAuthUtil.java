package com.example.demo.util;

import javax.servlet.http.HttpServletRequest;

public final class RequestAuthUtil {

    private RequestAuthUtil() {
    }

    public static Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        if (userId instanceof String) {
            try {
                return Long.parseLong((String) userId);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static String getCurrentRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role == null ? null : String.valueOf(role);
    }

    public static String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        return username == null ? null : String.valueOf(username);
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return "admin".equalsIgnoreCase(getCurrentRole(request));
    }

    public static boolean isSelf(HttpServletRequest request, Long targetUserId) {
        Long currentUserId = getCurrentUserId(request);
        return currentUserId != null && currentUserId.equals(targetUserId);
    }

    public static boolean isSelfOrAdmin(HttpServletRequest request, Long targetUserId) {
        return isAdmin(request) || isSelf(request, targetUserId);
    }
}
