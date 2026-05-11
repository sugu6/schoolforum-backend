package com.example.schoolforum.service.impl;

import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.PostsMapper;
import com.example.schoolforum.pojo.BrowseHistory;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.service.BrowseHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.schoolforum.mapper.BrowseHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 浏览历史表 服务层实现。
 *
 * @author sugu
 * @since 2026-05-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl extends ServiceImpl<BrowseHistoryMapper, BrowseHistory> implements BrowseHistoryService {

    private final PostsMapper postsMapper;

    @Override
    @Transactional
    public BrowseHistory addBrowseHistory(Long userId, Long postId) {
        Posts post = postsMapper.selectOneById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        QueryWrapper existWrapper = QueryWrapper.create()
                .where(BrowseHistory::getUserId).eq(userId)
                .and(BrowseHistory::getPostId).eq(postId);
        BrowseHistory existHistory = getMapper().selectOneByQuery(existWrapper);
        
        if (existHistory != null) {
            existHistory.setViewedAt(LocalDateTime.now());
            getMapper().update(existHistory);
            log.info("更新浏览历史: userId={}, postId={}", userId, postId);
            return existHistory;
        }

        BrowseHistory browseHistory = BrowseHistory.builder()
                .userId(userId)
                .postId(postId)
                .viewedAt(LocalDateTime.now())
                .build();
        this.save(browseHistory);

        log.info("添加浏览历史: userId={}, postId={}", userId, postId);
        return browseHistory;
    }

    @Override
    @Transactional
    public void removeBrowseHistory(Long userId, Long postId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(BrowseHistory::getUserId).eq(userId)
                .and(BrowseHistory::getPostId).eq(postId);
        int deleted = getMapper().deleteByQuery(wrapper);
        if (deleted == 0) {
            throw new BusinessException("浏览历史不存在");
        }

        log.info("删除浏览历史: userId={}, postId={}", userId, postId);
    }

    @Override
    @Transactional
    public void clearBrowseHistory(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(BrowseHistory::getUserId).eq(userId);
        getMapper().deleteByQuery(wrapper);

        log.info("清空浏览历史: userId={}", userId);
    }

    @Override
    public Page<Posts> list(Long userId, int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select("p.*", "u.username as author_name", "u.avatar_url as author_avatar")
                .from("browse_history").as("bh")
                .leftJoin("posts").as("p").on("bh.post_id = p.id")
                .leftJoin("users").as("u").on("p.author_id = u.id")
                .where("bh.user_id = ?", userId)
                .orderBy("bh.viewed_at", false);
        return postsMapper.paginate(pageNumber, pageSize, wrapper);
    }

    @Override
    public Page<BrowseHistory> listPage(int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .orderBy("viewed_at", false);
        return getMapper().paginate(pageNumber, pageSize, wrapper);
    }
}
