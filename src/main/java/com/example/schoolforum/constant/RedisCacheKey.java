package com.example.schoolforum.constant;

public final class RedisCacheKey {

    private RedisCacheKey() {
    }

    public static final String POST_VIEW_COUNT = "post:view_count:";
    public static final String POST_VIEW_COUNT_BASE = "post:view_count_base:";
    public static final String POST_LIKE_COUNT = "post:like_count:";
    public static final String POST_LIKE_COUNT_BASE = "post:like_count_base:";
    public static final String POST_COMMENT_COUNT = "post:comment_count:";
    public static final String POST_COMMENT_COUNT_BASE = "post:comment_count_base:";
    public static final String POST_FAVORITE_COUNT = "post:favorite_count:";
    public static final String POST_FAVORITE_COUNT_BASE = "post:favorite_count_base:";
    public static final String HOT_POSTS = "posts:hot:";
    public static final String HOT_RANK_ZSET = "posts:hot_rank:";
    public static final String LATEST_POSTS = "posts:latest:";
    public static final String CATEGORIES = "posts:categories";
    public static final String USER_INFO = "user:info:";

    public static final long HOT_POSTS_TTL = 300;
    public static final long HOT_RANK_ZSET_TTL = 600;
    public static final long LATEST_POSTS_TTL = 300;
    public static final long CATEGORIES_TTL = 3600;
    public static final long USER_INFO_TTL = 1800;
    public static final long POST_VIEW_COUNT_TTL = 600;
    public static final long POST_LIKE_COUNT_TTL = 600;
    public static final long POST_COMMENT_COUNT_TTL = 600;
    public static final long POST_FAVORITE_COUNT_TTL = 600;

    public static String postViewCountKey(Long postId) {
        return POST_VIEW_COUNT + postId;
    }

    public static String postViewCountBaseKey(Long postId) {
        return POST_VIEW_COUNT_BASE + postId;
    }

    public static String postLikeCountKey(Long postId) {
        return POST_LIKE_COUNT + postId;
    }

    public static String postLikeCountBaseKey(Long postId) {
        return POST_LIKE_COUNT_BASE + postId;
    }

    public static String postCommentCountKey(Long postId) {
        return POST_COMMENT_COUNT + postId;
    }

    public static String postCommentCountBaseKey(Long postId) {
        return POST_COMMENT_COUNT_BASE + postId;
    }

    public static String postFavoriteCountKey(Long postId) {
        return POST_FAVORITE_COUNT + postId;
    }

    public static String postFavoriteCountBaseKey(Long postId) {
        return POST_FAVORITE_COUNT_BASE + postId;
    }

    public static String hotPostsKey(int limit) {
        return HOT_POSTS + limit;
    }

    public static String hotRankZSetKey(String window) {
        return HOT_RANK_ZSET + window;
    }

    public static String latestPostsKey(int limit) {
        return LATEST_POSTS + limit;
    }

    public static String userInfoKey(Long userId) {
        return USER_INFO + userId;
    }
}
