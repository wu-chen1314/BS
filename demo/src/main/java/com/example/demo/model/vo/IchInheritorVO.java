package com.example.demo.model.vo;

import com.example.demo.entity.IchInheritor;

public class IchInheritorVO extends IchInheritor {

    // 关联的项目文本名称
    private String projectName;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
