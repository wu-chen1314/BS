package com.example.demo.aop;

import com.example.demo.entity.SysOperationLog;
import com.example.demo.mapper.SysOperationLogMapper;
import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Autowired
    private SysOperationLogMapper logMapper;

    @Pointcut("execution(* com.example.demo.controller.IchProjectController.*(..))")
    public void projectLog() {
    }

    @Around("projectLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 1. 执行目标方法
        Object result = joinPoint.proceed();
        
        long timeTaken = System.currentTimeMillis() - startTime;
        String methodName = joinPoint.getSignature().getName();
        
        if (timeTaken > 500) {
            log.warn("⚠️ Slow API Request detected. Method: [{}], Time taken: {}ms", methodName, timeTaken);
        } else {
            log.info("API Method: [{}], Time taken: {}ms", methodName, timeTaken);
        }

        try {
            String action = "";
            if (methodName.startsWith("add"))
                action = "新增项目";
            else if (methodName.startsWith("update"))
                action = "修改项目";
            else if (methodName.startsWith("delete"))
                action = "删除项目";
            else
                return result; // Non-mutating method, simply return.

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            SysOperationLog operationLog = new SysOperationLog();
            operationLog.setAction(action);
            operationLog.setTargetType("IchProject");
            operationLog.setIp(request.getRemoteAddr());
            operationLog.setCreatedAt(LocalDateTime.now());
            operationLog.setDetail("执行方法：" + methodName + ", 耗时：" + timeTaken + "ms");

            // ✅ Bug3修复：从请求头读取真实用户ID，而不是硬编码为 1
            // 前端登录后将 userId 存入 localStorage，每次请求通过 X-User-Id 头传递
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                try {
                    operationLog.setUserId(Long.parseLong(userIdHeader));
                } catch (NumberFormatException ignored) {
                    operationLog.setUserId(-1L);
                }
            } else {
                operationLog.setUserId(-1L); // 未携带头时标记为未知
            }

            logMapper.insert(operationLog);
            log.info("✅ [操作日志] 已记录：{}，操作用户ID：{}", action, operationLog.getUserId());

        } catch (Exception e) {
            log.error("LogAspect Exception: ", e);
        }
        
        return result;
    }
}