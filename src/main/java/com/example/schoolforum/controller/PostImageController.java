package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.schoolforum.service.PostImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/post-images")
@RequiredArgsConstructor
@Tag(name = "帖子图片管理", description = "帖子图片上传、删除等接口")
public class PostImageController {

    private final PostImageService postImageService;

    @PostMapping("/upload")
    @Operation(summary = "上传帖子图片", description = "上传帖子内容中的图片，返回图片URL，可嵌入Markdown内容中")
    @SaCheckLogin
    public Map<String, String> uploadImage(
            @Parameter(description = "图片文件", required = true) @RequestParam("file") MultipartFile file) {
        String imageUrl = postImageService.uploadImage(file);
        return Map.of("url", imageUrl);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除帖子图片", description = "删除指定的帖子图片")
    @SaCheckLogin
    public String deleteImage(
            @Parameter(description = "图片URL", required = true) @RequestParam String url) {
        postImageService.deleteImage(url);
        return "删除成功";
    }
}
