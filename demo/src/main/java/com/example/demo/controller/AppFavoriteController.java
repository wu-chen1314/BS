package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.result.Result;
import com.example.demo.entity.AppFavorite;
import com.example.demo.entity.IchProject;
import com.example.demo.service.AppFavoriteService;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    // 切换收藏状态
    @PostMapping("/toggle")
    public Result<Boolean> toggleFavorite(@RequestBody AppFavorite favorite) {
        if (favorite.getUserId() == null || favorite.getProjectId() == null) {
            return Result.error("参数错误");
        }
        LambdaQueryWrapper<AppFavorite> query = new LambdaQueryWrapper<>();
        query.eq(AppFavorite::getUserId, favorite.getUserId())
                .eq(AppFavorite::getProjectId, favorite.getProjectId());
        AppFavorite exist = favoriteService.getOne(query);
        if (exist != null) {
            favoriteService.removeById(exist.getId());
            return Result.success(false);
        } else {
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteService.save(favorite);
            return Result.success(true);
        }
    }

    // 查询某用户是否收藏了某项目
    @GetMapping("/check")
    public Result<Boolean> checkFavorite(@RequestParam Long userId, @RequestParam Long projectId) {
        long count = favoriteService.count(new LambdaQueryWrapper<AppFavorite>()
                .eq(AppFavorite::getUserId, userId)
                .eq(AppFavorite::getProjectId, projectId));
        return Result.success(count > 0);
    }

    /**
     * ✨ 新功能3：分页查询用户的收藏列表，并关联返回项目基本信息
     * GET /api/favorites/list?userId=1&pageNum=1&pageSize=10
     */
    @GetMapping("/list")
    public Result<Page<IchProject>> getFavoriteList(@RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        // 1. 查出该用户的所有收藏记录（按时间倒序）
        List<AppFavorite> favorites = favoriteService.list(
                new LambdaQueryWrapper<AppFavorite>()
                        .eq(AppFavorite::getUserId, userId)
                        .orderByDesc(AppFavorite::getCreatedAt));
        if (favorites.isEmpty()) {
            return Result.success(new Page<>());
        }
        // 2. 提取项目ID列表
        List<Long> projectIds = favorites.stream()
                .map(AppFavorite::getProjectId)
                .collect(Collectors.toList());

        // 3. 分页查询这批项目（保持收藏时间倒序）
        Page<IchProject> page = projectService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<IchProject>().in(IchProject::getId, projectIds));
        return Result.success(page);
    }
}