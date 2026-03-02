package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.Result;
import com.example.demo.entity.IchProject;

import java.util.List;

public interface IchProjectService extends IService<IchProject> {
        // 级联删除方法（同时删除关联的传承人、评论等）
        boolean deleteProjectsWithRelations(List<Long> ids);

        // ✅ Bug6修复：将多步保存逻辑迁入 Service，配合 @Transactional 使用
        Result<Boolean> saveProjectWithInheritors(IchProject project);

        // 分页获取项目（带浏览量）
        <P extends IPage<IchProject>> P pageWithViewCount(P page, Wrapper<IchProject> queryWrapper);
}
