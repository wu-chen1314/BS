package com.example.demo.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewsArticleUpsertRequest {

    private String title;
    private String summary;
    private String content;
    private String source;
    private String tag;
    private String tagType;
    private String coverImageUrl;
    private String videoUrl;
    private LocalDateTime publishedAt;
}
