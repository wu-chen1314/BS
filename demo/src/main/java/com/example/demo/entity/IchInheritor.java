package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("ich_inheritor")
@Data
public class IchInheritor {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name; // 姓名
    private String avatarUrl; // 头像
    private String sex; // 性别
    private String level; // 传承级别
    private String description; // 简介
    private Long projectId; // 所属项目 ID

    // 地图位置相关字段
    private java.math.BigDecimal longitude; // 经度
    private java.math.BigDecimal latitude; // 纬度
    private String address; // 工作坊/传承地地址

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

}