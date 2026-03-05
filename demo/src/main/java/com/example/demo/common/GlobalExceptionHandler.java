package com.example.demo.common.result;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ✅ Bug8修复：全局统一异常处理器（已增强）
 * 所有未捕获异常统一返回 Result 格式，避免 Spring 默认白页
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理数据库唯一键冲突（如账号重复注册）
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> handleDuplicate() {
        return Result.error("数据已存在，请勿重复添加");
    }

    // 处理参数非法异常
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error("参数错误：" + e.getMessage());
    }

    // 处理运行时异常（业务级别）
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        System.err.println("【全局异常】业务运行时异常：" + e.getMessage());
        e.printStackTrace();
        return Result.error("操作失败：" + e.getMessage());
    }

    // 兜底：捕获所有其他异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        System.err.println("【全局异常】系统异常：" + e.getMessage());
        e.printStackTrace();
        return Result.error("系统繁忙，请稍后再试");
    }
}
