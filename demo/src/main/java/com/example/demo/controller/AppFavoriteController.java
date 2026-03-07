package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.result.Result;
import com.example.demo.entity.AppFavorite;
import com.example.demo.entity.IchProject;
import com.example.demo.service.AppFavoriteService;
import com.example.demo.service.IchProjectService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
public class AppFavoriteController {

    @Autowired
    private AppFavoriteService favoriteService;

    @Autowired
    private IchProjectService projectService;

    @PostMapping("/toggle")
    public Result<Boolean> toggleFavorite(@RequestBody AppFavorite favorite, HttpServletRequest request) {
        Long currentUserId = RequestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("Please log in first");
        }
        if (favorite.getProjectId() == null) {
            return Result.error("Project ID is required");
        }
        if (favorite.getUserId() != null && !RequestAuthUtil.isSelfOrAdmin(request, favorite.getUserId())) {
            return Result.error("Cannot change another user's favorites");
        }

        Long effectiveUserId = favorite.getUserId() == null ? currentUserId : favorite.getUserId();
        LambdaQueryWrapper<AppFavorite> query = new LambdaQueryWrapper<>();
        query.eq(AppFavorite::getUserId, effectiveUserId)
                .eq(AppFavorite::getProjectId, favorite.getProjectId());
        AppFavorite exist = favoriteService.getOne(query);
        if (exist != null) {
            favoriteService.removeById(exist.getId());
            return Result.success(false);
        }

        favorite.setUserId(effectiveUserId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteService.save(favorite);
        return Result.success(true);
    }

    @GetMapping("/check")
    public Result<Boolean> checkFavorite(@RequestParam Long userId, @RequestParam Long projectId, HttpServletRequest request) {
        if (!RequestAuthUtil.isSelfOrAdmin(request, userId)) {
            return Result.error("Cannot view another user's favorite status");
        }
        long count = favoriteService.count(new LambdaQueryWrapper<AppFavorite>()
                .eq(AppFavorite::getUserId, userId)
                .eq(AppFavorite::getProjectId, projectId));
        return Result.success(count > 0);
    }

    @GetMapping("/list")
    public Result<Page<IchProject>> getFavoriteList(@RequestParam Long userId,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    HttpServletRequest request) {
        if (!RequestAuthUtil.isSelfOrAdmin(request, userId)) {
            return Result.error("Cannot view another user's favorites");
        }

        List<AppFavorite> favorites = favoriteService.list(
                new LambdaQueryWrapper<AppFavorite>()
                        .eq(AppFavorite::getUserId, userId)
                        .orderByDesc(AppFavorite::getCreatedAt));
        if (favorites.isEmpty()) {
            return Result.success(new Page<>());
        }

        List<Long> projectIds = favorites.stream()
                .map(AppFavorite::getProjectId)
                .collect(Collectors.toList());

        Page<IchProject> page = projectService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<IchProject>().in(IchProject::getId, projectIds));
        return Result.success(page);
    }
}