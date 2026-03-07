package com.example.demo.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.result.Result;
import com.example.demo.entity.AiChatHistory;
import com.example.demo.entity.AppComment;
import com.example.demo.entity.AppFavorite;
import com.example.demo.entity.ChatSession;
import com.example.demo.entity.SysOperationLog;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.AiChatHistoryMapper;
import com.example.demo.mapper.AppCommentMapper;
import com.example.demo.mapper.AppFavoriteMapper;
import com.example.demo.mapper.ChatSessionMapper;
import com.example.demo.mapper.SysOperationLogMapper;
import com.example.demo.service.SysUserService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private AppFavoriteMapper appFavoriteMapper;

    @Autowired
    private AppCommentMapper appCommentMapper;

    @Autowired
    private AiChatHistoryMapper aiChatHistoryMapper;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id, HttpServletRequest request) {
        if (!RequestAuthUtil.isSelfOrAdmin(request, id)) {
            return Result.error("No permission to view this user");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("User not found");
        }
        user.setPasswordHash(null);
        return Result.success(user);
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody SysUser user, HttpServletRequest request) {
        if (user.getId() == null) {
            return Result.error("User ID is required");
        }
        if (!RequestAuthUtil.isSelfOrAdmin(request, user.getId())) {
            return Result.error("No permission to update this user");
        }

        SysUser existing = sysUserService.getById(user.getId());
        if (existing == null) {
            return Result.error("User not found");
        }

        boolean isAdmin = RequestAuthUtil.isAdmin(request);
        existing.setNickname(user.getNickname());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setAvatarUrl(user.getAvatarUrl());
        existing.setRegionCode(user.getRegionCode());

        if (isAdmin) {
            if (user.getStatus() != null) {
                existing.setStatus(user.getStatus());
            }
            if (StrUtil.isNotBlank(user.getRole())) {
                existing.setRole("admin".equals(user.getRole()) ? "admin" : "user");
            }
        }

        existing.setPasswordHash(null);
        return Result.success(sysUserService.updateById(existing));
    }

    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody SysUser user, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can add users");
        }
        if (StrUtil.isBlank(user.getUsername())) {
            return Result.error("Username is required");
        }

        long count = sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.error("Username already exists");
        }

        user.setRole("admin".equals(user.getRole()) ? "admin" : "user");
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setPasswordHash(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));
        return Result.success(sysUserService.save(user));
    }

    @GetMapping("/page")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String keyword,
                                      HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can view user list");
        }

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper;
        if (StrUtil.isNotBlank(keyword)) {
            wrapper = new LambdaQueryWrapper<SysUser>()
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickname, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword);
        } else {
            wrapper = new LambdaQueryWrapper<>();
        }

        Page<SysUser> resultPage = sysUserService.page(page, wrapper);
        resultPage.getRecords().forEach(item -> item.setPasswordHash(null));
        return Result.success(resultPage);
    }

    @DeleteMapping("/delete/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can delete users");
        }
        if (RequestAuthUtil.isSelf(request, id)) {
            return Result.error("Cannot delete current login user");
        }

        sysOperationLogMapper.delete(new LambdaQueryWrapper<SysOperationLog>()
                .eq(SysOperationLog::getUserId, id));
        appFavoriteMapper.delete(new LambdaQueryWrapper<AppFavorite>()
                .eq(AppFavorite::getUserId, id));
        appCommentMapper.delete(new LambdaQueryWrapper<AppComment>()
                .eq(AppComment::getUserId, id));
        aiChatHistoryMapper.delete(new LambdaQueryWrapper<AiChatHistory>()
                .eq(AiChatHistory::getUserId, id));
        chatSessionMapper.delete(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, id));
        return Result.success(sysUserService.removeById(id));
    }

    @PutMapping("/reset-password/{id}")
    public Result<Boolean> resetPassword(@PathVariable Long id, HttpServletRequest request) {
        if (!RequestAuthUtil.isAdmin(request)) {
            return Result.error("Only administrators can reset passwords");
        }

        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("User not found");
        }
        user.setPasswordHash(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));
        return Result.success(sysUserService.updateById(user));
    }

    @PostMapping("/change-password")
    public Result<Boolean> changePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long id = Long.parseLong(params.get("id"));
        if (!RequestAuthUtil.isSelf(request, id)) {
            return Result.error("Only current user can change own password");
        }

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("User not found");
        }

        String md5OldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes(StandardCharsets.UTF_8));
        if (!md5OldPassword.equals(user.getPasswordHash())) {
            return Result.error("Current password is incorrect");
        }

        user.setPasswordHash(DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8)));
        return Result.success(sysUserService.updateById(user));
    }
}
