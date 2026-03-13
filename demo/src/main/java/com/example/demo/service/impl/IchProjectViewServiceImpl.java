package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.IchProjectView;
import com.example.demo.mapper.IchProjectViewMapper;
import com.example.demo.service.IchProjectViewService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IchProjectViewServiceImpl extends ServiceImpl<IchProjectViewMapper, IchProjectView>
        implements IchProjectViewService {

    @Override
    public List<IchProjectView> getHotRanking(int limit) {
        LambdaQueryWrapper<IchProjectView> query = new LambdaQueryWrapper<>();
        query.orderByDesc(IchProjectView::getViewCount);
        query.last("LIMIT " + limit);
        return this.list(query);
    }

    @Override
    public Long increaseViewCount(Long projectId) {
        if (projectId == null) {
            return 0L;
        }

        LambdaQueryWrapper<IchProjectView> query = new LambdaQueryWrapper<>();
        query.eq(IchProjectView::getProjectId, projectId);
        IchProjectView viewRecord = this.getOne(query);

        if (viewRecord == null) {
            viewRecord = new IchProjectView();
            viewRecord.setProjectId(projectId);
            viewRecord.setViewCount(1L);
            viewRecord.setUpdatedAt(LocalDateTime.now());
            this.save(viewRecord);
        } else {
            viewRecord.setViewCount(viewRecord.getViewCount() + 1);
            viewRecord.setUpdatedAt(LocalDateTime.now());
            this.updateById(viewRecord);
        }

        return viewRecord.getViewCount();
    }

    @Override
    public Long getCurrentViewCount(Long projectId) {
        if (projectId == null) {
            return 0L;
        }

        LambdaQueryWrapper<IchProjectView> query = new LambdaQueryWrapper<>();
        query.eq(IchProjectView::getProjectId, projectId);
        IchProjectView viewRecord = this.getOne(query);
        return viewRecord == null || viewRecord.getViewCount() == null ? 0L : viewRecord.getViewCount();
    }
}
