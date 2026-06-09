package com.example.schoolforum.service.impl;

import com.example.schoolforum.event.PostStatsUpdateEvent;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.PostsMapper;
import com.example.schoolforum.pojo.Favorites;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.service.FavoritesService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.schoolforum.mapper.FavoritesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.schoolforum.pojo.table.FavoritesTableDef.FAVORITES;

/**
 * 收藏表 服务层实现。
 *
 * @author sugu
 * @since 2026-03-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoritesServiceImpl extends ServiceImpl<FavoritesMapper, Favorites> implements FavoritesService {

    private final PostsMapper postsMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Favorites addFavorite(Long userId, Long postId) {
        Posts post = postsMapper.selectOneById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        Favorites favorite = Favorites.builder()
                .userId(userId)
                .postId(postId)
                .createdAt(LocalDateTime.now())
                .build();
        try {
            this.save(favorite);
        } catch (DuplicateKeyException e) {
            // 已收藏，幂等处理
            return favorite;
        }

        eventPublisher.publishEvent(new PostStatsUpdateEvent(postId, PostStatsUpdateEvent.StatsType.FAVORITE_ADD));

        log.info("用户收藏帖子: userId={}, postId={}", userId, postId);
        return favorite;
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long postId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(Favorites::getUserId).eq(userId)
                .and(Favorites::getPostId).eq(postId);
        int deleted = getMapper().deleteByQuery(wrapper);
        if (deleted == 0) {
            throw new BusinessException("未收藏该帖子");
        }

        eventPublisher.publishEvent(new PostStatsUpdateEvent(postId, PostStatsUpdateEvent.StatsType.FAVORITE_REMOVE));

        log.info("用户取消收藏: userId={}, postId={}", userId, postId);
    }

    @Override
    public boolean isFavorited(Long userId, Long postId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(Favorites::getUserId).eq(userId)
                .and(Favorites::getPostId).eq(postId);
        return getMapper().selectCountByQuery(wrapper) > 0;
    }

    @Override
    public List<Posts> getUserFavorites(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select("f.*")
                .from("favorites").as("f")
                .where(FAVORITES.as("f").USER_ID.eq(userId))
                .orderBy("f.created_at", false);
        List<Favorites> favorites = getMapper().selectListByQuery(wrapper);

        if (favorites.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> postIds = favorites.stream()
                .map(Favorites::getPostId)
                .toList();

        QueryWrapper postWrapper = QueryWrapper.create()
                .select("p.*", "u.username as author_name")
                .from("posts").as("p")
                .leftJoin("users").as("u").on("p.author_id = u.id")
                .in("p.id", postIds);
        return postsMapper.selectListByQuery(postWrapper);
    }

    @Override
    public Page<Posts> list(Long userId, int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select("p.*", "u.username as author_name", "u.avatar_url as author_avatar")
                .from("favorites").as("f")
                .leftJoin("posts").as("p").on("f.post_id = p.id")
                .leftJoin("users").as("u").on("p.author_id = u.id")
                .where(FAVORITES.as("f").USER_ID.eq(userId))
                .orderBy("f.created_at", false);
        return postsMapper.paginate(pageNumber, pageSize, wrapper);
    }

    @Override
    public Page<Favorites> listPage(int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .orderBy("created_at", false);
        return getMapper().paginate(pageNumber, pageSize, wrapper);
    }

    @Override
    public int countByPostId(Long postId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(Favorites::getPostId).eq(postId);
        return (int) getMapper().selectCountByQuery(wrapper);
    }
}
