package com.example.demo.common;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j; // 如果你有 lombok 的话

@RestControllerAdvice // 这是一个增强版 Controller，专门捕获异常
public class GlobalExceptionHandler {

    // 捕获所有 Exception 类型的错误
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        // 在后台打印错误堆栈，方便程序员排错
        e.printStackTrace();

        // 返回给前端友好的提示，而不是报错页面
        return Result.error("系统繁忙，请稍后再试：" + e.getMessage());
    }

    // 你还可以专门捕获特定异常，比如 DuplicateKeyException (数据库重复)
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> handleDuplicate() { return Result.error("数据已存在，请勿重复添加"); }
}