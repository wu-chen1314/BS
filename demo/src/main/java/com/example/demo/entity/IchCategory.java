package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@TableName("ich_category")
@Data
public class IchCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    private Integer level;

    private Integer sortNo;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<IchCategory> children;
}
