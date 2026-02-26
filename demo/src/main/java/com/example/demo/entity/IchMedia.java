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
//public class IchMedia {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;  // 媒体资源ID
//
//    private String mediaType;  // 资源类型：image/audio/video/model/doc
//
//    private String title;  // 标题/说明
//
//    private String url;  // 资源URL/文件路径
//
//    private Integer durationSec;  // 时长（秒，音视频可用）
//
//    private Long sizeBytes;  // 文件大小（字节）
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
//    public String getMediaType() {
//        return mediaType;
//    }
//
//    public void setMediaType(String mediaType) {
//        this.mediaType = mediaType;
//    }
//
//    public String getTitle() {
//        return title;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    public String getUrl() {
//        return url;
//    }
//
//    public void setUrl(String url) {
//        this.url = url;
//    }
//
//    public Integer getDurationSec() {
//        return durationSec;
//    }
//
//    public void setDurationSec(Integer durationSec) {
//        this.durationSec = durationSec;
//    }
//
//    public Long getSizeBytes() {
//        return sizeBytes;
//    }
//
//    public void setSizeBytes(Long sizeBytes) {
//        this.sizeBytes = sizeBytes;
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
