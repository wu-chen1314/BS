package com.example.demo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("ich_project")
public class IchProject {
    @ExcelProperty("项目ID")
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;          // 项目名称
    private Long categoryId;      // 类别ID
    private Long regionId;        // 地区ID
    private String protectLevel;  // 保护级别
    private String status;        // 存续状态
    private String history;       // 历史渊源
    private String features;      // 核心特征
    private String coverUrl;      // 封面图
    // ✨✨✨ 必须补上这个字段！
    // 对应数据库的 video_url 字段
    private String videoUrl;
    private Integer auditStatus;  // 审核状态：1通过
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // ✨✨✨ 新增临时字段
    @TableField(exist = false)
    private String inheritorNames;

    // ✨✨✨ 新增临时字段
    @TableField(exist = false)
    private List<Long> inheritorIds;
}