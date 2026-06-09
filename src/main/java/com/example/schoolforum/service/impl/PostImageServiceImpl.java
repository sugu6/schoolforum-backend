package com.example.schoolforum.service.impl;

import com.example.schoolforum.config.FileUploadProperties;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.PostsMapper;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.service.PostImageService;
import com.example.schoolforum.util.FileUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static com.example.schoolforum.pojo.table.PostsTableDef.POSTS;

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
    private final PostsMapper postsMapper;

    private static final String IMAGE_URL_PREFIX = "/post-images/";

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

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
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("不支持的图片格式，仅允许: jpg, jpeg, png, gif, webp, bmp");
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
            throw new BusinessException("图片上传失败，请稍后重试");
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(IMAGE_URL_PREFIX)) {
            return;
        }

        // Check if the current user owns the post containing this image
        Long currentUserId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        QueryWrapper ownerCheck = QueryWrapper.create()
                .where(POSTS.AUTHOR_ID.eq(currentUserId))
                .and(POSTS.CONTENT.like("%" + imageUrl + "%"));
        long count = postsMapper.selectCountByQuery(ownerCheck);
        if (count == 0) {
            throw new BusinessException("无权删除此图片");
        }

        String filename = imageUrl.substring(IMAGE_URL_PREFIX.length());
        // 防止路径遍历攻击
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BusinessException("非法文件名");
        }
        try {
            Path uploadPath = Paths.get(fileUploadProperties.getPostImagePath());
            Path filePath = uploadPath.resolve(filename);
            // 确保解析后的路径仍在上传目录内
            if (!filePath.normalize().startsWith(uploadPath.normalize())) {
                throw new BusinessException("非法文件路径");
            }
            Files.deleteIfExists(filePath);
            log.info("帖子图片删除成功: imageUrl={}", imageUrl);
        } catch (IOException e) {
            log.error("帖子图片删除失败: imageUrl={}, error={}", imageUrl, e.getMessage());
        }
    }

}
