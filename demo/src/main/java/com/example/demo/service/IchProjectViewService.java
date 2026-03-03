package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.IchProjectView;

import java.util.List;

public interface IchProjectViewService extends IService<IchProjectView> {
    Long increaseViewCount(Long projectId);

    List<IchProjectView> getHotRanking(int limit);
}
