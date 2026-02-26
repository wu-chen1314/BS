//package com.example.demo.entity;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//
//
//import java.util.Date;
//
//@Entity
//public class IchRegion {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;  // 地区ID
//
//    private Long parentId;  // 父级地区ID（省/市/县层级）
//
//    private String name;  // 地区名称
//
//    private String regionCode;  // 地区编码（可用行政区划编码）
//
//    private Byte level;  // 层级：1省 2市 3县区
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
//    public String getRegionCode() {
//        return regionCode;
//    }
//
//    public void setRegionCode(String regionCode) {
//        this.regionCode = regionCode;
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
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
//    // Getters and Setters
//}
