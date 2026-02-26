package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.AppFavorite; // 确保你生成了实体类
import com.example.demo.service.AppFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/favorites")
public class AppFavoriteController {

    @Autowired
    private AppFavoriteService favoriteService;

    // ✨ 切换收藏状态 (点一下收藏，再点一下取消)
    @PostMapping("/toggle")
    public Result<Boolean> toggleFavorite(@RequestBody AppFavorite favorite) {
        if (favorite.getUserId() == null || favorite.getProjectId() == null) {
            return Result.error("参数错误");
        }

        // 1. 查一下是否已经收藏过
        LambdaQueryWrapper<AppFavorite> query = new LambdaQueryWrapper<>();
        query.eq(AppFavorite::getUserId, favorite.getUserId())
                .eq(AppFavorite::getProjectId, favorite.getProjectId());

        AppFavorite exist = favoriteService.getOne(query);

        if (exist != null) {
            // 2. 如果已收藏，则删除 (取消收藏)
            favoriteService.removeById(exist.getId());
            return Result.success(false); // 返回 false 表示当前状态为“未收藏”
        } else {
            // 3. 如果未收藏，则添加
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteService.save(favorite);
            return Result.success(true); // 返回 true 表示当前状态为“已收藏”
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
}