package com.example.schoolforum.service.impl;

import com.example.schoolforum.component.PostQueryHelper;
import com.example.schoolforum.component.PostStatsCache;
import com.example.schoolforum.component.PostViewCountCache;
import com.example.schoolforum.enums.ActiveStatus;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.PostsMapper;
import com.example.schoolforum.mapper.UsersMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.pojo.document.PostDocument;
import com.example.schoolforum.pojo.document.UserDocument;
import com.example.schoolforum.pojo.dto.CombinedSearchResult;
import com.example.schoolforum.pojo.dto.KeywordSuggestion;
import com.example.schoolforum.pojo.dto.PostSearchDocument;
import com.example.schoolforum.pojo.dto.SearchResult;
import com.example.schoolforum.pojo.dto.UserSearchDocument;
import com.example.schoolforum.service.SearchService;
import com.manticoresearch.client.ApiException;
import com.manticoresearch.client.api.IndexApi;
import com.manticoresearch.client.api.SearchApi;
import com.manticoresearch.client.api.UtilsApi;
import com.manticoresearch.client.model.DeleteDocumentRequest;
import com.manticoresearch.client.model.InsertDocumentRequest;
import com.manticoresearch.client.model.SearchRequest;
import com.manticoresearch.client.model.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final IndexApi indexApi;
    private final SearchApi searchApi;
    private final UtilsApi utilsApi;
    private final PostQueryHelper postQueryHelper;
    private final PostsMapper postsMapper;
    private final UsersMapper usersMapper;
    private final PostStatsCache postStatsCache;
    private final PostViewCountCache viewCountCache;

    @Override
    public void deletePost(Long postId) {
        try {
            DeleteDocumentRequest deleteRequest = new DeleteDocumentRequest();
            deleteRequest.index(PostDocument.INDEX_NAME).setId(postId);
            indexApi.delete(deleteRequest);
        } catch (Exception e) {
            log.error("Failed to delete post {}: {}", postId, e.getMessage(), e);
            throw new RuntimeException("删除帖子搜索索引失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void indexUser(UserSearchDocument document) {
        try {
            InsertDocumentRequest docRequest = new InsertDocumentRequest();
            Map<String, Object> doc = new HashMap<>();
            doc.put("username", document.getUsername());
            doc.put("avatar_url", document.getAvatarUrl());
            doc.put("bio", document.getBio());
            doc.put("role", toRoleInt(document.getRole()));
            doc.put("is_active", toActiveBool(document.getIsActive()));
            doc.put("created_at", parseTimestamp(document.getCreatedAt()));
            docRequest.index(UserDocument.INDEX_NAME).id(document.getId()).setDoc(doc);
            indexApi.replace(docRequest);
        } catch (Exception e) {
            // 写索引是尽力而为，搜索引擎不可用时不阻塞用户更新/注册等业务
            log.error("Failed to index user {}: {}", document.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void deleteUser(Long userId) {
        try {
            DeleteDocumentRequest deleteRequest = new DeleteDocumentRequest();
            deleteRequest.index(UserDocument.INDEX_NAME).setId(userId);
            indexApi.delete(deleteRequest);
        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("删除用户搜索索引失败: " + e.getMessage(), e);
        }
    }

    @Override
    public CombinedSearchResult search(String query, int page, int pageSize) {
        SearchResult<PostSearchDocument> postResult = searchPostsInternal(query, page, pageSize);
        SearchResult<UserSearchDocument> userResult = searchUsersInternal(query, page, pageSize);

        return CombinedSearchResult.builder()
                .posts(postResult)
                .users(userResult)
                .build();
    }

    @Override
    public List<KeywordSuggestion> getKeywordSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String keyword = prefix.trim();
        try {
            // 纯数据库实现：帖子标题 + 已激活用户名前缀匹配，不依赖 Manticore 索引/建表
            String likePattern = escapeLikePattern(keyword);
            // MyBatis-Flex: likeLeft 生成 LIKE '值%'（前缀匹配），likeRight 是 LIKE '%值'（后缀）
            QueryWrapper titleWrapper = QueryWrapper.create()
                    .select("DISTINCT title")
                    .from(Posts.class)
                    .likeLeft("title", likePattern)
                    .orderBy("title", true)
                    .limit(limit);
            List<String> titles = postsMapper.selectListByQueryAs(titleWrapper, String.class);

            QueryWrapper userWrapper = QueryWrapper.create()
                    .select("DISTINCT username")
                    .from(Users.class)
                    .likeLeft("username", likePattern)
                    .where(Users::getIsActive).eq(ActiveStatus.ACTIVE)
                    .orderBy("username", true)
                    .limit(limit);
            List<String> usernames = usersMapper.selectListByQueryAs(userWrapper, String.class);

            return Stream.concat(titles.stream(), usernames.stream())
                    .distinct()
                    .limit(limit)
                    .map(key -> KeywordSuggestion.builder()
                            .keyword(key)
                            .count(0L)
                            .score(0.0)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取搜索联想词失败: prefix={}, error={}", keyword, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 转义 LIKE 通配符，避免用户输入 % _ \ 影响匹配语义。
     */
    private String escapeLikePattern(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Override
    public void syncAllPosts() {
        try {
            createPostsIndex();

            List<Posts> posts = postQueryHelper.selectAllWithRelations();
            for (Posts post : posts) {
                PostDocument doc = PostDocument.fromEntity(post);
                insertPostDocument(doc);
            }
            log.info("Successfully synced {} posts to Manticore Search", posts.size());
        } catch (Exception e) {
            log.error("Failed to sync posts: {}", e.getMessage(), e);
            throw new RuntimeException("帖子索引同步失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void syncAllUsers() {
        try {
            createUsersIndex();

            List<Users> users = usersMapper.selectAll();
            for (Users user : users) {
                UserDocument doc = UserDocument.fromEntity(user);
                insertUserDocument(doc);
            }
            log.info("Successfully synced {} users to Manticore Search", users.size());
        } catch (Exception e) {
            log.error("Failed to sync users: {}", e.getMessage(), e);
            throw new RuntimeException("用户索引同步失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void indexPostById(Long postId) {
        Posts post = postQueryHelper.selectByIdWithRelations(postId);
        if (post != null) {
            PostDocument doc = PostDocument.fromEntity(post);
            insertPostDocument(doc);
        }
    }

    @Override
    public void deleteAllIndexes() {
        try {
            utilsApi.sql("DROP TABLE IF EXISTS " + PostDocument.INDEX_NAME, true);
            utilsApi.sql("DROP TABLE IF EXISTS " + UserDocument.INDEX_NAME, true);
            log.info("Deleted all search indexes");
        } catch (Exception e) {
            log.error("Failed to delete indexes: {}", e.getMessage(), e);
            // 失败必须抛出，避免"删了一半"后继续同步造成空索引
            throw new RuntimeException("删除搜索索引失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void flushAllIndexes() {
        try {
            utilsApi.sql("FLUSH TABLE " + PostDocument.INDEX_NAME, true);
            utilsApi.sql("FLUSH TABLE " + UserDocument.INDEX_NAME, true);
            log.info("Manticore indexes flushed to disk");
        } catch (Exception e) {
            log.warn("FLUSH TABLE 失败: {}", e.getMessage());
        }
    }

    @Override
    public long getPostsCollectionCount() {
        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setIndex(PostDocument.INDEX_NAME);
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("match_all", null);
            searchRequest.setQuery(queryMap);
            searchRequest.setLimit(0);
            SearchResponse response = searchApi.search(searchRequest);
            return response.getHits().getTotal() != null ? response.getHits().getTotal() : 0L;
        } catch (Exception e) {
            log.error("Failed to get posts count: {}", e.getMessage());
            return 0L;
        }
    }

    @Override
    public long getUsersCollectionCount() {
        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setIndex(UserDocument.INDEX_NAME);
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("match_all", null);
            searchRequest.setQuery(queryMap);
            searchRequest.setLimit(0);
            SearchResponse response = searchApi.search(searchRequest);
            return response.getHits().getTotal() != null ? response.getHits().getTotal() : 0L;
        } catch (Exception e) {
            log.error("Failed to get users count: {}", e.getMessage());
            return 0L;
        }
    }

    private void createPostsIndex() throws ApiException {
        utilsApi.sql("DROP TABLE IF EXISTS " + PostDocument.INDEX_NAME, true);
        utilsApi.sql(
                "CREATE TABLE " + PostDocument.INDEX_NAME + " ("
                        + "author_id BIGINT, "
                        + "author_name TEXT, "
                        + "author_avatar STRING, "
                        + "title TEXT, "
                        + "content TEXT, "
                        + "category_id BIGINT, "
                        + "category_name STRING, "
                        + "parent_category_name STRING, "
                        + "tags JSON, "
                        + "like_count INTEGER, "
                        + "view_count INTEGER, "
                        + "comment_count INTEGER, "
                        + "favorite_count INTEGER, "
                        + "cover_image STRING, "
                        + "is_pinned INTEGER, "
                        + "is_essential INTEGER, "
                        + "created_at BIGINT, "
                        + "updated_at BIGINT"
                        + ") charset_table = '0..9, A..Z->a..z, _, a..z, chinese' morphology = 'icu_chinese'"
                        + " rt_flush_period = '300'",
                true);
        log.info("Created posts index with Chinese+English charset and ICU morphology");
    }

    private void createUsersIndex() throws ApiException {
        utilsApi.sql("DROP TABLE IF EXISTS " + UserDocument.INDEX_NAME, true);
        utilsApi.sql(
                "CREATE TABLE " + UserDocument.INDEX_NAME + " ("
                        + "username TEXT, "
                        + "avatar_url STRING, "
                        + "bio TEXT, "
                        + "role INTEGER, "
                        + "is_active INTEGER, "
                        + "created_at BIGINT"
                        + ") charset_table = '0..9, A..Z->a..z, _, a..z, chinese' morphology = 'icu_chinese'"
                        + " rt_flush_period = '300'",
                true);
        log.info("Created users index with Chinese+English charset and ICU morphology");
    }

    private void insertPostDocument(PostDocument doc) {
        try {
            InsertDocumentRequest docRequest = new InsertDocumentRequest();
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("author_id", doc.getAuthorId());
            docMap.put("author_name", doc.getAuthorName());
            docMap.put("author_avatar", doc.getAuthorAvatar());
            docMap.put("title", doc.getTitle());
            docMap.put("content", doc.getContent());
            docMap.put("category_id", doc.getCategoryId());
            docMap.put("category_name", doc.getCategoryName());
            docMap.put("parent_category_name", doc.getParentCategoryName());
            docMap.put("tags", doc.getTags());
            docMap.put("like_count", doc.getLikeCount());
            docMap.put("view_count", doc.getViewCount());
            docMap.put("comment_count", doc.getCommentCount());
            docMap.put("favorite_count", doc.getFavoriteCount());
            docMap.put("cover_image", doc.getCoverImage());
            docMap.put("is_pinned", doc.getIsPinned());
            docMap.put("is_essential", doc.getIsEssential());
            docMap.put("created_at", doc.getCreatedAt());
            docMap.put("updated_at", doc.getUpdatedAt());
            docRequest.index(PostDocument.INDEX_NAME).id(doc.getId()).setDoc(docMap);
            indexApi.replace(docRequest);
        } catch (Exception e) {
            log.error("Failed to insert post {}: {}", doc.getId(), e.getMessage(), e);
            // 同步索引必须失败即停，避免静默产生空索引
            throw new RuntimeException("写入帖子搜索索引失败: " + e.getMessage(), e);
        }
    }

    private void insertUserDocument(UserDocument doc) {
        try {
            InsertDocumentRequest docRequest = new InsertDocumentRequest();
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("username", doc.getUsername());
            docMap.put("avatar_url", doc.getAvatarUrl());
            docMap.put("bio", doc.getBio());
            docMap.put("role", doc.getRole());
            docMap.put("is_active", doc.getIsActive());
            docMap.put("created_at", doc.getCreatedAt());
            docRequest.index(UserDocument.INDEX_NAME).id(doc.getId()).setDoc(docMap);
            indexApi.replace(docRequest);
        } catch (Exception e) {
            log.error("Failed to insert user {}: {}", doc.getId(), e.getMessage(), e);
            throw new RuntimeException("写入用户搜索索引失败: " + e.getMessage(), e);
        }
    }

    private SearchResult<PostSearchDocument> searchPostsInternal(String query, int page, int pageSize) {
        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setIndex(PostDocument.INDEX_NAME);

            Map<String, Object> matchQuery = new HashMap<>();
            matchQuery.put("title,content,author_name", query);
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("match", matchQuery);
            searchRequest.setQuery(queryMap);

            searchRequest.setLimit(pageSize);
            searchRequest.setOffset((page - 1) * pageSize);

            SearchResponse response = searchApi.search(searchRequest);
            Long totalHits = response.getHits().getTotal() != null ? response.getHits().getTotal() : 0L;

            List<PostSearchDocument> hits = response.getHits().getHits().stream()
                    .map(rawHit -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> hit = (Map<String, Object>) rawHit;
                        Map<String, Object> source = hit.get("_source") instanceof Map
                                ? (Map<String, Object>) hit.get("_source")
                                : new HashMap<>();
                        return PostSearchDocument.builder()
                                .id(toLong(hit.get("_id")))
                                .authorId(toLong(source.get("author_id")))
                                .authorName((String) source.get("author_name"))
                                .authorAvatar((String) source.get("author_avatar"))
                                .title((String) source.get("title"))
                                .content((String) source.get("content"))
                                .coverImage((String) source.get("cover_image"))
                                .categoryId(toLong(source.get("category_id")))
                                .categoryName((String) source.get("category_name"))
                                .parentCategoryName((String) source.get("parent_category_name"))
                                .tagNames(toStringList(source.get("tags")))
                                .likeCount(toInteger(source.get("like_count")))
                                .commentCount(toInteger(source.get("comment_count")))
                                .favoriteCount(toInteger(source.get("favorite_count")))
                                .viewCount(toInteger(source.get("view_count")))
                                .isPinned(toBoolean(source.get("is_pinned")) ? "PINNED" : "NOT_PINNED")
                                .isEssential(toBoolean(source.get("is_essential")) ? "ESSENTIAL" : "NOT_ESSENTIAL")
                                .createdAt(formatTimestamp(toLong(source.get("created_at"))))
                                .updatedAt(formatTimestamp(toLong(source.get("updated_at"))))
                                .build();
                    })
                    .collect(Collectors.toList());

            // 用实时计数回填，避免索引快照里的过时统计
            fillRealTimeStatsForSearch(hits);

            return SearchResult.<PostSearchDocument>builder()
                    .query(query)
                    .totalHits(totalHits)
                    .hitsPerPage(pageSize)
                    .page(page)
                    .totalPages((int) Math.ceil((double) totalHits / pageSize))
                    .hits(hits)
                    .build();
        } catch (Exception e) {
            log.error("Failed to search posts: {}", e.getMessage(), e);
            throw new BusinessException("搜索服务暂不可用，请稍后重试");
        }
    }

    private SearchResult<UserSearchDocument> searchUsersInternal(String query, int page, int pageSize) {
        String keyword = query == null ? "" : query.trim();
        try {
            if (keyword.isEmpty()) {
                return SearchResult.<UserSearchDocument>builder()
                        .query(query)
                        .totalHits(0L)
                        .hitsPerPage(pageSize)
                        .page(page)
                        .totalPages(0)
                        .hits(new ArrayList<>())
                        .build();
            }

            // 模糊查询：username/bio 包含关键词（LIKE '%关键词%'），仅已激活用户
            String containsPattern = "%" + escapeLikePattern(keyword) + "%";
            QueryWrapper wrapper = QueryWrapper.create();
            wrapper.where(Users::getIsActive).eq(ActiveStatus.ACTIVE);
            wrapper.and("(username LIKE ? OR bio LIKE ?)", containsPattern, containsPattern);
            wrapper.orderBy("id", true);
            Page<Users> userPage = usersMapper.paginate(page, pageSize, wrapper);

            List<UserSearchDocument> hits = userPage.getRecords().stream()
                    .map(u -> UserSearchDocument.builder()
                            .id(u.getId())
                            .username(u.getUsername())
                            .email(null)
                            .avatarUrl(u.getAvatarUrl())
                            .bio(u.getBio())
                            .role(u.getRole() != null ? u.getRole().name() : "USER")
                            .isActive(u.getIsActive() != null && u.getIsActive() == ActiveStatus.ACTIVE ? 1 : 0)
                            .build())
                    .collect(Collectors.toList());

            return SearchResult.<UserSearchDocument>builder()
                    .query(query)
                    .totalHits(userPage.getTotalRow())
                    .hitsPerPage(pageSize)
                    .page(page)
                    .totalPages((int) userPage.getTotalPage())
                    .hits(hits)
                    .build();
        } catch (Exception e) {
            log.error("Failed to search users: {}", e.getMessage(), e);
            throw new BusinessException("搜索服务暂不可用，请稍后重试");
        }
    }

    private void fillRealTimeStatsForSearch(List<PostSearchDocument> hits) {
        if (hits == null || hits.isEmpty()) {
            return;
        }
        try {
            List<Long> postIds = hits.stream()
                    .map(PostSearchDocument::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (postIds.isEmpty()) {
                return;
            }
            Map<Long, Integer> viewCounts = viewCountCache.batchGetRealTimeViewCount(postIds);
            Map<Long, Integer> likeCounts = postStatsCache.batchGetRealTimeLikeCount(postIds);
            Map<Long, Integer> commentCounts = postStatsCache.batchGetRealTimeCommentCount(postIds);
            Map<Long, Integer> favoriteCounts = postStatsCache.batchGetRealTimeFavoriteCount(postIds);
            for (PostSearchDocument hit : hits) {
                Long id = hit.getId();
                if (id == null) {
                    continue;
                }
                applyPositive(viewCounts.get(id), hit::setViewCount);
                applyPositive(likeCounts.get(id), hit::setLikeCount);
                applyPositive(commentCounts.get(id), hit::setCommentCount);
                applyPositive(favoriteCounts.get(id), hit::setFavoriteCount);
            }
        } catch (Exception e) {
            log.warn("回填搜索结果实时统计失败: {}", e.getMessage());
        }
    }

    private void applyPositive(Integer value, java.util.function.IntConsumer setter) {
        if (value != null && value > 0) {
            setter.accept(value);
        }
    }

    private int toRoleInt(String role) {
        if (role == null) return 2;
        return switch (role) {
            case "SUPER_ADMIN" -> 0;
            case "ADMIN" -> 1;
            default -> 2;
        };
    }

    private String toRoleString(Integer role) {
        if (role == null) return "USER";
        return switch (role) {
            case 0 -> "SUPER_ADMIN";
            case 1 -> "ADMIN";
            default -> "USER";
        };
    }

    private boolean toActiveBool(Integer isActive) {
        return isActive != null && isActive == 1;
    }

    private Long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return null;
        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatTimestamp(Long epochMillis) {
        if (epochMillis == null) return null;
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value == null) return null;
        if (value instanceof List) return (List<String>) value;
        return new ArrayList<>();
    }
}
