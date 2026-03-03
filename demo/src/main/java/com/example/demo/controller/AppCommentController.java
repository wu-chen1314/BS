package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.AppComment;
import com.example.demo.entity.CommentVO;
import com.example.demo.entity.SysUser;
import com.example.demo.service.AppCommentService;
import com.example.demo.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
public class AppCommentController {

    @Autowired
    private AppCommentService commentService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * ✅ Bug7修复：获取某个项目的评论列表，携带用户昵称和头像
     * GET /api/comments/list?projectId=1
     */
    @GetMapping("/list")
    public Result<List<CommentVO>> list(@RequestParam Long projectId) {
        // 1. 查询评论列表（按时间倒序）
        List<AppComment> comments = commentService.list(new LambdaQueryWrapper<AppComment>()
                .eq(AppComment::getProjectId, projectId)
                .orderByDesc(AppComment::getCreatedAt));

        if (comments.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 2. 提取所有 userId，批量查询用户信息（避免 N+1）
        List<Long> userIds = comments.stream()
                .map(AppComment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<SysUser> users = sysUserService.listByIds(userIds);
        // 以 userId 为 key 建立 Map，方便 O(1) 查找
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 3. 组装 VO
        List<CommentVO> voList = comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setProjectId(c.getProjectId());
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setParentId(c.getParentId());
            vo.setStatus(c.getStatus());
            vo.setCreatedAt(c.getCreatedAt());
            SysUser user = userMap.get(c.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatarUrl(user.getAvatarUrl());
            } else {
                vo.setNickname("匿名用户");
                vo.setAvatarUrl("");
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    // 发表评论
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody AppComment comment) {
        comment.setCreatedAt(LocalDateTime.now());
        comment.setStatus(1);
        return Result.success(commentService.save(comment));
    }

    // 删除评论
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id) {
        boolean success = commentService.removeById(id);
        if (success) {
            return Result.success(true);
        } else {
            return Result.error("删除失败");
        }
    }
}