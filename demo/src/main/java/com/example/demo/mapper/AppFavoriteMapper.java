package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.AppFavorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppFavoriteMapper extends BaseMapper<AppFavorite> {
}