package com.example.schoolforum.component;

import com.example.schoolforum.mapper.PostsMapper;
import com.example.schoolforum.pojo.Categories;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.pojo.Users;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostQueryHelper {

    private final PostsMapper postsMapper;

    private static final String P = "p";
    private static final String U = "u";
    private static final String C = "c";
    private static final String PC = "pc";
    private static final String T = "t";

    public QueryWrapper baseQuery() {
        return QueryWrapper.create()
                .select(
                        P + ".*",
                        U + ".username AS author_name",
                        U + ".avatar_url AS author_avatar",
                        C + ".name AS category_name",
                        PC + ".name AS parent_category_name"
                )
                .from(Posts.class).as(P)
                .leftJoin(Users.class).as(U).on(Posts::getAuthorId, Users::getId)
                .leftJoin(Categories.class).as(C).on(Posts::getCategoryId, Categories::getId)
                .leftJoin("categories").as(PC).on(C + ".parent_id = " + PC + ".id");
    }

    public QueryWrapper queryWithTags() {
        return baseQuery()
                .select("GROUP_CONCAT(DISTINCT " + T + ".name) AS tag_names")
                .leftJoin("post_tags").as("pt").on(P + ".id = pt.post_id")
                .leftJoin("tags").as(T).on("pt.tag_id = " + T + ".id")
                .groupBy(P + ".id");
    }

    public QueryWrapper lambda() {
        return QueryWrapper.create();
    }

    public QueryWrapper whereId(Long id) {
        return QueryWrapper.create().where(Posts::getId).eq(id);
    }

    public QueryWrapper whereAuthorId(Long authorId) {
        return QueryWrapper.create().where(Posts::getAuthorId).eq(authorId);
    }

    public QueryWrapper whereCategoryId(Long categoryId) {
        return QueryWrapper.create().where(Posts::getCategoryId).eq(categoryId);
    }

    public QueryWrapper whereCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return QueryWrapper.create();
        }
        return QueryWrapper.create().where(Posts::getCategoryId).in(categoryIds);
    }

    public QueryWrapper whereTitleLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return QueryWrapper.create();
        }
        return QueryWrapper.create().where(Posts::getTitle).like(keyword);
    }

    public QueryWrapper whereContentLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return QueryWrapper.create();
        }
        return QueryWrapper.create().where(Posts::getContent).like(keyword);
    }

    public QueryWrapper whereKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return QueryWrapper.create();
        }
        return QueryWrapper.create()
                .where(Posts::getTitle).like(keyword)
                .or(Posts::getContent).like(keyword);
    }

    public QueryWrapper wherePinned() {
        return QueryWrapper.create().where(Posts::getIsPinned).eq(true);
    }

    public QueryWrapper whereEssential() {
        return QueryWrapper.create().where(Posts::getIsEssential).eq(true);
    }

    public QueryWrapper orderDefault() {
        return QueryWrapper.create()
                .orderBy(Posts::getIsPinned, false)
                .orderBy(Posts::getCreatedAt, false);
    }

    public QueryWrapper orderHot() {
        return QueryWrapper.create()
                .orderBy(Posts::getIsPinned, false)
                .orderBy(Posts::getLikeCount, false)
                .orderBy(Posts::getViewCount, false)
                .orderBy(Posts::getCommentCount, false);
    }

    public QueryWrapper orderLatest() {
        return QueryWrapper.create()
                .orderBy(Posts::getUpdatedAt, false);
    }

    public List<Posts> selectAllWithRelations() {
        return postsMapper.selectListByQuery(baseQuery().orderBy(P + ".is_pinned", false).orderBy(P + ".created_at", false));
    }

    public Posts selectByIdWithRelations(Long postId) {
        return postsMapper.selectOneByQuery(baseQuery().where(P + ".id = {0}", postId));
    }

    public Posts selectByIdWithTags(Long postId) {
        return postsMapper.selectOneByQuery(queryWithTags().where(P + ".id = {0}", postId));
    }

    public Page<Posts> paginateAll(int page, int size) {
        return postsMapper.paginate(page, size,
                baseQuery().orderBy(P + ".is_pinned", false).orderBy(P + ".created_at", false));
    }

    public Page<Posts> paginateByCategory(int page, int size, Long categoryId) {
        QueryWrapper wrapper = baseQuery();
        if (categoryId != null) {
            wrapper.where(P + ".category_id = {0}", categoryId);
        }
        return postsMapper.paginate(page, size,
                wrapper.orderBy(P + ".is_pinned", false).orderBy(P + ".created_at", false));
    }

    public Page<Posts> paginateByCategories(int page, int size, List<Long> categoryIds) {
        QueryWrapper wrapper = baseQuery();
        if (categoryIds != null && !categoryIds.isEmpty()) {
            wrapper.and(P + ".category_id IN ({0})", categoryIds.toArray());
        }
        return postsMapper.paginate(page, size,
                wrapper.orderBy(P + ".is_pinned", false).orderBy(P + ".created_at", false));
    }

    public Page<Posts> paginateHot(int page, int size) {
        return postsMapper.paginate(page, size,
                baseQuery().orderBy(P + ".is_pinned", false)
                       .orderBy(P + ".like_count", false)
                       .orderBy(P + ".view_count", false)
                       .orderBy(P + ".comment_count", false));
    }

    public Page<Posts> paginateSearch(String keyword, int page, int size) {
        QueryWrapper wrapper = baseQuery();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and("(" + P + ".title LIKE {0} OR " + P + ".content LIKE {0})",
                      "%" + keyword.trim() + "%");
        }
        return postsMapper.paginate(page, size,
                wrapper.orderBy(P + ".is_pinned", false).orderBy(P + ".created_at", false));
    }

    public Page<Posts> paginateByAuthor(Long authorId, int page, int size) {
        return postsMapper.paginate(page, size,
                baseQuery().where(P + ".author_id = {0}", authorId)
                       .orderBy(P + ".updated_at", false));
    }

    public Page<Posts> paginatePinned(int page, int size) {
        return postsMapper.paginate(page, size,
                baseQuery().where(P + ".is_pinned = 1")
                       .orderBy(P + ".created_at", false));
    }

    public Page<Posts> paginateEssential(int page, int size) {
        return postsMapper.paginate(page, size,
                baseQuery().where(P + ".is_essential = 1")
                       .orderBy(P + ".created_at", false));
    }

    public long countByCategory(Long categoryId) {
        return postsMapper.selectCountByQuery(whereCategoryId(categoryId));
    }

    public long countByAuthor(Long authorId) {
        return postsMapper.selectCountByQuery(whereAuthorId(authorId));
    }

    public long countAll() {
        return postsMapper.selectCountByQuery(QueryWrapper.create());
    }

    public QueryWrapper filterCategory(QueryWrapper wrapper, Long categoryId) {
        if (categoryId != null) {
            wrapper.and(P + ".category_id = {0}", categoryId);
        }
        return wrapper;
    }

    public QueryWrapper filterCategories(QueryWrapper wrapper, List<Long> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            wrapper.and(P + ".category_id IN ({0})", categoryIds.toArray());
        }
        return wrapper;
    }

    public QueryWrapper filterAuthor(QueryWrapper wrapper, Long authorId) {
        if (authorId != null) {
            wrapper.and(P + ".author_id = {0}", authorId);
        }
        return wrapper;
    }

    public QueryWrapper filterKeyword(QueryWrapper wrapper, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and("(" + P + ".title LIKE {0} OR " + P + ".content LIKE {0})",
                      "%" + keyword.trim() + "%");
        }
        return wrapper;
    }

    public QueryWrapper filterPinned(QueryWrapper wrapper) {
        return wrapper.and(P + ".is_pinned = 1");
    }

    public QueryWrapper filterEssential(QueryWrapper wrapper) {
        return wrapper.and(P + ".is_essential = 1");
    }

    public QueryWrapper sortDefault(QueryWrapper wrapper) {
        return wrapper.orderBy(P + ".is_pinned", false)
                     .orderBy(P + ".created_at", false);
    }

    public QueryWrapper sortHot(QueryWrapper wrapper) {
        return wrapper.orderBy(P + ".is_pinned", false)
                     .orderBy(P + ".like_count", false)
                     .orderBy(P + ".view_count", false)
                     .orderBy(P + ".comment_count", false);
    }

    public QueryWrapper sortCustom(QueryWrapper wrapper, String field, boolean asc) {
        if (field != null && !field.isEmpty()) {
            wrapper.orderBy(P + "." + field, asc);
        } else {
            sortDefault(wrapper);
        }
        return wrapper;
    }

    public QueryWrapper buildComplexQuery(Long categoryId, Long authorId, String keyword,
                                          Boolean pinnedOnly, Boolean essentialOnly,
                                          String sortBy, boolean ascending) {
        QueryWrapper wrapper = baseQuery();

        filterCategory(wrapper, categoryId);
        filterAuthor(wrapper, authorId);
        filterKeyword(wrapper, keyword);

        if (Boolean.TRUE.equals(pinnedOnly)) {
            filterPinned(wrapper);
        }
        if (Boolean.TRUE.equals(essentialOnly)) {
            filterEssential(wrapper);
        }

        if ("hot".equalsIgnoreCase(sortBy)) {
            sortHot(wrapper);
        } else {
            sortCustom(wrapper, sortBy, ascending);
        }

        return wrapper;
    }

    public Page<Posts> searchAdvanced(Long categoryId, Long authorId, String keyword,
                                       Boolean pinnedOnly, Boolean essentialOnly,
                                       String sortBy, boolean ascending,
                                       int page, int size) {
        QueryWrapper wrapper = buildComplexQuery(categoryId, authorId, keyword,
                pinnedOnly, essentialOnly, sortBy, ascending);
        return postsMapper.paginate(page, size, wrapper);
    }
    
    public Posts selectPostWithAuthorById(Long postId) {
        return selectByIdWithRelations(postId);
    }
    
    public List<Posts> selectAllPostsWithAuthor() {
        return selectAllWithRelations();
    }

    public QueryWrapper buildBaseQueryWithRelations() {
        return baseQuery();
    }
}