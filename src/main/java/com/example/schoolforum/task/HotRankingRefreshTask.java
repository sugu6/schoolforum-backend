package com.example.schoolforum.task;

import com.example.schoolforum.constant.RedisCacheKey;
import com.example.schoolforum.mapper.PostsMapper;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.util.HotScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 热门榜定时刷新任务
 * 
 * 功能：
 * 1. 定期计算所有帖子的热度分数
 * 2. 更新 Redis Sorted Set 缓存，支持高效的热门榜查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotRankingRefreshTask {

    private final PostsMapper postsMapper;
    private final HotScoreCalculator hotScoreCalculator;
    private final StringRedisTemplate redisTemplate;

    /**
     * 每5分钟刷新一次热门榜缓存
     * 使用 fixedRate 保证固定的执行间隔
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void refreshHotRanking() {
        log.info("开始刷新热门榜缓存...");
        long startTime = System.currentTimeMillis();

        try {
            String zSetKey = RedisCacheKey.hotRankZSetKey("all");
            
            List<Posts> allPosts = postsMapper.selectAll();

            redisTemplate.delete(zSetKey);

            for (Posts post : allPosts) {
                double score = hotScoreCalculator.calculateScore(post);
                
                redisTemplate.opsForZSet().add(
                    zSetKey,
                    String.valueOf(post.getId()),
                    score
                );
            }

            redisTemplate.expire(zSetKey, RedisCacheKey.HOT_RANK_ZSET_TTL, TimeUnit.SECONDS);

            long size = redisTemplate.opsForZSet().size(zSetKey);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("热门榜缓存刷新完成，共{}条帖子，耗时: {}ms", size, duration);
        } catch (Exception e) {
            log.error("刷新热门榜缓存失败", e);
        }
    }
}
