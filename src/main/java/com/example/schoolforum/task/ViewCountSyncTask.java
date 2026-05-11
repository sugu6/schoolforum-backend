package com.example.schoolforum.task;

import com.example.schoolforum.component.PostStatsCache;
import com.example.schoolforum.component.PostViewCountCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private final PostViewCountCache viewCountCache;
    private final PostStatsCache postStatsCache;
    private final JdbcTemplate jdbcTemplate;

    private static final String REDIS_KEY_PREFIX = "post:view_count:";

    @Scheduled(fixedRate = 300000)
    public void syncViewCountToDatabase() {
        log.debug("开始同步帖子统计数据到数据库");
        
        Set<String> keys = viewCountCache.getAllViewCountKeys();
        if (keys == null || keys.isEmpty()) {
            log.debug("没有需要同步的浏览量数据");
            return;
        }

        List<Object[]> viewCountArgs = new ArrayList<>();
        List<Object[]> likeCountArgs = new ArrayList<>();
        List<Object[]> commentCountArgs = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();
        
        for (String key : keys) {
            try {
                String postIdStr = key.replace(REDIS_KEY_PREFIX, "");
                Long postId = Long.parseLong(postIdStr);
                postIds.add(postId);
            } catch (NumberFormatException e) {
                log.warn("解析帖子ID失败: key={}", key);
            }
        }

        for (Long postId : postIds) {
            Long viewIncr = viewCountCache.getAndResetViewCount(postId);
            Long likeIncr = postStatsCache.getLikeCountIncrement(postId);
            Long commentIncr = postStatsCache.getCommentCountIncrement(postId);
            
            if (viewIncr != null && viewIncr > 0) {
                viewCountArgs.add(new Object[]{viewIncr, postId});
            }
            if (likeIncr != null && likeIncr != 0) {
                likeCountArgs.add(new Object[]{likeIncr, postId});
            }
            if (commentIncr != null && commentIncr != 0) {
                commentCountArgs.add(new Object[]{commentIncr, postId});
            }
        }

        int viewUpdated = batchUpdate("UPDATE posts SET view_count = view_count + ? WHERE id = ?", viewCountArgs);
        int likeUpdated = batchUpdate("UPDATE posts SET like_count = like_count + ? WHERE id = ?", likeCountArgs);
        int commentUpdated = batchUpdate("UPDATE posts SET comment_count = comment_count + ? WHERE id = ?", commentCountArgs);
        
        log.info("统计数据同步完成: 浏览量{}条, 点赞数{}条, 评论数{}条", 
                viewUpdated, likeUpdated, commentUpdated);
        
        for (Long postId : postIds) {
            Integer currentView = viewCountCache.getRealTimeViewCount(postId);
            Integer currentLike = postStatsCache.getRealTimeLikeCount(postId);
            Integer currentComment = postStatsCache.getRealTimeCommentCount(postId);
            
            viewCountCache.initBaseViewCount(postId, currentView);
            postStatsCache.initLikeCountBase(postId, currentLike);
            postStatsCache.initCommentCountBase(postId, currentComment);
            
            viewCountCache.resetViewCountIncrement(postId);
            postStatsCache.resetLikeCountIncrement(postId);
            postStatsCache.resetCommentCountIncrement(postId);
        }
    }

    private int batchUpdate(String sql, List<Object[]> batchArgs) {
        if (batchArgs.isEmpty()) {
            return 0;
        }
        try {
            return jdbcTemplate.batchUpdate(sql, batchArgs).length;
        } catch (Exception e) {
            log.error("批量更新失败: sql={}", sql, e);
            return 0;
        }
    }
}
