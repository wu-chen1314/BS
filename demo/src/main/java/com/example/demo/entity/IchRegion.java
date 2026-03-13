package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ich_region")
@Data
public class IchRegion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    @TableField("region_code")
    private String regionCode;

    private Integer level;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private LocalDateTime createdAt;
}
