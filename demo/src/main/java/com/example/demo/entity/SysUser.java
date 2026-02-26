package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user") // 对应数据库表名
public class SysUser {

    @TableId(type = IdType.AUTO) // 对应数据库的自增主键
    private Long id;

    private String username;
    private String passwordHash; // 对应数据库 password_hash
    private String nickname;
    private String phone;
    private String email;
    private String avatarUrl;
    private String regionCode;
    private Integer status; // 1启用 0禁用
    private String role;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}