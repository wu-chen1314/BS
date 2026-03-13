package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("heritage_trail_favorite")
public class HeritageTrailFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String trailId;

    private String trailTitle;

    private String routeType;

    private LocalDateTime createdAt;
}
