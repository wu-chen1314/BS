package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_user")
@Data
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonAlias({ "password" })
    private String passwordHash;

    private String nickname;
    private String phone;
    private String email;
    private String avatarUrl;
    private String regionCode;
    private Integer status;
    private String role;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}