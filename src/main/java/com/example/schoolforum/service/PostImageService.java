package com.example.schoolforum.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 帖子图片上传服务接口。
 *
 * @author sugu
 * @since 2026-03-07
 */
public interface PostImageService {

    /**
     * 上传帖子图片
     *
     * @param file 图片文件
     * @return 图片访问URL
     */
    String uploadImage(MultipartFile file);

    /**
     * 删除帖子图片
     *
     * @param imageUrl 图片URL
     */
    void deleteImage(String imageUrl);
}
