package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.NewsArticle;
import com.example.demo.entity.NewsReadLog;
import com.example.demo.request.NewsArticleUpsertRequest;
import com.example.demo.service.NewsArticleService;
import com.example.demo.service.NewsReadLogService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsArticleService newsArticleService;
    private final NewsReadLogService newsReadLogService;
    private final RequestAuthUtil requestAuthUtil;

    public NewsController(NewsArticleService newsArticleService,
                          NewsReadLogService newsReadLogService,
                          RequestAuthUtil requestAuthUtil) {
        this.newsArticleService = newsArticleService;
        this.newsReadLogService = newsReadLogService;
        this.requestAuthUtil = requestAuthUtil;
    }

    @GetMapping("/articles")
    public Result<Map<String, Object>> listArticles(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "4") Integer pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String tag) {
        int safePageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int safePageSize = Math.max(1, Math.min(pageSize == null ? 4 : pageSize, 12));

        LambdaQueryWrapper<NewsArticle> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String safeKeyword = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(NewsArticle::getTitle, safeKeyword)
                    .or()
                    .like(NewsArticle::getSummary, safeKeyword)
                    .or()
                    .like(NewsArticle::getSource, safeKeyword));
        }
        if (StringUtils.hasText(tag)) {
            queryWrapper.eq(NewsArticle::getTag, tag.trim());
        }
        queryWrapper.orderByDesc(NewsArticle::getPublishedAt).orderByDesc(NewsArticle::getId);

        Page<NewsArticle> page = newsArticleService.page(new Page<>(safePageNum, safePageSize), queryWrapper);
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(this::toArticleListItem)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        result.put("hasMore", page.getCurrent() * page.getSize() < page.getTotal());
        result.put("availableTags", collectAvailableTags());
        return Result.success(result);
    }

    @GetMapping("/articles/{id}")
    public Result<Map<String, Object>> getArticleDetail(@PathVariable Long id) {
        NewsArticle article = newsArticleService.getById(id);
        if (article == null) {
            return Result.error("资讯不存在或已下线");
        }
        return Result.success(toArticleDetail(article));
    }

    @PostMapping("/articles")
    public Result<Map<String, Object>> createArticle(@RequestBody NewsArticleUpsertRequest requestBody,
                                                     HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("只有管理员可以发布资讯");
        }

        Result<NewsArticle> normalizedResult = normalizeArticle(null, requestBody);
        if (normalizedResult.getCode() != 200) {
            return Result.error(normalizedResult.getMsg());
        }

        NewsArticle article = normalizedResult.getData();
        newsArticleService.save(article);
        return Result.success(toArticleDetail(article));
    }

    @PutMapping("/articles/{id}")
    public Result<Map<String, Object>> updateArticle(@PathVariable Long id,
                                                     @RequestBody NewsArticleUpsertRequest requestBody,
                                                     HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("只有管理员可以编辑资讯");
        }

        NewsArticle existing = newsArticleService.getById(id);
        if (existing == null) {
            return Result.error("资讯不存在或已下线");
        }

        Result<NewsArticle> normalizedResult = normalizeArticle(existing, requestBody);
        if (normalizedResult.getCode() != 200) {
            return Result.error(normalizedResult.getMsg());
        }

        NewsArticle article = normalizedResult.getData();
        article.setId(id);
        newsArticleService.updateById(article);
        return Result.success(toArticleDetail(article));
    }

    @DeleteMapping("/articles/{id}")
    public Result<Boolean> deleteArticle(@PathVariable Long id, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("只有管理员可以删除资讯");
        }

        NewsArticle existing = newsArticleService.getById(id);
        if (existing == null) {
            return Result.error("资讯不存在或已下线");
        }

        return Result.success(newsArticleService.removeById(id));
    }

    @PostMapping("/articles/{id}/read")
    public Result<Map<String, Object>> recordArticleRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }

        NewsArticle article = newsArticleService.getById(id);
        if (article == null) {
            return Result.error("资讯不存在或已下线");
        }

        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        article.setUpdatedAt(LocalDateTime.now());
        newsArticleService.updateById(article);

        NewsReadLog readLog = new NewsReadLog();
        readLog.setUserId(userId);
        readLog.setArticleId(id);
        readLog.setCreatedAt(LocalDateTime.now());
        newsReadLogService.save(readLog);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("articleId", id);
        result.put("viewCount", article.getViewCount());
        result.put("readAt", readLog.getCreatedAt());
        return Result.success(result);
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(@RequestParam(defaultValue = "5") Integer recentLimit,
                                                    HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }

        int safeRecentLimit = Math.max(1, Math.min(recentLimit == null ? 5 : recentLimit, 10));
        List<NewsArticle> allArticles = newsArticleService.list(new LambdaQueryWrapper<NewsArticle>()
                .orderByDesc(NewsArticle::getPublishedAt)
                .orderByDesc(NewsArticle::getId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("articleCount", allArticles.size());
        result.put("totalViewCount", allArticles.stream()
                .mapToLong(item -> item.getViewCount() == null ? 0 : item.getViewCount())
                .sum());
        result.put("featuredTags", aggregateFeaturedTags(allArticles));
        result.put("hotArticles", allArticles.stream()
                .sorted((left, right) -> Integer.compare(
                        right.getViewCount() == null ? 0 : right.getViewCount(),
                        left.getViewCount() == null ? 0 : left.getViewCount()))
                .limit(3)
                .map(this::toArticleListItem)
                .collect(Collectors.toList()));
        result.put("recentReads", buildRecentReads(userId, safeRecentLimit));
        return Result.success(result);
    }

    private Result<NewsArticle> normalizeArticle(NewsArticle existing, NewsArticleUpsertRequest requestBody) {
        if (requestBody == null) {
            return Result.error("资讯内容不能为空");
        }

        String title = trimToNull(requestBody.getTitle());
        String content = trimToNull(requestBody.getContent());
        if (!StringUtils.hasText(title)) {
            return Result.error("请输入资讯标题");
        }
        if (!StringUtils.hasText(content)) {
            return Result.error("请输入资讯正文");
        }

        LocalDateTime now = LocalDateTime.now();
        NewsArticle article = existing == null ? new NewsArticle() : existing;
        article.setTitle(title);
        article.setContent(content);
        article.setSummary(buildSummary(requestBody.getSummary(), content));
        article.setSource(defaultIfBlank(requestBody.getSource(), "平台内容中心"));
        article.setTag(defaultIfBlank(requestBody.getTag(), "综合资讯"));
        article.setTagType(defaultIfBlank(requestBody.getTagType(), "info"));
        article.setCoverImageUrl(trimToNull(requestBody.getCoverImageUrl()));
        article.setVideoUrl(trimToNull(requestBody.getVideoUrl()));
        article.setPublishedAt(requestBody.getPublishedAt() == null ? now : requestBody.getPublishedAt());
        article.setUpdatedAt(now);
        article.setCreatedAt(existing == null || existing.getCreatedAt() == null ? now : existing.getCreatedAt());
        article.setViewCount(existing == null || existing.getViewCount() == null ? 0 : existing.getViewCount());
        return Result.success(article);
    }

    private String buildSummary(String summary, String content) {
        String normalizedSummary = trimToNull(summary);
        if (StringUtils.hasText(normalizedSummary)) {
            return normalizedSummary;
        }

        String compactContent = content.replaceAll("\\s+", " ").trim();
        if (compactContent.length() <= 90) {
            return compactContent;
        }
        return compactContent.substring(0, 90) + "...";
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private List<String> collectAvailableTags() {
        return newsArticleService.list(new LambdaQueryWrapper<NewsArticle>()
                        .orderByDesc(NewsArticle::getPublishedAt)
                        .orderByDesc(NewsArticle::getId))
                .stream()
                .map(NewsArticle::getTag)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private List<Map<String, Object>> buildRecentReads(Long userId, int limit) {
        Page<NewsReadLog> page = newsReadLogService.page(new Page<>(1, 20),
                new LambdaQueryWrapper<NewsReadLog>()
                        .eq(NewsReadLog::getUserId, userId)
                        .orderByDesc(NewsReadLog::getCreatedAt));

        if (page.getRecords() == null || page.getRecords().isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> articleIds = page.getRecords().stream()
                .map(NewsReadLog::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (articleIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, NewsArticle> articleMap = newsArticleService.listByIds(articleIds).stream()
                .collect(Collectors.toMap(NewsArticle::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (NewsReadLog log : page.getRecords()) {
            Long articleId = log.getArticleId();
            if (articleId == null || seen.contains(articleId)) {
                continue;
            }
            NewsArticle article = articleMap.get(articleId);
            if (article == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(toArticleListItem(article));
            item.put("lastReadAt", log.getCreatedAt());
            result.add(item);
            seen.add(articleId);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<Map<String, Object>> aggregateFeaturedTags(List<NewsArticle> articles) {
        if (CollectionUtils.isEmpty(articles)) {
            return Collections.emptyList();
        }

        Map<String, Long> counter = new LinkedHashMap<>();
        for (NewsArticle article : articles) {
            if (!StringUtils.hasText(article.getTag())) {
                continue;
            }
            long weight = article.getViewCount() == null || article.getViewCount() <= 0 ? 1L : article.getViewCount();
            counter.merge(article.getTag().trim(), weight, Long::sum);
        }

        return counter.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(6)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> toArticleListItem(NewsArticle article) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", article.getId());
        result.put("title", article.getTitle());
        result.put("summary", article.getSummary());
        result.put("source", article.getSource());
        result.put("tag", article.getTag());
        result.put("tagType", article.getTagType());
        result.put("image", article.getCoverImageUrl());
        result.put("coverImageUrl", article.getCoverImageUrl());
        result.put("videoUrl", article.getVideoUrl());
        result.put("publishedAt", article.getPublishedAt());
        result.put("viewCount", article.getViewCount() == null ? 0 : article.getViewCount());
        return result;
    }

    private Map<String, Object> toArticleDetail(NewsArticle article) {
        Map<String, Object> result = new LinkedHashMap<>(toArticleListItem(article));
        result.put("content", article.getContent());
        return result;
    }
}
