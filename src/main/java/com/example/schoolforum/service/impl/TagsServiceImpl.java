package com.example.schoolforum.service.impl;

import com.example.schoolforum.enums.ActiveStatus;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.PostTagsMapper;
import com.example.schoolforum.mapper.TagsMapper;
import com.example.schoolforum.pojo.PostTags;
import com.example.schoolforum.pojo.Tags;
import com.example.schoolforum.service.TagsService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.schoolforum.pojo.table.PostTagsTableDef.POST_TAGS;
import static com.example.schoolforum.pojo.table.TagsTableDef.TAGS;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags> implements TagsService {

    private final TagsMapper tagsMapper;
    private final PostTagsMapper postTagsMapper;

    @Override
    public List<Tags> listAll() {
        QueryWrapper wrapper = QueryWrapper.create()
                .orderBy(TAGS.POST_COUNT, false);
        return this.list(wrapper);
    }

    @Override
    public List<Tags> listEnabled() {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(TAGS.STATUS.eq(ActiveStatus.ACTIVE))
                .orderBy(TAGS.POST_COUNT, false);
        return this.list(wrapper);
    }

    @Override
    public List<Tags> listByCategoryId(Long categoryId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(TAGS.CATEGORY_ID.eq(categoryId))
                .where(TAGS.STATUS.eq(ActiveStatus.ACTIVE))
                .orderBy(TAGS.POST_COUNT, false);
        return this.list(wrapper);
    }

    @Override
    public List<Tags> getHotTags(int limit) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(TAGS.STATUS.eq(ActiveStatus.ACTIVE))
                .orderBy(TAGS.POST_COUNT, false)
                .limit(limit);
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public Tags createTag(String name, Long categoryId) {
        if (getByName(name) != null) {
            throw new BusinessException("标签名称已存在");
        }

        Tags tag = new Tags();
        tag.setName(name);
        tag.setCategoryId(categoryId);
        tag.setPostCount(0);
        tag.setStatus(ActiveStatus.ACTIVE);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());

        this.save(tag);
        log.info("标签创建成功: id={}, name={}", tag.getId(), name);
        return tag;
    }

    @Override
    @Transactional
    public Tags updateTag(Long id, String name, Long categoryId, ActiveStatus status) {
        Tags tag = this.getById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        if (name != null && !name.equals(tag.getName())) {
            if (getByName(name) != null) {
                throw new BusinessException("标签名称已存在");
            }
            tag.setName(name);
        }
        if (categoryId != null) {
            tag.setCategoryId(categoryId);
        }
        if (status != null) {
            tag.setStatus(status);
        }
        tag.setUpdatedAt(LocalDateTime.now());

        this.updateById(tag);
        log.info("标签更新成功: id={}", id);
        return tag;
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tags tag = this.getById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        QueryWrapper deleteWrapper = QueryWrapper.create()
                .where(POST_TAGS.TAG_ID.eq(id));
        postTagsMapper.deleteByQuery(deleteWrapper);

        this.removeById(id);
        log.info("标签删除成功: id={}, name={}", id, tag.getName());
    }

    @Override
    @Transactional
    public void updatePostCount(Long tagId) {
        if (tagId == null) {
            return;
        }

        QueryWrapper countWrapper = QueryWrapper.create()
                .where(POST_TAGS.TAG_ID.eq(tagId));

        long count = postTagsMapper.selectCountByQuery(countWrapper);

        Tags tag = UpdateEntity.of(Tags.class, tagId);
        tag.setPostCount((int) count);
        tag.setUpdatedAt(LocalDateTime.now());
        tagsMapper.update(tag);
    }

    @Override
    public Tags getByName(String name) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(TAGS.NAME.eq(name));
        return this.getOne(wrapper);
    }

    @Override
    public List<Tags> getByIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .where(TAGS.ID.in(tagIds));
        return this.list(wrapper);
    }

    @Override
    public List<String> getTagNamesByPostId(Long postId) {
        if (postId == null) {
            return Collections.emptyList();
        }

        QueryWrapper wrapper = QueryWrapper.create()
                .select("t.name")
                .from("post_tags").as("pt")
                .leftJoin("tags").as("t").on("pt.tag_id = t.id")
                .where("pt.post_id = ?", postId);

        List<Tags> tags = tagsMapper.selectListByQuery(wrapper);
        return tags.stream()
                .map(Tags::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<String>> getTagNamesByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QueryWrapper wrapper = QueryWrapper.create()
                .select("pt.post_id", "t.name")
                .from("post_tags").as("pt")
                .leftJoin("tags").as("t").on("pt.tag_id = t.id")
                .where(POST_TAGS.POST_ID.in(postIds));

        List<Map<String, Object>> results = tagsMapper.selectListByQueryAs(wrapper, (Class<Map<String, Object>>)(Class<?>)Map.class);

        return results.stream()
                .collect(Collectors.groupingBy(
                        row -> ((Number) row.get("post_id")).longValue(),
                        Collectors.mapping(
                                row -> (String) row.get("name"),
                                Collectors.toList()
                        )
                ));
    }

    @Override
    @Transactional
    public void associatePostWithTags(Long postId, List<Long> tagIds) {
        if (postId == null || tagIds == null || tagIds.isEmpty()) {
            return;
        }

        removePostTags(postId);

        LocalDateTime now = LocalDateTime.now();
        for (Long tagId : tagIds) {
            PostTags postTag = new PostTags();
            postTag.setPostId(postId);
            postTag.setTagId(tagId);
            postTag.setCreatedAt(now);
            postTagsMapper.insert(postTag);

            incrementTagPostCount(tagId);
        }
        log.info("帖子标签关联成功: postId={}, tagIds={}", postId, tagIds);
    }

    @Override
    @Transactional
    public void removePostTags(Long postId) {
        if (postId == null) {
            return;
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(POST_TAGS.POST_ID.eq(postId));
        List<PostTags> existingTags = postTagsMapper.selectListByQuery(queryWrapper);

        for (PostTags pt : existingTags) {
            decrementTagPostCount(pt.getTagId());
        }

        QueryWrapper deleteWrapper = QueryWrapper.create()
                .where(POST_TAGS.POST_ID.eq(postId));
        postTagsMapper.deleteByQuery(deleteWrapper);
    }

    @Override
    @Transactional
    public void incrementTagPostCount(Long tagId) {
        if (tagId == null) {
            return;
        }
        Tags tag = this.getById(tagId);
        if (tag != null) {
            tag.setPostCount(tag.getPostCount() == null ? 1 : tag.getPostCount() + 1);
            tag.setUpdatedAt(LocalDateTime.now());
            this.updateById(tag);
        }
    }

    @Override
    @Transactional
    public void decrementTagPostCount(Long tagId) {
        if (tagId == null) {
            return;
        }
        Tags tag = this.getById(tagId);
        if (tag != null && tag.getPostCount() != null && tag.getPostCount() > 0) {
            tag.setPostCount(tag.getPostCount() - 1);
            tag.setUpdatedAt(LocalDateTime.now());
            this.updateById(tag);
        }
    }
}
