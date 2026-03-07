package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.AppFavorite;
import com.example.demo.entity.IchProject;
import com.example.demo.service.AppFavoriteService;
import com.example.demo.service.IchProjectService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
public class AppFavoriteController {

    @Autowired
    private AppFavoriteService favoriteService;

    @Autowired
    private IchProjectService projectService;

    @Autowired
    private RequestAuthUtil requestAuthUtil;

    @PostMapping("/toggle")
    public Result<Boolean> toggleFavorite(@RequestBody AppFavorite favorite, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("未登录或令牌无效");
        }
        if (favorite.getProjectId() == null) {
            return Result.error("参数错误");
        }

        LambdaQueryWrapper<AppFavorite> query = new LambdaQueryWrapper<AppFavorite>()
                .eq(AppFavorite::getUserId, currentUserId)
                .eq(AppFavorite::getProjectId, favorite.getProjectId());
        AppFavorite existing = favoriteService.getOne(query);
        if (existing != null) {
            favoriteService.removeById(existing.getId());
            return Result.success(false);
        }

        favorite.setUserId(currentUserId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteService.save(favorite);
        return Result.success(true);
    }

    @GetMapping("/check")
    public Result<Boolean> checkFavorite(@RequestParam Long projectId, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("未登录或令牌无效");
        }

        long count = favoriteService.count(new LambdaQueryWrapper<AppFavorite>()
                .eq(AppFavorite::getUserId, currentUserId)
                .eq(AppFavorite::getProjectId, projectId));
        return Result.success(count > 0);
    }

    @GetMapping("/list")
    public Result<Page<IchProject>> getFavoriteList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("未登录或令牌无效");
        }

        List<AppFavorite> favorites = favoriteService.list(new LambdaQueryWrapper<AppFavorite>()
                .eq(AppFavorite::getUserId, currentUserId)
                .orderByDesc(AppFavorite::getCreatedAt));

        Page<IchProject> page = new Page<>(pageNum, pageSize, favorites.size());
        if (favorites.isEmpty()) {
            page.setRecords(Collections.emptyList());
            return Result.success(page);
        }

        List<Long> orderedProjectIds = favorites.stream()
                .map(AppFavorite::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (orderedProjectIds.isEmpty()) {
            page.setRecords(Collections.emptyList());
            return Result.success(page);
        }

        List<IchProject> projects = projectService.listByIds(orderedProjectIds);
        projectService.populateProjectRelations(projects);
        Map<Long, IchProject> projectMap = projects.stream()
                .collect(Collectors.toMap(IchProject::getId, Function.identity(), (left, right) -> left));

        List<IchProject> orderedProjects = orderedProjectIds.stream()
                .map(projectMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int fromIndex = Math.max((pageNum - 1) * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, orderedProjects.size());
        if (fromIndex >= orderedProjects.size()) {
            page.setRecords(Collections.emptyList());
        } else {
            page.setRecords(orderedProjects.subList(fromIndex, toIndex));
        }
        return Result.success(page);
    }
}
