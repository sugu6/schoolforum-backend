package com.example.schoolforum.service.impl;

import com.example.schoolforum.component.PostQueryHelper;
import com.example.schoolforum.component.PostStatsCache;
import com.example.schoolforum.component.PostViewCountCache;
import com.example.schoolforum.service.SearchService;
import com.example.schoolforum.constant.RedisCacheKey;
import com.example.schoolforum.enums.EssentialStatus;
import com.example.schoolforum.enums.PinnedStatus;
import com.example.schoolforum.enums.RelatedType;
import com.example.schoolforum.event.PostStatsUpdateEvent;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.CommentsMapper;
import com.example.schoolforum.mapper.FavoritesMapper;
import com.example.schoolforum.pojo.Categories;
import com.example.schoolforum.pojo.PostTags;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.service.CategoriesService;
import com.example.schoolforum.service.TagsService;
import com.example.schoolforum.service.UsersService;
import com.example.schoolforum.util.HotScoreCalculator;
import com.example.schoolforum.util.MarkdownUtil;
import com.example.schoolforum.util.PermissionUtil;
import com.example.schoolforum.websocket.PostStatsWebSocketHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.schoolforum.mapper.PostsMapper;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.service.PostsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.schoolforum.pojo.table.PostsTableDef.POSTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostsServiceImpl extends ServiceImpl<PostsMapper, Posts> implements PostsService {

    private final PostsMapper postsMapper;
    private final PostQueryHelper postQueryHelper;
    private final SearchService searchService;
    private final CategoriesService categoriesService;
    private final TagsService tagsService;
    private final PostViewCountCache viewCountCache;
    private final PostStatsCache postStatsCache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UsersService usersService;
    private final PostStatsWebSocketHandler postStatsWebSocketHandler;
    private final HotScoreCalculator hotScoreCalculator;
    private final CommentsMapper commentsMapper;
    private final FavoritesMapper favoritesMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<Posts> getHotList(int limit) {
        String cacheKey = RedisCacheKey.hotPostsKey(limit);
        
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                List<Posts> posts = objectMapper.readValue(cached, new TypeReference<List<Posts>>() {});
                fillRealTimeStats(posts);
                return posts;
            }
        } catch (Exception e) {
            log.warn("读取热门帖子缓存失败", e);
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .where("p.created_at >= ?", thirtyDaysAgo)
                .orderBy("p.like_count", false)
                .orderBy("p.view_count", false)
                .orderBy("p.comment_count", false)
                .limit(limit * 3);

        List<Posts> candidatePosts = postsMapper.selectListByQuery(wrapper);

        candidatePosts.sort((a, b) -> Double.compare(
            hotScoreCalculator.calculateScore(b),
            hotScoreCalculator.calculateScore(a)
        ));

        List<Posts> posts = candidatePosts.stream()
                .limit(limit)
                .toList();

        fillRealTimeStats(posts);

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(posts),
                    RedisCacheKey.HOT_POSTS_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入热门帖子缓存失败", e);
        }

        return posts;
    }

    @Override
    public List<Posts> getLatestList(int limit) {
        String cacheKey = RedisCacheKey.latestPostsKey(limit);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                List<Posts> posts = objectMapper.readValue(cached, new TypeReference<List<Posts>>() {});
                fillRealTimeStats(posts);
                return posts;
            }
        } catch (Exception e) {
            log.warn("读取最新帖子缓存失败", e);
        }

        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .orderBy("p.updated_at", false)
                .limit(limit);

        List<Posts> posts = postsMapper.selectListByQuery(wrapper);
        fillRealTimeStats(posts);
        fillTagsForPosts(posts);

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(posts),
                    RedisCacheKey.LATEST_POSTS_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入最新帖子缓存失败", e);
        }

        return posts;
    }

    @Override
    public Page<Posts> getHotListPage(int pageNumber, int pageSize, Long categoryId) {
        List<Long> categoryIds = getCategoryIds(categoryId);

        String zSetKey = RedisCacheKey.hotRankZSetKey("all");
        
        try {
            Set<String> topPostIdsFromCache = redisTemplate.opsForZSet()
                    .reverseRange(zSetKey, (long) (pageNumber - 1) * pageSize, (long) pageNumber * pageSize - 1);

            if (topPostIdsFromCache != null && !topPostIdsFromCache.isEmpty()) {
                List<Long> postIds = topPostIdsFromCache.stream()
                        .map(Long::parseLong)
                        .toList();

                QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                        .where(POSTS.ID.in(postIds))
                        .orderBy("p.is_pinned", false);

                if (!categoryIds.isEmpty()) {
                    wrapper.and(POSTS.CATEGORY_ID.in(categoryIds));
                }

                Page<Posts> page = postsMapper.paginate(pageNumber, pageSize, wrapper);
                
                List<Posts> sortedPosts = postIds.stream()
                        .map(id -> page.getRecords().stream()
                                .filter(p -> p.getId().equals(id))
                                .findFirst()
                                .orElse(null))
                        .filter(p -> p != null)
                        .toList();

                page.setRecords(sortedPosts);
                fillRealTimeStats(sortedPosts);
                fillTagsForPosts(sortedPosts);
                return page;
            }
        } catch (Exception e) {
            log.warn("从Redis读取热门榜失败，回退到数据库查询", e);
        }

        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .orderBy("p.is_pinned", false);

        if (!categoryIds.isEmpty()) {
            wrapper.where(POSTS.CATEGORY_ID.in(categoryIds));
        }

        Page<Posts> page = postsMapper.paginate(pageNumber, pageSize, wrapper);
        
        List<Posts> records = page.getRecords();
        records.sort((a, b) -> Double.compare(
            hotScoreCalculator.calculateScore(b),
            hotScoreCalculator.calculateScore(a)
        ));

        fillRealTimeStats(records);
        fillTagsForPosts(records);
        
        return page;
    }

    @Override
    public Page<Posts> getLatestListPage(int pageNumber, int pageSize, Long categoryId) {
        List<Long> categoryIds = getCategoryIds(categoryId);

        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .orderBy("p.is_pinned", false)
                .orderBy("p.updated_at", false);

        if (!categoryIds.isEmpty()) {
            wrapper.where(POSTS.CATEGORY_ID.in(categoryIds));
        }

        Page<Posts> page = postsMapper.paginate(pageNumber, pageSize, wrapper);
        fillRealTimeStats(page.getRecords());
        fillTagsForPosts(page.getRecords());
        return page;
    }

    @Override
    @Transactional
    public Posts createPost(Long authorId, String title, String content, List<Long> tagIds, Long categoryId, String coverImage) {
        Posts post = new Posts();
        post.setAuthorId(authorId);
        post.setTitle(title);
        post.setContent(content);
        post.setCategoryId(categoryId);

        if (coverImage != null && !coverImage.isBlank()) {
            post.setCoverImage(coverImage);
        } else {
            String firstImage = MarkdownUtil.extractFirstImage(content);
            if (firstImage != null) {
                post.setCoverImage(firstImage);
            }
        }
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setIsPinned(PinnedStatus.NOT_PINNED);
        post.setIsEssential(EssentialStatus.NOT_ESSENTIAL);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        this.save(post);

        if (tagIds != null && !tagIds.isEmpty()) {
            tagsService.associatePostWithTags(post.getId(), tagIds);
        }

        if (categoryId != null) {
            categoriesService.updatePostCount(categoryId);
        }

        searchService.indexPostById(post.getId());

        log.info("帖子创建成功: postId={}, authorId={}, categoryId={}", post.getId(), authorId, categoryId);
        return post;
    }

    @Override
    @Transactional
    public Posts updatePost(Long postId, String title, String content, List<Long> tagIds, Long categoryId, String coverImage, Long userId) {
        Posts post = getPostOrThrow(postId);
        PermissionUtil.checkOwnerOrAdmin(post.getAuthorId(), "无权限操作此帖子");

        if (title != null && !title.isBlank()) {
            post.setTitle(title);
        }
        if (content != null && !content.isBlank()) {
            post.setContent(content);
        }
        if (tagIds != null) {
            tagsService.removePostTags(postId);
            if (!tagIds.isEmpty()) {
                tagsService.associatePostWithTags(postId, tagIds);
            }
        }
        if (categoryId != null && !categoryId.equals(post.getCategoryId())) {
            Long oldCategoryId = post.getCategoryId();
            post.setCategoryId(categoryId);
            if (oldCategoryId != null) {
                categoriesService.updatePostCount(oldCategoryId);
            }
            categoriesService.updatePostCount(categoryId);
        }
        if (coverImage != null) {
            post.setCoverImage(coverImage.isBlank() ? null : coverImage);
        }
        post.setUpdatedAt(LocalDateTime.now());

        this.updateById(post);

        searchService.indexPostById(postId);

        log.info("帖子更新成功: postId={}, userId={}", postId, userId);
        return post;
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Posts post = getPostOrThrow(postId);
        PermissionUtil.checkOwnerOrAdmin(post.getAuthorId(), "无权限操作此帖子");

        Long categoryId = post.getCategoryId();

        tagsService.removePostTags(postId);

        this.removeById(postId);
        searchService.deletePost(postId);

        if (categoryId != null) {
            categoriesService.updatePostCount(categoryId);
        }
        
        log.info("帖子删除成功: postId={}, userId={}", postId, userId);
    }

    @Override
    public Posts getPostWithViewCount(Long postId, boolean increment) {
        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .where("p.id = {0}", postId);

        Posts post = postsMapper.selectOneByQuery(wrapper);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        if (increment) {
            viewCountCache.incrementViewCount(postId);

            int actualCommentCount = commentsMapper.countByPostId(postId);
            int actualFavoriteCount = favoritesMapper.countByPostId(postId);
            postStatsCache.initPostStats(postId, post.getViewCount(), post.getLikeCount(), actualCommentCount, actualFavoriteCount);

            Integer viewCount = viewCountCache.getRealTimeViewCount(postId);
            postStatsWebSocketHandler.broadcastViewCount(postId, viewCount);
        }

        fillRealTimeStats(post);
        post.setTagNames(tagsService.getTagNamesByPostId(postId));
        return post;
    }

    @Override
    public List<Posts> listAllWithAuthor() {
        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .orderBy("p.is_pinned", false)
                .orderBy("p.created_at", false);

        List<Posts> posts = postsMapper.selectListByQuery(wrapper);
        fillRealTimeStats(posts);
        fillTagsForPosts(posts);
        return posts;
    }

    @Override
    public Posts getPostWithAuthorById(Long postId) {
        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .where(POSTS.ID.eq(postId));

        Posts post = postsMapper.selectOneByQuery(wrapper);
        if (post != null) {
            fillRealTimeStats(post);
            fillTagsForPosts(List.of(post));
        }
        return post;
    }

    @Override
    public Page<Posts> list(int pageNumber, int pageSize) {
        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .orderBy("p.is_pinned", false)
                .orderBy("p.created_at", false);

        Page<Posts> page = postsMapper.paginate(pageNumber, pageSize, wrapper);
        fillRealTimeStats(page.getRecords());
        fillTagsForPosts(page.getRecords());
        return page;
    }

    @Override
    public Page<Posts> listByCategory(Long categoryId, int pageNumber, int pageSize) {
        List<Long> categoryIds = getCategoryIds(categoryId);

        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .orderBy("p.is_pinned", false)
                .orderBy("p.created_at", false);

        if (!categoryIds.isEmpty()) {
            wrapper.where(POSTS.CATEGORY_ID.in(categoryIds));
        }

        Page<Posts> page = postsMapper.paginate(pageNumber, pageSize, wrapper);
        fillRealTimeStats(page.getRecords());
        fillTagsForPosts(page.getRecords());
        return page;
    }

    @Override
    public Page<Posts> listPage(int pageNumber, int pageSize) {
        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .orderBy("p.is_pinned", false)
                .orderBy("p.created_at", false);

        Page<Posts> page = postsMapper.paginate(pageNumber, pageSize, wrapper);
        fillRealTimeStats(page.getRecords());
        fillTagsForPosts(page.getRecords());
        return page;
    }

    private void fillRealTimeStats(List<Posts> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        try {
            List<Long> postIds = posts.stream().map(Posts::getId).toList();

            Map<Long, Integer> viewCounts = viewCountCache.batchGetRealTimeViewCount(postIds);
            Map<Long, Integer> likeCounts = postStatsCache.batchGetRealTimeLikeCount(postIds);
            Map<Long, Integer> commentCounts = postStatsCache.batchGetRealTimeCommentCount(postIds);
            Map<Long, Integer> favoriteCounts = postStatsCache.batchGetRealTimeFavoriteCount(postIds);

            for (Posts post : posts) {
                Integer viewCount = viewCounts.get(post.getId());
                if (viewCount != null && viewCount > 0) {
                    post.setViewCount(viewCount);
                }

                Integer likeCount = likeCounts.get(post.getId());
                if (likeCount != null && likeCount > 0) {
                    post.setLikeCount(likeCount);
                }

                Integer commentCount = commentCounts.get(post.getId());
                if (commentCount != null && commentCount > 0) {
                    post.setCommentCount(commentCount);
                }

                Integer favoriteCount = favoriteCounts.get(post.getId());
                if (favoriteCount != null && favoriteCount > 0) {
                    post.setFavoriteCount(favoriteCount);
                }
            }
        } catch (Exception e) {
            log.warn("填充帖子实时统计数据失败，使用数据库原始数据: {}", e.getMessage());
        }
    }

    private void fillRealTimeStats(Posts post) {
        try {
            Integer realTimeViewCount = viewCountCache.getRealTimeViewCount(post.getId());
            if (realTimeViewCount != null && realTimeViewCount > 0) {
                post.setViewCount(realTimeViewCount);
            }

            Integer realTimeLikeCount = postStatsCache.getRealTimeLikeCount(post.getId());
            if (realTimeLikeCount != null && realTimeLikeCount > 0) {
                post.setLikeCount(realTimeLikeCount);
            }

            Integer realTimeCommentCount = postStatsCache.getRealTimeCommentCount(post.getId());
            if (realTimeCommentCount != null && realTimeCommentCount > 0) {
                post.setCommentCount(realTimeCommentCount);
            }

            Integer realTimeFavoriteCount = postStatsCache.getRealTimeFavoriteCount(post.getId());
            if (realTimeFavoriteCount != null && realTimeFavoriteCount > 0) {
                post.setFavoriteCount(realTimeFavoriteCount);
            }
        } catch (Exception e) {
            log.warn("填充帖子实时统计数据失败，使用数据库原始数据: postId={}, error={}", post.getId(), e.getMessage());
        }
    }

    private void fillTagsForPosts(List<Posts> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        try {
            List<Long> postIds = posts.stream().map(Posts::getId).toList();
            Map<Long, List<String>> tagNamesMap = tagsService.getTagNamesByPostIds(postIds);
            for (Posts post : posts) {
                post.setTagNames(tagNamesMap.getOrDefault(post.getId(), Collections.emptyList()));
            }
        } catch (Exception e) {
            log.warn("填充帖子标签数据失败: {}", e.getMessage());
            for (Posts post : posts) {
                post.setTagNames(Collections.emptyList());
            }
        }
    }

    @Override
    @Transactional
    public void likePost(Long postId) {
        Posts post = this.getById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        
        Posts update = UpdateEntity.of(Posts.class, postId);
        UpdateWrapper<Posts> wrapper = UpdateWrapper.of(update);
        wrapper.set(POSTS.LIKE_COUNT, POSTS.LIKE_COUNT.add(1));
        int updated = postsMapper.update(update);
        if (updated > 0) {
            postStatsCache.incrementLikeCount(postId);
            
            Integer likeCount = postStatsCache.getRealTimeLikeCount(postId);
            postStatsWebSocketHandler.broadcastLikeCount(postId, likeCount);
            
            log.info("帖子点赞: postId={}", postId);
        }
    }

    @Override
    @Transactional
    public void unlikePost(Long postId) {
        Posts update = UpdateEntity.of(Posts.class, postId);
        UpdateWrapper<Posts> wrapper = UpdateWrapper.of(update);
        wrapper.set(POSTS.LIKE_COUNT, POSTS.LIKE_COUNT.add(-1));
        int updated = postsMapper.update(update);
        if (updated > 0) {
            postStatsCache.decrementLikeCount(postId);
            
            Integer likeCount = postStatsCache.getRealTimeLikeCount(postId);
            postStatsWebSocketHandler.broadcastLikeCount(postId, likeCount);
            
            log.info("帖子取消点赞: postId={}", postId);
        }
    }

    @Override
    @Transactional
    public void favoritePost(Long postId) {
        Posts post = this.getById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        
        Posts update = UpdateEntity.of(Posts.class, postId);
        UpdateWrapper<Posts> wrapper = UpdateWrapper.of(update);
        wrapper.set(POSTS.FAVORITE_COUNT, POSTS.FAVORITE_COUNT.add(1));
        int updated = postsMapper.update(update);
        if (updated > 0) {
            postStatsCache.incrementFavoriteCount(postId);
            log.info("帖子收藏: postId={}", postId);
        }
    }

    @Override
    @Transactional
    public void unfavoritePost(Long postId) {
        Posts update = UpdateEntity.of(Posts.class, postId);
        UpdateWrapper<Posts> wrapper = UpdateWrapper.of(update);
        wrapper.set(POSTS.FAVORITE_COUNT, POSTS.FAVORITE_COUNT.add(-1));
        int updated = postsMapper.update(update);
        if (updated > 0) {
            postStatsCache.decrementFavoriteCount(postId);
            log.info("帖子取消收藏: postId={}", postId);
        }
    }

    @Override
    @Transactional
    public void setPinned(Long postId, boolean pinned) {
        getPostOrThrow(postId);

        Posts update = UpdateEntity.of(Posts.class, postId);
        update.setIsPinned(pinned ? PinnedStatus.PINNED : PinnedStatus.NOT_PINNED);
        postsMapper.update(update);

        log.info("帖子置顶状态更新: postId={}, pinned={}", postId, pinned);
    }

    @Override
    @Transactional
    public void setEssential(Long postId, boolean essential) {
        getPostOrThrow(postId);

        Posts update = UpdateEntity.of(Posts.class, postId);
        update.setIsEssential(essential ? EssentialStatus.ESSENTIAL : EssentialStatus.NOT_ESSENTIAL);
        postsMapper.update(update);

        log.info("帖子精华状态更新: postId={}, essential={}", postId, essential);
    }

    @Override
    public Page<Posts> listEssential(int pageNumber, int pageSize, Long categoryId) {
        List<Long> categoryIds = getCategoryIds(categoryId);

        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .where("p.is_essential = {0}", EssentialStatus.ESSENTIAL.getCode())
                .orderBy("p.is_pinned", false)
                .orderBy("p.created_at", false);

        if (!categoryIds.isEmpty()) {
            wrapper.where(POSTS.CATEGORY_ID.in(categoryIds));
        }

        Page<Posts> page = postsMapper.paginate(pageNumber, pageSize, wrapper);
        fillRealTimeStats(page.getRecords());
        fillTagsForPosts(page.getRecords());
        return page;
    }

    @Override
    public Page<Posts> listByAuthor(Long authorId, int pageNumber, int pageSize) {
        QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                .where("p.author_id = {0}", authorId)
                .orderBy("p.is_pinned", false)
                .orderBy("p.created_at", false);

        Page<Posts> page = postsMapper.paginate(pageNumber, pageSize, wrapper);
        fillRealTimeStats(page.getRecords());
        fillTagsForPosts(page.getRecords());
        return page;
    }

    private Posts getPostOrThrow(Long id) {
        Posts post = this.getById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        return post;
    }

    private List<Long> getCategoryIds(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        List<Long> categoryIds = new ArrayList<>();
        categoryIds.add(categoryId);
        
        List<Categories> childCategories = categoriesService.getChildrenByParentId(categoryId);
        for (Categories child : childCategories) {
            categoryIds.add(child.getId());
        }
        
        return categoryIds;
    }

    @Override
    public List<Posts> getRelatedPosts(Long postId, int limit) {
        Posts currentPost = this.getById(postId);
        if (currentPost == null) {
            return Collections.emptyList();
        }

        List<Posts> relatedPosts = new ArrayList<>();
        Set<Long> excludeIds = new HashSet<>();
        excludeIds.add(postId);

        List<Long> tagIds = getTagIdsByPostId(postId);
        
        if (currentPost.getCategoryId() != null) {
            QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                    .where(POSTS.CATEGORY_ID.eq(currentPost.getCategoryId()))
                    .where(POSTS.ID.notIn(excludeIds))
                    .limit(limit * 2);
            
            List<Posts> categoryPosts = postsMapper.selectListByQuery(wrapper);
            relatedPosts.addAll(categoryPosts);
            
            for (Posts post : categoryPosts) {
                excludeIds.add(post.getId());
            }
        }

        if (tagIds != null && !tagIds.isEmpty() && relatedPosts.size() < limit) {
            QueryWrapper wrapper = QueryWrapper.create()
                    .select("p.*", "u.name as author_name", "u.avatar as author_avatar")
                    .from("posts").as("p")
                    .leftJoin("users").as("u").on("p.author_id = u.id")
                    .leftJoin("post_tags").as("pt").on("p.id = pt.post_id")
                    .and("pt.tag_id IN (" + tagIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")")
                    .and("p.id NOT IN (" + excludeIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")")
                    .limit(limit * 2);
            
            List<Posts> tagPosts = postsMapper.selectListByQuery(wrapper);
            relatedPosts.addAll(tagPosts);
            
            for (Posts post : tagPosts) {
                excludeIds.add(post.getId());
            }
        }

        if (relatedPosts.size() < limit) {
            QueryWrapper wrapper = postQueryHelper.buildBaseQueryWithRelations()
                    .where(POSTS.ID.notIn(excludeIds))
                    .orderBy("RAND()")
                    .limit(limit - relatedPosts.size());
            
            List<Posts> randomPosts = postsMapper.selectListByQuery(wrapper);
            relatedPosts.addAll(randomPosts);
        }

        Collections.shuffle(relatedPosts);
        List<Posts> result = relatedPosts.stream()
                .limit(limit)
                .toList();

        fillRealTimeStats(result);
        fillTagsForPosts(result);
        
        return result;
    }

    private List<Long> getTagIdsByPostId(Long postId) {
        if (postId == null) {
            return Collections.emptyList();
        }
        
        QueryWrapper wrapper = QueryWrapper.create()
                .select("tag_id")
                .from("post_tags")
                .where("post_id = {0}", postId);
        
        List<PostTags> postTags = postsMapper.selectListByQueryAs(wrapper, 
                (Class<PostTags>)(Class<?>)PostTags.class);
        
        return postTags.stream()
                .map(PostTags::getTagId)
                .toList();
    }
}
