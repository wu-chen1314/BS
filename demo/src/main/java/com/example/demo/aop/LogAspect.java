package com.example.demo.aop;

import com.example.demo.entity.SysOperationLog;
import com.example.demo.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
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

    // 1. 定义切点：拦截 IchProjectController 下的所有方法
    @Pointcut("execution(* com.example.demo.controller.IchProjectController.*(..))")
    public void projectLog() {}

    // 2. 后置通知：当方法执行成功后触发
    @AfterReturning(pointcut = "projectLog()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            // 获取方法名
            String methodName = joinPoint.getSignature().getName();

            // 我们只记录“增删改”操作，不记录“查询”
            String action = "";
            if (methodName.startsWith("add")) action = "新增项目";
            else if (methodName.startsWith("update")) action = "修改项目";
            else if (methodName.startsWith("delete")) action = "删除项目";
            else return; // 如果是 getPage 之类的查询，直接忽略

            // 获取请求信息 (IP等)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            // 构造日志对象
            SysOperationLog log = new SysOperationLog();
            log.setAction(action);
            log.setTargetType("IchProject");
            log.setIp(request.getRemoteAddr());
            log.setCreatedAt(LocalDateTime.now());

            // 模拟获取当前用户ID (实际应从 Token 或 Session 获取)
            // 这里为了演示，暂时写死为 1 (管理员)
            log.setUserId(1L);

            log.setDetail("执行方法：" + methodName);

            // 保存到数据库
            logMapper.insert(log);
            System.out.println("✅ [操作日志] 已记录：" + action);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}