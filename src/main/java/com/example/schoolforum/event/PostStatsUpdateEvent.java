package com.example.schoolforum.event;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PostStatsUpdateEvent {

    private final Long postId;
    private final StatsType type;

    public PostStatsUpdateEvent(Long postId, StatsType type) {
        this.postId = postId;
        this.type = type;
    }

    public enum StatsType {
        FAVORITE_ADD,
        FAVORITE_REMOVE,
        COMMENT_ADD,
        COMMENT_REMOVE
    }
}
