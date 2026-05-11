package com.example.schoolforum.service.impl;

import com.example.schoolforum.config.FileUploadProperties;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.service.PostImageService;
import com.example.schoolforum.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

/**
 * 帖子图片上传服务实现。
 *
 * @author sugu
 * @since 2026-03-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageServiceImpl implements PostImageService {

    private final FileUploadProperties fileUploadProperties;

    private static final String IMAGE_URL_PREFIX = "/post-images/";

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String contentType = file.getContentType();
        String allowedTypes = fileUploadProperties.getAllowedTypes();
        if (contentType == null || !Arrays.asList(allowedTypes.split(",")).contains(contentType)) {
            throw new BusinessException("不支持的文件类型，仅支持: " + allowedTypes);
        }

        long maxSize = FileUtil.parseSize(fileUploadProperties.getMaxSize());
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超过限制，最大允许: " + fileUploadProperties.getMaxSize());
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            Path uploadPath = Paths.get(fileUploadProperties.getPostImagePath());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            String imageUrl = IMAGE_URL_PREFIX + newFilename;
            log.info("帖子图片上传成功: imageUrl={}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("帖子图片上传失败: error={}", e.getMessage(), e);
            throw new BusinessException("图片上传失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(IMAGE_URL_PREFIX)) {
            return;
        }

        String filename = imageUrl.substring(IMAGE_URL_PREFIX.length());
        try {
            Path uploadPath = Paths.get(fileUploadProperties.getPostImagePath());
            Path filePath = uploadPath.resolve(filename);
            Files.deleteIfExists(filePath);
            log.info("帖子图片删除成功: imageUrl={}", imageUrl);
        } catch (IOException e) {
            log.error("帖子图片删除失败: imageUrl={}, error={}", imageUrl, e.getMessage());
        }
    }

}
