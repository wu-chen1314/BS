package com.example.demo.model.dto;

import com.example.demo.entity.IchProject;
import java.util.List;

/**
 * 用于接收前端提交的非遗项目保存表单
 */
public class IchProjectDTO extends IchProject {

    // 暂存表单提交上来的关联传承人 ID 数组
    private List<Long> inheritorIds;

    public List<Long> getInheritorIds() {
        return inheritorIds;
    }

    public void setInheritorIds(List<Long> inheritorIds) {
        this.inheritorIds = inheritorIds;
    }
}
