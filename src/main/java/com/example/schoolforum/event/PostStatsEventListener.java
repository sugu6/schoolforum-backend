package com.example.schoolforum.event;

import com.example.schoolforum.component.PostStatsCache;
import com.example.schoolforum.service.PostsService;
import com.example.schoolforum.websocket.PostStatsWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostStatsEventListener {

    private final PostsService postsService;
    private final PostStatsCache postStatsCache;
    private final PostStatsWebSocketHandler postStatsWebSocketHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostStatsUpdate(PostStatsUpdateEvent event) {
        Long postId = event.getPostId();
        
        switch (event.getType()) {
            case FAVORITE_ADD -> {
                postsService.favoritePost(postId);
                log.info("事件驱动-帖子收藏数增加: postId={}", postId);
            }
            case FAVORITE_REMOVE -> {
                postsService.unfavoritePost(postId);
                log.info("事件驱动-帖子收藏数减少: postId={}", postId);
            }
            case COMMENT_ADD -> {
                log.debug("事件驱动-帖子评论数增加: postId={}", postId);
            }
            case COMMENT_REMOVE -> {
                log.debug("事件驱动-帖子评论数减少: postId={}", postId);
            }
        }
    }
}
