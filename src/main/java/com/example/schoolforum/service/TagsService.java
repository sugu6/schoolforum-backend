package com.example.schoolforum.service;

import com.example.schoolforum.enums.ActiveStatus;
import com.mybatisflex.core.service.IService;
import com.example.schoolforum.pojo.Tags;

import java.util.List;
import java.util.Map;

/**
 * 标签表 服务层。
 *
 * @author sugu
 * @since 2026-03-09
 */
public interface TagsService extends IService<Tags> {

    List<Tags> listAll();

    List<Tags> listEnabled();

    List<Tags> listByCategoryId(Long categoryId);

    List<Tags> getHotTags(int limit);

    Tags createTag(String name, Long categoryId);

    Tags updateTag(Long id, String name, Long categoryId, ActiveStatus status);

    void deleteTag(Long id);

    void updatePostCount(Long tagId);

    Tags getByName(String name);

    List<Tags> getByIds(List<Long> tagIds);

    List<String> getTagNamesByPostId(Long postId);

    Map<Long, List<String>> getTagNamesByPostIds(List<Long> postIds);

    void associatePostWithTags(Long postId, List<Long> tagIds);

    void removePostTags(Long postId);

    void incrementTagPostCount(Long tagId);

    void decrementTagPostCount(Long tagId);
}
