package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.IchCategory;
import com.example.demo.entity.IchInheritor;
import com.example.demo.entity.IchProject;
import com.example.demo.entity.IchRegion;
import com.example.demo.mapper.IchCategoryMapper;
import com.example.demo.mapper.IchRegionMapper;
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.vo.RegionCategoryVO;
import com.example.demo.vo.StatisticsVO;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/region-category")
public class RegionCategoryController {

    @Autowired
    private IchProjectService projectService;

    @Autowired
    private IchInheritorService inheritorService;

    @Autowired
    private IchCategoryMapper categoryMapper;

    @Autowired
    private IchRegionMapper regionMapper;

    @GetMapping("/all")
    @Cacheable(value = "regionCategory", key = "'allData'")
    public Result<RegionCategoryVO> getAllData() {
        RegionCategoryVO result = new RegionCategoryVO();
        result.setProvinces(regionMapper.selectList(
                new LambdaQueryWrapper<IchRegion>().eq(IchRegion::getLevel, 1).orderByAsc(IchRegion::getId)));
        result.setCategoryTree(buildCategoryTree(loadAllCategories()));
        result.setRegionStatistics(buildRegionStatistics(null));
        result.setCategoryStatistics(buildCategoryStatistics(null));
        return Result.success(result);
    }

    @GetMapping("/cities")
    @Cacheable(value = "regionCategory", key = "'cities_' + #provinceId")
    public Result<List<IchRegion>> getCities(@RequestParam Long provinceId) {
        List<IchRegion> cities = regionMapper.selectList(new LambdaQueryWrapper<IchRegion>()
                .eq(IchRegion::getParentId, provinceId)
                .orderByAsc(IchRegion::getId));
        return Result.success(cities);
    }

    @GetMapping("/projects")
    public Result<Page<IchProject>> getProjects(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long categoryId) {

        QueryWrapper<IchProject> query = new QueryWrapper<>();
        applyRegionFilter(query, regionId);
        applyCategoryFilter(query, categoryId);
        query.orderByDesc("id");

        Page<IchProject> pageResult = projectService.pageWithViewCount(new Page<>(page, limit), query);
        projectService.populateProjectRelations(pageResult.getRecords());
        return Result.success(pageResult);
    }

    @GetMapping("/statistics/by-region")
    @Cacheable(value = "regionStatistics", key = "#regionId != null ? #regionId : 'all'")
    public Result<StatisticsVO> getRegionStatistics(@RequestParam(required = false) Long regionId) {
        return Result.success(buildRegionStatistics(regionId));
    }
    
    @GetMapping("/statistics/by-category")
    @Cacheable(value = "categoryStatistics", key = "#categoryId != null ? #categoryId : 'all'")
    public Result<StatisticsVO> getCategoryStatistics(@RequestParam(required = false) Long categoryId) {
        return Result.success(buildCategoryStatistics(categoryId));
    }

    private StatisticsVO buildRegionStatistics(Long regionId) {
        QueryWrapper<IchProject> query = new QueryWrapper<>();
        query.select("id", "category_id");
        applyRegionFilter(query, regionId);

        List<IchProject> projects = projectService.list(query);
        List<Long> projectIds = projects.stream().map(IchProject::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Set<Long> categoryIds = projects.stream().map(IchProject::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());

        StatisticsVO result = new StatisticsVO();
        result.setTotalProjects(projects.size());
        result.setCategoryCount(categoryIds.size());
        result.setInheritorCount(countInheritors(projectIds));
        return result;
    }

    private StatisticsVO buildCategoryStatistics(Long categoryId) {
        QueryWrapper<IchProject> query = new QueryWrapper<>();
        query.select("id", "region_id");
        applyCategoryFilter(query, categoryId);

        List<IchProject> projects = projectService.list(query);
        List<Long> projectIds = projects.stream().map(IchProject::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Set<Long> regionIds = projects.stream().map(IchProject::getRegionId).filter(Objects::nonNull).collect(Collectors.toSet());

        StatisticsVO result = new StatisticsVO();
        result.setTotalProjects(projects.size());
        result.setRegionCount(regionIds.size());
        result.setInheritorCount(countInheritors(projectIds));
        return result;
    }

    private long countInheritors(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return 0L;
        }
        return inheritorService.count(new LambdaQueryWrapper<IchInheritor>().in(IchInheritor::getProjectId, projectIds));
    }

    private void applyRegionFilter(QueryWrapper<IchProject> query, Long regionId) {
        if (regionId == null) {
            return;
        }
        List<Long> regionIds = collectRegionIds(regionId);
        if (regionIds.isEmpty()) {
            query.eq("region_id", regionId);
            return;
        }
        query.in("region_id", regionIds);
    }

    private void applyCategoryFilter(QueryWrapper<IchProject> query, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        List<Long> categoryIds = collectCategoryIds(categoryId);
        if (categoryIds.isEmpty()) {
            query.eq("category_id", categoryId);
            return;
        }
        query.in("category_id", categoryIds);
    }

    private List<Long> collectRegionIds(Long rootId) {
        List<IchRegion> regions = regionMapper.selectList(new LambdaQueryWrapper<IchRegion>()
                .select(IchRegion::getId, IchRegion::getParentId));
        Map<Long, List<Long>> childrenMap = regions.stream()
                .filter(region -> region.getParentId() != null)
                .collect(Collectors.groupingBy(IchRegion::getParentId,
                        Collectors.mapping(IchRegion::getId, Collectors.toList())));
        return collectTreeIds(rootId, childrenMap);
    }

    private List<Long> collectCategoryIds(Long rootId) {
        List<IchCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<IchCategory>()
                .select(IchCategory::getId, IchCategory::getParentId));
        Map<Long, List<Long>> childrenMap = categories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(IchCategory::getParentId,
                        Collectors.mapping(IchCategory::getId, Collectors.toList())));
        return collectTreeIds(rootId, childrenMap);
    }

    private List<Long> collectTreeIds(Long rootId, Map<Long, List<Long>> childrenMap) {
        if (rootId == null) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            ids.add(currentId);
            for (Long childId : childrenMap.getOrDefault(currentId, Collections.emptyList())) {
                queue.add(childId);
            }
        }
        return ids;
    }

    private List<IchCategory> loadAllCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<IchCategory>()
                .orderByAsc(IchCategory::getSortNo)
                .orderByAsc(IchCategory::getId));
    }

    private List<IchCategory> buildCategoryTree(List<IchCategory> categories) {
        Map<Long, IchCategory> categoryMap = new LinkedHashMap<>();
        for (IchCategory category : categories) {
            category.setChildren(new ArrayList<>());
            categoryMap.put(category.getId(), category);
        }

        List<IchCategory> roots = new ArrayList<>();
        for (IchCategory category : categories) {
            Long parentId = category.getParentId();
            IchCategory parent = parentId == null ? null : categoryMap.get(parentId);
            if (parent == null) {
                roots.add(category);
            } else {
                parent.getChildren().add(category);
            }
        }
        return roots;
    }
}
