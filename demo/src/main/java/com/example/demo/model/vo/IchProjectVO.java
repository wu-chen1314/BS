package com.example.demo.model.vo;

import com.example.demo.entity.IchProject;
import java.util.List;

public class IchProjectVO extends IchProject {

    // 从分类表/地区表连接查询出来的展示文本
    private String categoryName;
    private String regionName;

    // 传承人展示及统计数据
    private String inheritorNames;
    private Long viewCount;

    // 可能需要的额外嵌套列表等
    private List<Long> inheritorIds;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getInheritorNames() {
        return inheritorNames;
    }

    public void setInheritorNames(String inheritorNames) {
        this.inheritorNames = inheritorNames;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public List<Long> getInheritorIds() {
        return inheritorIds;
    }

    public void setInheritorIds(List<Long> inheritorIds) {
        this.inheritorIds = inheritorIds;
    }
}
