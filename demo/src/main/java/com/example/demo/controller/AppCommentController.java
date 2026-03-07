package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.result.Result;
import com.example.demo.entity.AppComment;
import com.example.demo.entity.SysUser;
import com.example.demo.model.vo.CommentVO;
import com.example.demo.service.AppCommentService;
import com.example.demo.service.SysUserService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
                .distinct()
                .collect(Collectors.toList());
        List<SysUser> users = sysUserService.listByIds(userIds);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<CommentVO> voList = comments.stream().map(comment -> {
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
                vo.setNickname("Anonymous");
                vo.setAvatarUrl("");
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody AppComment comment, HttpServletRequest request) {
        Long currentUserId = RequestAuthUtil.getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("Please log in first");
        }
        if (comment.getProjectId() == null || !StringUtils.hasText(comment.getContent())) {
            return Result.error("Project ID and content are required");
        }
        if (comment.getUserId() != null && !currentUserId.equals(comment.getUserId())) {
            return Result.error("Cannot post comments for another user");
        }

        comment.setUserId(currentUserId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setStatus(1);
        return Result.success(commentService.save(comment));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        AppComment comment = commentService.getById(id);
        if (comment == null) {
            return Result.error("Comment not found");
        }
        if (!RequestAuthUtil.isAdmin(request) && !RequestAuthUtil.isSelf(request, comment.getUserId())) {
            return Result.error("Cannot delete another user's comment");
        }
        return Result.success(commentService.removeById(id));
    }
}