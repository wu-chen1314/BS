package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.AppComment;
import com.example.demo.entity.CommentVO;
import com.example.demo.entity.SysUser;
import com.example.demo.service.AppCommentService;
import com.example.demo.service.SysUserService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
public class AppCommentController {

    @Autowired
    private AppCommentService commentService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private RequestAuthUtil requestAuthUtil;

    @GetMapping("/list")
    public Result<List<CommentVO>> list(@RequestParam Long projectId) {
        List<AppComment> comments = commentService.list(new LambdaQueryWrapper<AppComment>()
                .eq(AppComment::getProjectId, projectId)
                .orderByDesc(AppComment::getCreatedAt));

        if (comments.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<Long> userIds = comments.stream()
                .map(AppComment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<SysUser> users = sysUserService.listByIds(userIds);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, item -> item));

        List<CommentVO> result = comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setProjectId(comment.getProjectId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());
            vo.setParentId(comment.getParentId());
            vo.setStatus(comment.getStatus());
            vo.setCreatedAt(comment.getCreatedAt());

            SysUser user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatarUrl(user.getAvatarUrl());
            } else {
                vo.setNickname("匿名用户");
                vo.setAvatarUrl("");
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody AppComment comment, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("未登录或令牌无效");
        }
        if (comment.getProjectId() == null || comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }

        comment.setUserId(currentUserId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setStatus(1);
        return Result.success(commentService.save(comment));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = requestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("未登录或令牌无效");
        }

        AppComment comment = commentService.getById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        if (!requestAuthUtil.isAdmin(request) && !currentUserId.equals(comment.getUserId())) {
            return Result.error("无权限删除他人评论");
        }
        return Result.success(commentService.removeById(id));
    }
}
