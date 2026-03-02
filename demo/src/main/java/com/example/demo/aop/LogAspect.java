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
            String methodName = joinPoint.getSignature().getName();

            String action = "";
            if (methodName.startsWith("add"))
                action = "新增项目";
            else if (methodName.startsWith("update"))
                action = "修改项目";
            else if (methodName.startsWith("delete"))
                action = "删除项目";
            else
                return;

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            SysOperationLog log = new SysOperationLog();
            log.setAction(action);
            log.setTargetType("IchProject");
            log.setIp(request.getRemoteAddr());
            log.setCreatedAt(LocalDateTime.now());
            log.setDetail("执行方法：" + methodName);

            // ✅ Bug3修复：从请求头读取真实用户ID，而不是硬编码为 1
            // 前端登录后将 userId 存入 localStorage，每次请求通过 X-User-Id 头传递
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                try {
                    log.setUserId(Long.parseLong(userIdHeader));
                } catch (NumberFormatException ignored) {
                    log.setUserId(-1L);
                }
            } else {
                log.setUserId(-1L); // 未携带头时标记为未知
            }

            logMapper.insert(log);
            System.out.println("✅ [操作日志] 已记录：" + action + "，操作用户ID：" + log.getUserId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}