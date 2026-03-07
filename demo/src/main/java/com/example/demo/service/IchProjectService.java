package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.Result;
import com.example.demo.entity.IchProject;

import java.util.List;

public interface IchProjectService extends IService<IchProject> {

        boolean deleteProjectsWithRelations(List<Long> ids);

        Result<Boolean> saveProjectWithInheritors(IchProject project);

        <P extends IPage<IchProject>> P pageWithViewCount(P page, Wrapper<IchProject> queryWrapper);

        void populateProjectRelations(List<IchProject> projects);

        IchProject getProjectWithDetails(Long id);
}
