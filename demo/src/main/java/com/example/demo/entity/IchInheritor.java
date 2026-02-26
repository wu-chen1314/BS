package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ich_inheritor")
public class IchInheritor {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;        // 姓名
    private String avatarUrl;   //头像
    private String sex;         //性别
    private String level;       // 传承级别
    private String description; // 简介
    private Long projectId;     // 所属项目ID
    // ✨✨✨ 新增：用来存项目名字
    // @TableField(exist = false) 表示这个字段在数据库里不存在，是后来拼上去的
    @TableField(exist = false)
    private String projectName;
}