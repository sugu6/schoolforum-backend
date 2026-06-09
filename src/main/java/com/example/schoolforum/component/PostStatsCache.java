package com.example.schoolforum.component;

import com.example.schoolforum.constant.RedisCacheKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostStatsCache {

    private final StringRedisTemplate redisTemplate;
    private final PostViewCountCache viewCountCache;

    private static final String DECREMENT_SCRIPT =
            "local val = redis.call('GET', KEYS[1]); " +
            "if val and tonumber(val) > 0 then " +
            "  return redis.call('DECR', KEYS[1]); " +
            "else " +
            "  return 0; " +
            "end";

    private static final DefaultRedisScript<Long> DECREMENT_REDIS_SCRIPT = new DefaultRedisScript<>(DECREMENT_SCRIPT, Long.class);

    private enum StatsType {
        LIKE(
            RedisCacheKey::postLikeCountKey,
            RedisCacheKey::postLikeCountBaseKey,
            RedisCacheKey.POST_LIKE_COUNT_TTL,
            "点赞数"
        ),
        COMMENT(
            RedisCacheKey::postCommentCountKey,
            RedisCacheKey::postCommentCountBaseKey,
            RedisCacheKey.POST_COMMENT_COUNT_TTL,
            "评论数"
        ),
        FAVORITE(
            RedisCacheKey::postFavoriteCountKey,
            RedisCacheKey::postFavoriteCountBaseKey,
            RedisCacheKey.POST_FAVORITE_COUNT_TTL,
            "收藏数"
        );

        private final java.util.function.Function<Long, String> keyFunction;
        private final java.util.function.Function<Long, String> baseKeyFunction;
        private final long ttl;
        private final String displayName;

        StatsType(java.util.function.Function<Long, String> keyFunction,
                  java.util.function.Function<Long, String> baseKeyFunction,
                  long ttl,
                  String displayName) {
            this.keyFunction = keyFunction;
            this.baseKeyFunction = baseKeyFunction;
            this.ttl = ttl;
            this.displayName = displayName;
        }

        String getKey(Long postId) {
            return keyFunction.apply(postId);
        }

        String getBaseKey(Long postId) {
            return baseKeyFunction.apply(postId);
        }

        long getTtl() {
            return ttl;
        }

        String getDisplayName() {
            return displayName;
        }
    }

    private void incrementCount(Long postId, StatsType type) {
        String key = type.getKey(postId);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, type.getTtl(), TimeUnit.SECONDS);
        log.debug("帖子{}增加: postId={}", type.getDisplayName(), postId);
    }

    private void decrementCount(Long postId, StatsType type) {
        String key = type.getKey(postId);
        redisTemplate.execute(DECREMENT_REDIS_SCRIPT, Collections.singletonList(key));
        redisTemplate.expire(key, type.getTtl(), TimeUnit.SECONDS);
        log.debug("帖子{}减少: postId={}", type.getDisplayName(), postId);
    }

    private void initCountBase(Long postId, Integer baseCount, StatsType type) {
        String baseKey = type.getBaseKey(postId);
        String incrKey = type.getKey(postId);

        redisTemplate.opsForValue().set(baseKey, String.valueOf(baseCount),
                type.getTtl(), TimeUnit.SECONDS);

        redisTemplate.opsForValue().set(incrKey, "0", type.getTtl(), TimeUnit.SECONDS);
    }

    private Integer getRealTimeCount(Long postId, StatsType type) {
        String baseKey = type.getBaseKey(postId);
        String incrKey = type.getKey(postId);

        String baseCount = redisTemplate.opsForValue().get(baseKey);
        String incrCount = redisTemplate.opsForValue().get(incrKey);

        int base = baseCount != null ? Integer.parseInt(baseCount) : 0;
        int incr = incrCount != null ? Integer.parseInt(incrCount) : 0;

        return base + incr;
    }

    private Map<Long, Integer> batchGetRealTimeCount(List<Long> postIds, StatsType type) {
        return batchGetRealTimeCount(postIds, type::getBaseKey, type::getKey);
    }

    private Long getCountIncrement(Long postId, StatsType type) {
        String key = type.getKey(postId);
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0L;
    }

    private void resetCountIncrement(Long postId, StatsType type) {
        String key = type.getKey(postId);
        redisTemplate.opsForValue().set(key, "0", type.getTtl(), TimeUnit.SECONDS);
    }

    public void incrementLikeCount(Long postId) {
        incrementCount(postId, StatsType.LIKE);
    }

    public void decrementLikeCount(Long postId) {
        decrementCount(postId, StatsType.LIKE);
    }

    public void initLikeCountBase(Long postId, Integer baseCount) {
        initCountBase(postId, baseCount, StatsType.LIKE);
    }

    public Integer getRealTimeLikeCount(Long postId) {
        return getRealTimeCount(postId, StatsType.LIKE);
    }

    public Map<Long, Integer> batchGetRealTimeLikeCount(List<Long> postIds) {
        return batchGetRealTimeCount(postIds, StatsType.LIKE);
    }

    public Long getLikeCountIncrement(Long postId) {
        return getCountIncrement(postId, StatsType.LIKE);
    }

    public void resetLikeCountIncrement(Long postId) {
        resetCountIncrement(postId, StatsType.LIKE);
    }

    public void incrementCommentCount(Long postId) {
        incrementCount(postId, StatsType.COMMENT);
    }

    public void decrementCommentCount(Long postId) {
        decrementCount(postId, StatsType.COMMENT);
    }

    public void initCommentCountBase(Long postId, Integer baseCount) {
        initCountBase(postId, baseCount, StatsType.COMMENT);
    }

    public Integer getRealTimeCommentCount(Long postId) {
        return getRealTimeCount(postId, StatsType.COMMENT);
    }

    public Map<Long, Integer> batchGetRealTimeCommentCount(List<Long> postIds) {
        return batchGetRealTimeCount(postIds, StatsType.COMMENT);
    }

    public Long getCommentCountIncrement(Long postId) {
        return getCountIncrement(postId, StatsType.COMMENT);
    }

    public void resetCommentCountIncrement(Long postId) {
        resetCountIncrement(postId, StatsType.COMMENT);
    }

    public void incrementFavoriteCount(Long postId) {
        incrementCount(postId, StatsType.FAVORITE);
    }

    public void decrementFavoriteCount(Long postId) {
        decrementCount(postId, StatsType.FAVORITE);
    }

    public void initFavoriteCountBase(Long postId, Integer baseCount) {
        initCountBase(postId, baseCount, StatsType.FAVORITE);
    }

    public Integer getRealTimeFavoriteCount(Long postId) {
        return getRealTimeCount(postId, StatsType.FAVORITE);
    }

    public Map<Long, Integer> batchGetRealTimeFavoriteCount(List<Long> postIds) {
        return batchGetRealTimeCount(postIds, StatsType.FAVORITE);
    }

    public Long getFavoriteCountIncrement(Long postId) {
        return getCountIncrement(postId, StatsType.FAVORITE);
    }

    public void resetFavoriteCountIncrement(Long postId) {
        resetCountIncrement(postId, StatsType.FAVORITE);
    }

    public void initPostStats(Long postId, Integer viewCount, Integer likeCount, Integer commentCount, Integer favoriteCount) {
        viewCountCache.initBaseViewCount(postId, viewCount);
        initLikeCountBase(postId, likeCount);
        initCommentCountBase(postId, commentCount);
        initFavoriteCountBase(postId, favoriteCount);
    }

    private Map<Long, Integer> batchGetRealTimeCount(List<Long> postIds,
                                                      java.util.function.Function<Long, String> baseKeyFunc,
                                                      java.util.function.Function<Long, String> incrKeyFunc) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Integer> result = new HashMap<>();

        List<String> baseKeys = postIds.stream().map(baseKeyFunc).toList();
        List<String> incrKeys = postIds.stream().map(incrKeyFunc).toList();

        List<String> baseValues = redisTemplate.opsForValue().multiGet(baseKeys);
        List<String> incrValues = redisTemplate.opsForValue().multiGet(incrKeys);

        for (int i = 0; i < postIds.size(); i++) {
            Long postId = postIds.get(i);
            int base = 0;
            int incr = 0;

            if (baseValues != null && i < baseValues.size() && baseValues.get(i) != null) {
                base = Integer.parseInt(baseValues.get(i));
            }

            if (incrValues != null && i < incrValues.size() && incrValues.get(i) != null) {
                incr = Integer.parseInt(incrValues.get(i));
            }

            result.put(postId, base + incr);
        }

        return result;
    }
}