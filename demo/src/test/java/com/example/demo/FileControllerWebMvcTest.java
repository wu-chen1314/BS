package com.example.demo;

import com.example.demo.config.WebConfig;
import com.example.demo.controller.FileController;
import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.RequestAuthUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileController.class)
@Import({WebConfig.class, JwtInterceptor.class, RequestAuthUtil.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=ichPromotionSystemSecretKey202603021530NonHeritage",
        "jwt.expiration=86400000",
        "app.upload-dir=target/test-uploads"
})
class FileControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @AfterEach
    void cleanupUploads() throws Exception {
        Path uploadDir = Paths.get("target/test-uploads");
        if (!Files.exists(uploadDir)) {
            return;
        }
        Files.walk(uploadDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
    }

    @Test
    void adminCanUploadNewsCover() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", "cover".getBytes());

        mockMvc.perform(multipart("/api/file/upload/news-cover")
                        .file(file)
                        .header("Authorization", bearerToken(1L, "admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", startsWith("/files/news/covers/")));
    }

    @Test
    void nonAdminCannotUploadNewsCover() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", "cover".getBytes());

        mockMvc.perform(multipart("/api/file/upload/news-cover")
                        .file(file)
                        .header("Authorization", bearerToken(8L, "reader", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("只有管理员可以上传资讯封面"));
    }

    @Test
    void adminVideoUploadRejectsInvalidFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "clip.txt", "text/plain", "video".getBytes());

        mockMvc.perform(multipart("/api/file/upload/news-video")
                        .file(file)
                        .header("Authorization", bearerToken(1L, "admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("资讯视频仅支持 mp4、webm、mov 格式"));
    }

    private String bearerToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return "Bearer " + jwtUtil.generateToken(claims, username);
    }
}
