package com.example.demo.controller;

import com.example.demo.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    // 也就是你的项目根目录下的 files 文件夹
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/files/";

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 1. 检查目录是否存在，不存在则创建
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 2. 生成唯一文件名 (防止文件名冲突)
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + suffix;

        // 3. 保存文件到本地硬盘
        File dest = new File(UPLOAD_DIR + newFileName);
        file.transferTo(dest);

        // 4. 返回可访问的 URL 地址
        // 注意：这里返回的是 http://localhost:8080/files/xxx.jpg
        String fileUrl = "http://localhost:8080/files/" + newFileName;
        return Result.success(fileUrl);
    }
}