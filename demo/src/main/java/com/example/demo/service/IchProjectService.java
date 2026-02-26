package com.example.demo.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.IchProject;

import java.util.List;


public interface IchProjectService extends IService<IchProject> {
        // ✨✨ 新增：级联删除方法（同时删除关联的传承人、评论等）
        boolean deleteProjectsWithRelations(List<Long> ids);
}
