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
//public class SysPermission {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;  // 权限ID
//
//    private String permCode;  // 权限编码（如 ich:project:add）
//
//    private String permName;  // 权限名称（中文）
//
//    private String permDesc;  // 权限说明
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
//    public String getPermCode() {
//        return permCode;
//    }
//
//    public void setPermCode(String permCode) {
//        this.permCode = permCode;
//    }
//
//    public String getPermName() {
//        return permName;
//    }
//
//    public void setPermName(String permName) {
//        this.permName = permName;
//    }
//
//    public String getPermDesc() {
//        return permDesc;
//    }
//
//    public void setPermDesc(String permDesc) {
//        this.permDesc = permDesc;
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
