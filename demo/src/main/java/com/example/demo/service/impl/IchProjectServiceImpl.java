package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.AppComment;
import com.example.demo.entity.AppFavorite;
import com.example.demo.entity.IchInheritor; // 确保你有这个实体类
import com.example.demo.entity.IchProject;
import com.example.demo.mapper.AppCommentMapper;
import com.example.demo.mapper.AppFavoriteMapper;
import com.example.demo.mapper.IchInheritorMapper; // 确保你有这个Mapper
import com.example.demo.mapper.IchProjectMapper;
import com.example.demo.service.IchProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IchProjectServiceImpl extends ServiceImpl<IchProjectMapper, IchProject> implements IchProjectService {

    // ✨✨ 1. 注入所有关联表的 Mapper
    @Autowired
    private IchInheritorMapper inheritorMapper; // 传承人

    @Autowired
    private AppCommentMapper commentMapper;     // 评论

    @Autowired
    private AppFavoriteMapper favoriteMapper;   // 收藏

    // ✨✨ 2. 实现级联删除
    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务：要么全删，要么都不删
    public boolean deleteProjectsWithRelations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return false;

        // A. 先删传承人 (这就是报错的原因！)
        inheritorMapper.delete(new LambdaQueryWrapper<IchInheritor>()
                .in(IchInheritor::getProjectId, ids));

        // B. 删评论
        commentMapper.delete(new LambdaQueryWrapper<AppComment>()
                .in(AppComment::getProjectId, ids));

        // C. 删收藏
        favoriteMapper.delete(new LambdaQueryWrapper<AppFavorite>()
                .in(AppFavorite::getProjectId, ids));

        // D. 最后删项目自己
        return this.removeByIds(ids);
    }
}