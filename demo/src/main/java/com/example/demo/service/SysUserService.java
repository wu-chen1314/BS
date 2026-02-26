package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.Result;
import com.example.demo.entity.SysUser;

import java.util.Map;


public interface SysUserService extends IService<SysUser> {

    // 注册服务
    Result<SysUser> register(SysUser user);

    // 登录服务
    Result<SysUser> login(SysUser user);

    //忘记密码
    Result<Boolean> resetPassword(Map<String, String> params);
}