package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;       // 操作人ID
    private String action;     // 操作动作（如：新增非遗项目）
    private String targetType; // 操作对象（如：IchProject）
    private Long targetId;     // 对象ID
    private String ip;         // IP地址
    private String detail;     // 详情
    private LocalDateTime createdAt;
}