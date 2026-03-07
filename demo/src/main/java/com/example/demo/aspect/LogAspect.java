package com.example.demo.aspect;

import com.example.demo.common.result.Result;
import com.example.demo.entity.SysOperationLog;
import com.example.demo.mapper.SysOperationLogMapper;
import com.example.demo.util.RequestAuthUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysOperationLogMapper logMapper;

    @Pointcut("execution(* com.example.demo.controller.IchProjectController.*(..))")
    public void projectLog() {
    }

    @AfterReturning(pointcut = "projectLog()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            String action = resolveAction(joinPoint.getSignature().getName());
            if (action == null || !isSuccessfulResult(result)) {
                return;
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            Long userId = RequestAuthUtil.getCurrentUserId(request);
            if (userId == null || userId <= 0) {
                return;
            }

            SysOperationLog log = new SysOperationLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setTargetType("IchProject");
            log.setIp(request.getRemoteAddr());
            log.setCreatedAt(LocalDateTime.now());
            log.setDetail("method=" + joinPoint.getSignature().getName());
            logMapper.insert(log);
        } catch (Exception ignored) {
        }
    }

    private boolean isSuccessfulResult(Object result) {
        if (!(result instanceof Result)) {
            return false;
        }

        Result<?> apiResult = (Result<?>) result;
        if (!Integer.valueOf(200).equals(apiResult.getCode())) {
            return false;
        }

        Object data = apiResult.getData();
        return !(data instanceof Boolean) || Boolean.TRUE.equals(data);
    }

    private String resolveAction(String methodName) {
        if (methodName == null) {
            return null;
        }
        if (methodName.startsWith("add")) {
            return "ADD_PROJECT";
        }
        if (methodName.startsWith("update")) {
            return "UPDATE_PROJECT";
        }
        if (methodName.startsWith("delete")) {
            return "DELETE_PROJECT";
        }
        if (methodName.startsWith("audit")) {
            return "AUDIT_PROJECT";
        }
        return null;
    }
}