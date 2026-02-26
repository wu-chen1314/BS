package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.AppComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppCommentMapper extends BaseMapper<AppComment> {
}