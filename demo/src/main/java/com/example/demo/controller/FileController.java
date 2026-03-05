package com.example.demo.controller;

import com.example.demo.common.result.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    // 上传目录
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/files/";

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            // 1. 检查目录是否存在，不存在则创建
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 2. 生成唯一文件名 (防止文件名冲突)
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return Result.error("文件格式不正确");
            }
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + suffix;

            // 3. 保存文件到本地硬盘
            File dest = new File(UPLOAD_DIR + newFileName);
            file.transferTo(dest);

            // 4. 返回可访问的 URL 地址
            String fileUrl = "/files/" + newFileName;
            return Result.success(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    // 专门用于头像上传的接口
    @PostMapping("/upload/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 验证文件类型，只允许图片
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件格式不正确");
        }
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!suffix.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return Result.error("只支持 jpg、jpeg、png、gif、webp 格式的图片");
        }

        // 调用通用上传方法
        return upload(file);
    }
}