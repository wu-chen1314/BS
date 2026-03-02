package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.Result;
import com.example.demo.entity.AppComment;
import com.example.demo.entity.AppFavorite;
import com.example.demo.entity.IchInheritor;
import com.example.demo.entity.IchProject;
import com.example.demo.mapper.AppCommentMapper;
import com.example.demo.mapper.AppFavoriteMapper;
import com.example.demo.mapper.IchInheritorMapper;
import com.example.demo.mapper.IchProjectMapper;
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IchProjectServiceImpl extends ServiceImpl<IchProjectMapper, IchProject> implements IchProjectService {

        @Override
        public <P extends IPage<IchProject>> P pageWithViewCount(P page, Wrapper<IchProject> queryWrapper) {
                return baseMapper.selectPageWithViewCount(page, queryWrapper);
        }

        @Autowired
        private IchInheritorMapper inheritorMapper;

        @Autowired
        private IchInheritorService inheritorService;

        @Autowired
        private AppCommentMapper commentMapper;

        @Autowired
        private AppFavoriteMapper favoriteMapper;

        @Override
        @Transactional(rollbackFor = Exception.class)
        public boolean deleteProjectsWithRelations(List<Long> ids) {
                if (ids == null || ids.isEmpty())
                        return false;
                inheritorMapper.delete(new LambdaQueryWrapper<IchInheritor>().in(IchInheritor::getProjectId, ids));
                commentMapper.delete(new LambdaQueryWrapper<AppComment>().in(AppComment::getProjectId, ids));
                favoriteMapper.delete(new LambdaQueryWrapper<AppFavorite>().in(AppFavorite::getProjectId, ids));
                return this.removeByIds(ids);
        }

        // ✅ Bug6修复：多步操作加入事务，任意步骤出错则全部回滚
        @Override
        @Transactional(rollbackFor = Exception.class)
        public Result<Boolean> saveProjectWithInheritors(IchProject project) {
                boolean success = this.saveOrUpdate(project);
                if (success) {
                        Long projectId = project.getId();
                        // 先清空该项目的所有传承人绑定
                        UpdateWrapper<IchInheritor> reset = new UpdateWrapper<>();
                        reset.eq("project_id", projectId).set("project_id", null);
                        inheritorService.update(reset);
                        // 重新绑定前端传入的传承人列表
                        List<Long> newIds = project.getInheritorIds();
                        if (newIds != null && !newIds.isEmpty()) {
                                List<IchInheritor> updates = newIds.stream().map(id -> {
                                        IchInheritor inheritor = new IchInheritor();
                                        inheritor.setId(id);
                                        inheritor.setProjectId(projectId);
                                        return inheritor;
                                }).collect(Collectors.toList());
                                inheritorService.updateBatchById(updates);
                        }
                }
                return Result.success(success);
        }
}
