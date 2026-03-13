package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.common.Result;
import com.example.demo.entity.AppComment;
import com.example.demo.entity.AppFavorite;
import com.example.demo.entity.IchCategory;
import com.example.demo.entity.IchInheritor;
import com.example.demo.entity.IchProject;
import com.example.demo.entity.IchProjectView;
import com.example.demo.entity.IchRegion;
import com.example.demo.mapper.AppCommentMapper;
import com.example.demo.mapper.AppFavoriteMapper;
import com.example.demo.mapper.IchCategoryMapper;
import com.example.demo.mapper.IchProjectMapper;
import com.example.demo.mapper.IchProjectViewMapper;
import com.example.demo.mapper.IchRegionMapper;
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IchProjectServiceImpl extends ServiceImpl<IchProjectMapper, IchProject> implements IchProjectService {

        @Autowired
        private IchInheritorService inheritorService;

        @Autowired
        private IchCategoryMapper categoryMapper;

        @Autowired
        private IchRegionMapper regionMapper;

        @Autowired
        private IchProjectViewMapper projectViewMapper;

        @Autowired
        private AppCommentMapper commentMapper;

        @Autowired
        private AppFavoriteMapper favoriteMapper;

        @Override
        public <P extends IPage<IchProject>> P pageWithViewCount(P page, Wrapper<IchProject> queryWrapper) {
                return baseMapper.selectPageWithViewCount(page, queryWrapper);
        }

        @Override
        public void populateProjectRelations(List<IchProject> projects) {
                if (projects == null || projects.isEmpty()) {
                        return;
                }

                List<Long> projectIds = projects.stream()
                        .map(IchProject::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());

                if (projectIds.isEmpty()) {
                        return;
                }

                List<IchInheritor> inheritors = inheritorService.list(
                        new LambdaQueryWrapper<IchInheritor>().in(IchInheritor::getProjectId, projectIds));

                Map<Long, List<IchInheritor>> inheritorMap = inheritors.stream()
                        .filter(item -> item.getProjectId() != null)
                        .collect(Collectors.groupingBy(IchInheritor::getProjectId));

                Set<Long> categoryIds = projects.stream()
                        .map(IchProject::getCategoryId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Map<Long, String> categoryNameMap = categoryIds.isEmpty()
                        ? Collections.emptyMap()
                        : categoryMapper.selectBatchIds(categoryIds).stream()
                                .collect(Collectors.toMap(IchCategory::getId, IchCategory::getName));

                Set<Long> regionIds = projects.stream()
                        .map(IchProject::getRegionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Map<Long, IchRegion> regionMap = regionIds.isEmpty()
                        ? Collections.emptyMap()
                        : regionMapper.selectBatchIds(regionIds).stream()
                                .collect(Collectors.toMap(IchRegion::getId, item -> item));

                Map<Long, Long> viewCountMap = projectViewMapper.selectList(
                                new LambdaQueryWrapper<IchProjectView>().in(IchProjectView::getProjectId, projectIds))
                        .stream()
                        .collect(Collectors.toMap(IchProjectView::getProjectId, IchProjectView::getViewCount,
                                (left, right) -> right));

                for (IchProject project : projects) {
                        List<IchInheritor> projectInheritors = inheritorMap.getOrDefault(project.getId(), Collections.emptyList());
                        project.setInheritorNames(projectInheritors.stream()
                                .map(IchInheritor::getName)
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining(", ")));
                        project.setInheritorIds(projectInheritors.stream()
                                .map(IchInheritor::getId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
                        project.setCategoryName(categoryNameMap.get(project.getCategoryId()));
                        IchRegion region = regionMap.get(project.getRegionId());
                        project.setRegionName(region == null ? null : region.getName());
                        if ((project.getLongitude() == null || project.getLatitude() == null) && region != null) {
                                project.setLongitude(region.getLongitude());
                                project.setLatitude(region.getLatitude());
                        }
                        if (project.getViewCount() == null) {
                                project.setViewCount(viewCountMap.getOrDefault(project.getId(), 0L));
                        }
                }
        }

        @Override
        public IchProject getProjectWithDetails(Long id) {
                IchProject project = this.getById(id);
                if (project == null) {
                        return null;
                }
                populateProjectRelations(Collections.singletonList(project));
                return project;
        }

        @Override
        @Transactional(rollbackFor = Exception.class)
        public boolean deleteProjectsWithRelations(List<Long> ids) {
                if (ids == null || ids.isEmpty())
                        return false;
                UpdateWrapper<IchInheritor> reset = new UpdateWrapper<>();
                reset.in("project_id", ids).set("project_id", null);
                inheritorService.update(reset);
                projectViewMapper.delete(new LambdaQueryWrapper<IchProjectView>().in(IchProjectView::getProjectId, ids));
                commentMapper.delete(new LambdaQueryWrapper<AppComment>().in(AppComment::getProjectId, ids));
                favoriteMapper.delete(new LambdaQueryWrapper<AppFavorite>().in(AppFavorite::getProjectId, ids));
                return this.removeByIds(ids);
        }

        @Override
        @Transactional(rollbackFor = Exception.class)
        public Result<Boolean> saveProjectWithInheritors(IchProject project) {
                boolean success = this.saveOrUpdate(project);
                if (success) {
                        Long projectId = project.getId();
                        UpdateWrapper<IchInheritor> reset = new UpdateWrapper<>();
                        reset.eq("project_id", projectId).set("project_id", null);
                        inheritorService.update(reset);
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

