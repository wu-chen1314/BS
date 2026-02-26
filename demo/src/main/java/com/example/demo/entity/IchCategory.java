//package com.example.demo.entity;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//
//import java.util.Date;
//
//@Entity
//public class IchCategory {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;  // 类别ID
//
//    private Long parentId;  // 父级类别ID（可为空）
//
//    private String name;  // 类别名称（如传统技艺）
//
//    private Byte level;  // 层级：1/2/3...
//
//    private Integer sortNo;  // 排序号（越小越靠前）
//
//    private Date createdAt;  // 创建时间
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public Long getParentId() {
//        return parentId;
//    }
//
//    public void setParentId(Long parentId) {
//        this.parentId = parentId;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public Byte getLevel() {
//        return level;
//    }
//
//    public void setLevel(Byte level) {
//        this.level = level;
//    }
//
//    public Integer getSortNo() {
//        return sortNo;
//    }
//
//    public void setSortNo(Integer sortNo) {
//        this.sortNo = sortNo;
//    }
//
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
//// Getters and Setters
//}
