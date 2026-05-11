package com.example.schoolforum.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.example.schoolforum.component.PostQueryHelper;
import com.example.schoolforum.mapper.UsersMapper;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.pojo.document.PopularQueryDocument;
import com.example.schoolforum.pojo.document.PostDocument;
import com.example.schoolforum.pojo.document.UserDocument;
import com.example.schoolforum.pojo.dto.CombinedSearchResult;
import com.example.schoolforum.pojo.dto.KeywordSuggestion;
import com.example.schoolforum.pojo.dto.PostSearchDocument;
import com.example.schoolforum.pojo.dto.SearchResult;
import com.example.schoolforum.pojo.dto.UserSearchDocument;
import com.example.schoolforum.repository.PostSearchRepository;
import com.example.schoolforum.repository.UserSearchRepository;
import com.example.schoolforum.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final PostSearchRepository postSearchRepository;
    private final UserSearchRepository userSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final PostQueryHelper postQueryHelper;
    private final UsersMapper usersMapper;

    @Override
    public void deletePost(Long postId) {
        postSearchRepository.deleteById(postId);
    }

    @Override
    public void indexUser(UserSearchDocument document) {
        UserDocument doc = UserDocument.builder()
                .id(document.getId())
                .username(document.getUsername())
                .email(document.getEmail())
                .avatarUrl(document.getAvatarUrl())
                .bio(document.getBio())
                .role(toRoleInt(document.getRole()))
                .isActive(toActiveBool(document.getIsActive()))
                .createdAt(parseTimestamp(document.getCreatedAt()))
                .build();
        userSearchRepository.save(doc);
    }

    @Override
    public void deleteUser(Long userId) {
        userSearchRepository.deleteById(userId);
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
        try {
            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(q -> q
                            .prefix(p -> p
                                    .field("keyword")
                                    .value(prefix)))
                    .withSort(s -> s.field(f -> f.field("count").order(SortOrder.Desc)))
                    .withMaxResults(limit)
                    .build();

            SearchHits<PopularQueryDocument> hits = elasticsearchOperations.search(nativeQuery, PopularQueryDocument.class);

            return hits.getSearchHits().stream()
                    .map(hit -> {
                        PopularQueryDocument doc = hit.getContent();
                        return KeywordSuggestion.builder()
                                .keyword(doc.getKeyword())
                                .count(doc.getCount())
                                .score((double) hit.getScore())
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Failed to get keyword suggestions: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void syncAllPosts() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(PostDocument.class);
            indexOps.delete();
            indexOps.createWithMapping();
            log.info("Created posts index with IK mapping");

            List<Posts> posts = postQueryHelper.selectAllWithRelations();
            List<PostDocument> documents = posts.stream()
                    .map(PostDocument::fromEntity)
                    .collect(Collectors.toList());
            postSearchRepository.saveAll(documents);
            log.info("Successfully synced {} posts to Elasticsearch", documents.size());
        } catch (Exception e) {
            log.error("Failed to sync posts: {}", e.getMessage(), e);
            throw new RuntimeException("帖子索引同步失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void syncAllUsers() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(UserDocument.class);
            indexOps.delete();
            indexOps.createWithMapping();
            log.info("Created users index with IK mapping");

            List<Users> users = usersMapper.selectAll();
            List<UserDocument> documents = users.stream()
                    .map(UserDocument::fromEntity)
                    .collect(Collectors.toList());
            userSearchRepository.saveAll(documents);
            log.info("Successfully synced {} users to Elasticsearch", documents.size());
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
            postSearchRepository.save(doc);
        }
    }

    @Override
    public void deleteAllIndexes() {
        elasticsearchOperations.indexOps(PostDocument.class).delete();
        elasticsearchOperations.indexOps(UserDocument.class).delete();
        log.info("Deleted all search indexes");
    }

    @Override
    public long getPostsCollectionCount() {
        return postSearchRepository.count();
    }

    @Override
    public long getUsersCollectionCount() {
        return userSearchRepository.count();
    }

    private SearchResult<PostSearchDocument> searchPostsInternal(String query, int page, int pageSize) {
        try {
            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(q -> q
                            .multiMatch(mm -> mm
                                    .query(query)
                                    .fields("title", "content", "author_name")
                                    .type(TextQueryType.BoolPrefix)))
                    .withPageable(PageRequest.of(page - 1, pageSize))
                    .build();

            SearchHits<PostDocument> searchHits = elasticsearchOperations.search(nativeQuery, PostDocument.class);

            List<PostSearchDocument> hits = searchHits.getSearchHits().stream()
                    .map(hit -> {
                        PostDocument doc = hit.getContent();
                        return PostSearchDocument.builder()
                                .id(doc.getId())
                                .authorId(doc.getAuthorId())
                                .authorName(doc.getAuthorName())
                                .authorAvatar(doc.getAuthorAvatar())
                                .title(doc.getTitle())
                                .content(doc.getContent())
                                .coverImage(doc.getCoverImage())
                                .categoryId(doc.getCategoryId())
                                .categoryName(doc.getCategoryName())
                                .parentCategoryName(doc.getParentCategoryName())
                                .tagNames(doc.getTags())
                                .likeCount(doc.getLikeCount())
                                .commentCount(doc.getCommentCount())
                                .favoriteCount(doc.getFavoriteCount())
                                .viewCount(doc.getViewCount())
                                .isPinned(doc.getIsPinned() != null && doc.getIsPinned() ? "PINNED" : "NOT_PINNED")
                                .isEssential(doc.getIsEssential() != null && doc.getIsEssential() ? "ESSENTIAL" : "NOT_ESSENTIAL")
                                .createdAt(formatTimestamp(doc.getCreatedAt()))
                                .updatedAt(formatTimestamp(doc.getUpdatedAt()))
                                .build();
                    })
                    .collect(Collectors.toList());

            long totalHits = searchHits.getTotalHits();

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
            return SearchResult.<PostSearchDocument>builder().query(query).hits(new ArrayList<>()).build();
        }
    }

    private SearchResult<UserSearchDocument> searchUsersInternal(String query, int page, int pageSize) {
        try {
            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(q -> q
                            .multiMatch(mm -> mm
                                    .query(query)
                                    .fields("username", "email", "bio")
                                    .type(TextQueryType.BoolPrefix)))
                    .withPageable(PageRequest.of(page - 1, pageSize))
                    .build();

            SearchHits<UserDocument> searchHits = elasticsearchOperations.search(nativeQuery, UserDocument.class);

            List<UserSearchDocument> hits = searchHits.getSearchHits().stream()
                    .map(hit -> {
                        UserDocument doc = hit.getContent();
                        return UserSearchDocument.builder()
                                .id(doc.getId())
                                .username(doc.getUsername())
                                .email(doc.getEmail())
                                .avatarUrl(doc.getAvatarUrl())
                                .bio(doc.getBio())
                                .role(toRoleString(doc.getRole()))
                                .isActive(doc.getIsActive() != null && doc.getIsActive() ? 1 : 0)
                                .build();
                    })
                    .collect(Collectors.toList());

            long totalHits = searchHits.getTotalHits();

            return SearchResult.<UserSearchDocument>builder()
                    .query(query)
                    .totalHits(totalHits)
                    .hitsPerPage(pageSize)
                    .page(page)
                    .totalPages((int) Math.ceil((double) totalHits / pageSize))
                    .hits(hits)
                    .build();
        } catch (Exception e) {
            log.error("Failed to search users: {}", e.getMessage(), e);
            return SearchResult.<UserSearchDocument>builder().query(query).hits(new ArrayList<>()).build();
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
}