package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final long NEWS_COVER_MAX_SIZE = 5L * 1024 * 1024;
    private static final long NEWS_VIDEO_MAX_SIZE = 80L * 1024 * 1024;

    private static final Set<String> IMAGE_SUFFIXES = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    ));
    private static final Set<String> NEWS_VIDEO_SUFFIXES = new HashSet<>(Arrays.asList(
            ".mp4", ".webm", ".mov"
    ));

    private final RequestAuthUtil requestAuthUtil;
    private final String uploadDir;

    public FileController(RequestAuthUtil requestAuthUtil,
                          @Value("${app.upload-dir:}") String configuredUploadDir) {
        this.requestAuthUtil = requestAuthUtil;
        this.uploadDir = resolveUploadDir(configuredUploadDir);
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return storeFile(file, "", null, 0L, "文件格式不正确", "文件上传失败");
    }

    @PostMapping("/upload/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return storeFile(
                file,
                "avatars",
                IMAGE_SUFFIXES,
                NEWS_COVER_MAX_SIZE,
                "仅支持 jpg、jpeg、png、gif、webp 格式的图片",
                "头像大小不能超过 5 MB"
        );
    }

    @PostMapping("/upload/news-cover")
    public Result<String> uploadNewsCover(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("只有管理员可以上传资讯封面");
        }
        return storeFile(
                file,
                "news/covers",
                IMAGE_SUFFIXES,
                NEWS_COVER_MAX_SIZE,
                "资讯封面仅支持 jpg、jpeg、png、gif、webp 格式",
                "资讯封面大小不能超过 5 MB"
        );
    }

    @PostMapping("/upload/news-video")
    public Result<String> uploadNewsVideo(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("只有管理员可以上传资讯视频");
        }
        return storeFile(
                file,
                "news/videos",
                NEWS_VIDEO_SUFFIXES,
                NEWS_VIDEO_MAX_SIZE,
                "资讯视频仅支持 mp4、webm、mov 格式",
                "资讯视频大小不能超过 80 MB"
        );
    }

    private Result<String> storeFile(MultipartFile file,
                                     String subDirectory,
                                     Set<String> allowedSuffixes,
                                     long maxSize,
                                     String invalidFormatMessage,
                                     String oversizeMessage) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = extractSuffix(originalFilename);
        if (!StringUtils.hasText(suffix)) {
            return Result.error("文件格式不正确");
        }

        if (allowedSuffixes != null && !allowedSuffixes.contains(suffix)) {
            return Result.error(invalidFormatMessage);
        }

        if (maxSize > 0 && file.getSize() > maxSize) {
            return Result.error(oversizeMessage);
        }

        File directory = resolveDirectory(subDirectory);
        if (!directory.exists() && !directory.mkdirs()) {
            return Result.error("上传目录创建失败");
        }

        String newFileName = UUID.randomUUID() + suffix;
        File destination = new File(directory, newFileName);
        try {
            file.transferTo(destination);
            return Result.success(buildPublicPath(subDirectory, newFileName));
        } catch (IOException exception) {
            exception.printStackTrace();
            return Result.error("文件上传失败：" + exception.getMessage());
        }
    }

    private String extractSuffix(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return null;
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }

    private File resolveDirectory(String subDirectory) {
        if (!StringUtils.hasText(subDirectory)) {
            return new File(uploadDir);
        }
        return new File(uploadDir, normalizeSubDirectory(subDirectory));
    }

    private String buildPublicPath(String subDirectory, String fileName) {
        String normalizedSubDirectory = normalizeSubDirectory(subDirectory);
        if (!StringUtils.hasText(normalizedSubDirectory)) {
            return "/files/" + fileName;
        }
        return "/files/" + normalizedSubDirectory.replace(File.separatorChar, '/') + "/" + fileName;
    }

    private String normalizeSubDirectory(String subDirectory) {
        return StringUtils.hasText(subDirectory)
                ? subDirectory.replace("/", File.separator).replace("\\", File.separator)
                : "";
    }

    private String resolveUploadDir(String configuredUploadDir) {
        String baseDir = StringUtils.hasText(configuredUploadDir)
                ? configuredUploadDir.trim()
                : System.getProperty("user.dir") + File.separator + "files";
        if (baseDir.endsWith(File.separator)) {
            return baseDir;
        }
        return baseDir + File.separator;
    }
}
