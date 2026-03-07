package com.example.demo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@TableName("ich_project")
@Data
public class IchProject {
    @ExcelProperty("项目ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name; // 项目名称
    private Long categoryId; // 类别 ID
    private Long regionId; // 地区 ID
    private String protectLevel; // 保护级别
    private String status; // 传承状态
    private String history; // 历史介绍
    private String features; // 项目特点
    private String coverUrl; // 封面图
    // 视频地址，对应数据库 video_url 字段
    private String videoUrl;
    private Integer auditStatus; // 审核状态：0 待审核，1 已通过，2 已驳回
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 地理与服务信息
    private java.math.BigDecimal longitude; // 经度
    private java.math.BigDecimal latitude; // 纬度
    private String address; // 地址
    private String contactPhone; // 联系电话
    private String openingHours; // 开放时间

    // 非数据库字段：传承人名称展示
    @TableField(exist = false)
    private String inheritorNames;

    // 非数据库字段：传承人 ID 集合
    @TableField(exist = false)
    private List<Long> inheritorIds;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String regionName;

    @TableField(exist = false)
    private Long viewCount;

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getProtectLevel() {
        return protectLevel;
    }

    public void setProtectLevel(String protectLevel) {
        this.protectLevel = protectLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getInheritorNames() {
        return inheritorNames;
    }

    public void setInheritorNames(String inheritorNames) {
        this.inheritorNames = inheritorNames;
    }

    public List<Long> getInheritorIds() {
        return inheritorIds;
    }

    public void setInheritorIds(List<Long> inheritorIds) {
        this.inheritorIds = inheritorIds;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }
}
