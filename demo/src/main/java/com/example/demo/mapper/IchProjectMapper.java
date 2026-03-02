package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.example.demo.entity.IchProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IchProjectMapper extends BaseMapper<IchProject> {
    @Select("SELECT p.*, COALESCE(v.view_count, 0) as view_count FROM ich_project p " +
            "LEFT JOIN ich_project_view v ON p.id = v.project_id ${ew.customSqlSegment}")
    <P extends IPage<IchProject>> P selectPageWithViewCount(P page,
            @Param(Constants.WRAPPER) Wrapper<IchProject> queryWrapper);
}