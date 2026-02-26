package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.AppComment;
import com.example.demo.service.AppCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class AppCommentController {

    @Autowired
    private AppCommentService commentService;

    // 获取某个项目的评论列表 (按时间倒序)
    @GetMapping("/list")
    public Result<List<AppComment>> list(@RequestParam Long projectId) {
        // 这里实际项目中通常需要关联查询 User 表获取头像和昵称
        // 为简化毕设，假设前端根据 userId 再去查或者后端做个 DTO
        return Result.success(commentService.list(new LambdaQueryWrapper<AppComment>()
                .eq(AppComment::getProjectId, projectId)
                .orderByDesc(AppComment::getCreatedAt)));
    }

    // 发表评论
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody AppComment comment) {
        comment.setCreatedAt(LocalDateTime.now());
        comment.setStatus(1); // 默认正常
        return Result.success(commentService.save(comment));
    }

    /**
     * 删除评论接口
     * DELETE /api/comments/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id) {
        // 生产环境中，这里应该加权限校验：
        // if (!currentUser.isAdmin()) return Result.error("无权操作");

        boolean success = commentService.removeById(id);
        if (success) {
            return Result.success(true);
        } else {
            return Result.error("删除失败");
        }
    }
}