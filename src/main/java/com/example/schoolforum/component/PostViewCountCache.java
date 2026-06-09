package com.example.schoolforum.component;

import com.example.schoolforum.constant.RedisCacheKey;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostViewCountCache {

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> getAndResetScript;

    @PostConstruct
    public void init() {
        getAndResetScript = new DefaultRedisScript<>();
        getAndResetScript.setScriptText("""
            local current = redis.call('GET', KEYS[1])
            if current and tonumber(current) > 0 then
                redis.call('SET', KEYS[1], '0')
                redis.call('EXPIRE', KEYS[1], ARGV[1])
                return current
            end
            return 0
            """);
        getAndResetScript.setResultType(Long.class);
    }

    public Long incrementViewCount(Long postId) {
        String key = RedisCacheKey.postViewCountKey(postId);
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, RedisCacheKey.POST_VIEW_COUNT_TTL, TimeUnit.SECONDS);
        log.debug("帖子浏览量增加: postId={}, viewCount={}", postId, count);
        return count;
    }

    public void initBaseViewCount(Long postId, Integer baseCount) {
        String key = RedisCacheKey.postViewCountBaseKey(postId);
        redisTemplate.opsForValue().set(key, String.valueOf(baseCount), 
                RedisCacheKey.POST_VIEW_COUNT_TTL, TimeUnit.SECONDS);
    }

    public Integer getRealTimeViewCount(Long postId) {
        String baseKey = RedisCacheKey.postViewCountBaseKey(postId);
        String incrKey = RedisCacheKey.postViewCountKey(postId);
        
        String baseCount = redisTemplate.opsForValue().get(baseKey);
        String incrCount = redisTemplate.opsForValue().get(incrKey);
        
        if (baseCount != null) {
            redisTemplate.expire(baseKey, RedisCacheKey.POST_VIEW_COUNT_TTL, TimeUnit.SECONDS);
        }
        if (incrCount != null) {
            redisTemplate.expire(incrKey, RedisCacheKey.POST_VIEW_COUNT_TTL, TimeUnit.SECONDS);
        }
        
        int base = baseCount != null ? Integer.parseInt(baseCount) : 0;
        int incr = incrCount != null ? Integer.parseInt(incrCount) : 0;
        
        return base + incr;
    }

    public Map<Long, Integer> batchGetRealTimeViewCount(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Integer> result = new HashMap<>();
        
        List<String> baseKeys = postIds.stream()
                .map(RedisCacheKey::postViewCountBaseKey)
                .toList();
        
        List<String> incrKeys = postIds.stream()
                .map(RedisCacheKey::postViewCountKey)
                .toList();
        
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

    public Long getViewCount(Long postId) {
        String key = RedisCacheKey.postViewCountKey(postId);
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0L;
    }

    public Long getAndResetViewCount(Long postId) {
        String key = RedisCacheKey.postViewCountKey(postId);
        Long count = redisTemplate.execute(
                getAndResetScript,
                Collections.singletonList(key),
                String.valueOf(RedisCacheKey.POST_VIEW_COUNT_TTL)
        );
        log.debug("获取并重置浏览量: postId={}, count={}", postId, count);
        return count != null ? count : 0L;
    }

    public List<Long> batchGetAndResetViewCount(List<Long> postIds) {
        return postIds.stream()
                .map(this::getAndResetViewCount)
                .toList();
    }

    public void setViewCount(Long postId, Long viewCount) {
        String key = RedisCacheKey.postViewCountKey(postId);
        redisTemplate.opsForValue().set(key, String.valueOf(viewCount));
    }

    public void deleteViewCount(Long postId) {
        String key = RedisCacheKey.postViewCountKey(postId);
        String baseKey = RedisCacheKey.postViewCountBaseKey(postId);
        redisTemplate.delete(key);
        redisTemplate.delete(baseKey);
    }

    public Set<String> getAllViewCountKeys() {
        Set<String> keys = new HashSet<>();
        String pattern = RedisCacheKey.POST_VIEW_COUNT + "*";
        ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    public void deleteViewCountKeys(Set<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void resetViewCountIncrement(Long postId) {
        String key = RedisCacheKey.postViewCountKey(postId);
        redisTemplate.opsForValue().set(key, "0", 
                RedisCacheKey.POST_VIEW_COUNT_TTL, TimeUnit.SECONDS);
    }
}