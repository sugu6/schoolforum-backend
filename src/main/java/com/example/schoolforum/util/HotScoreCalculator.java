package com.example.schoolforum.util;

import com.example.schoolforum.pojo.Posts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 质量调节衰减热门榜评分算法
 *
 * 核心特性：
 * 1. 多维度综合评分（点赞、评论、收藏、浏览量）
 * 2. 互动率指标（衡量内容质量，高质量内容衰减更慢）
 * 3. 质量调节衰减（互动率高的帖子有效衰减指数更小）
 * 4. 可配置的权重参数
 * 5. 精华/置顶加成
 *
 * 算法公式：
 * qualityScore = likeCount * likeWeight + commentCount * commentWeight + favoriteCount * favoriteWeight + viewCount * viewWeight
 * interactionRate = (likeCount + commentCount + favoriteCount) / max(viewCount, 1)
 * qualityFactor = 1 + clamp(interactionRate, 0, maxRate) * rateWeight
 * adjustedDecay = (1 + hoursElapsed / decayBaseHours) ^ (decayExponent / qualityFactor)
 * finalScore = qualityScore / adjustedDecay + bonus
 */
@Slf4j
@Component
public class HotScoreCalculator {

    @Value("${hot-rank.like-weight:3.0}")
    private double likeWeight;

    @Value("${hot-rank.comment-weight:5.0}")
    private double commentWeight;

    @Value("${hot-rank.favorite-weight:4.0}")
    private double favoriteWeight;

    @Value("${hot-rank.view-weight:0.1}")
    private double viewWeight;

    @Value("${hot-rank.decay-base:24.0}")
    private double decayBaseHours;

    @Value("${hot-rank.decay-exponent:1.5}")
    private double decayExponent;

    @Value("${hot-rank.essential-bonus:50.0}")
    private double essentialBonus;

    @Value("${hot-rank.pinned-bonus:30.0}")
    private double pinnedBonus;

    @Value("${hot-rank.rate-weight:5.0}")
    private double rateWeight;

    @Value("${hot-rank.max-rate:1.0}")
    private double maxRate;

    @Value("${hot-rank.min-views-for-rate:50}")
    private int minViewsForRate;

    /**
     * 计算帖子的综合热度分数
     *
     * @param post 帖子实体
     * @return 热度分数（越高越热门）
     */
    public double calculateScore(Posts post) {
        if (post == null) {
            return 0.0;
        }

        double qualityScore = calculateBaseScore(post);
        double qualityFactor = calculateQualityFactor(post);
        double adjustedDecay = calculateTimeDecay(post.getCreatedAt(), qualityFactor);
        double bonus = calculateBonus(post);

        double finalScore = (qualityScore / adjustedDecay) + bonus;

        log.debug("帖子[{}]热度分数计算: qualityScore={}, qualityFactor={}, adjustedDecay={}, bonus={}, finalScore={}",
                post.getId(), qualityScore, qualityFactor, adjustedDecay, bonus, finalScore);

        return finalScore;
    }

    /**
     * 计算基础互动分（不考虑时间衰减）
     *
     * @param post 帖子实体
     * @return 基础分数
     */
    public double calculateBaseScore(Posts post) {
        int likes = safeGetInt(post.getLikeCount());
        int comments = safeGetInt(post.getCommentCount());
        int favorites = safeGetInt(post.getFavoriteCount());
        int views = safeGetInt(post.getViewCount());

        return likes * likeWeight +
               comments * commentWeight +
               favorites * favoriteWeight +
               views * viewWeight;
    }

    /**
     * 计算互动率
     *
     * 互动率 = (点赞数 + 评论数 + 收藏数) / max(浏览量, 1)
     * 衡量浏览者中有多大比例进行了互动，是内容质量的直接指标
     *
     * @param post 帖子实体
     * @return 互动率（0.0 ~ maxRate）
     */
    public double calculateInteractionRate(Posts post) {
        int likes = safeGetInt(post.getLikeCount());
        int comments = safeGetInt(post.getCommentCount());
        int favorites = safeGetInt(post.getFavoriteCount());
        int views = safeGetInt(post.getViewCount());

        int interactionCount = likes + comments + favorites;
        double rate = (double) interactionCount / Math.max(views, 1);
        return Math.min(rate, maxRate);
    }

    /**
     * 计算质量因子
     *
     * 浏览量低于门槛时不启用互动率调节，防止新帖获得不合理加成。
     * 质量因子 = 1 + clamp(互动率, 0, maxRate) * rateWeight
     * 互动率越高，质量因子越大，有效衰减指数越小，衰减越慢
     *
     * @param post 帖子实体
     * @return 质量因子（>= 1.0）
     */
    public double calculateQualityFactor(Posts post) {
        int views = safeGetInt(post.getViewCount());
        if (views < minViewsForRate) {
            return 1.0;
        }
        double interactionRate = calculateInteractionRate(post);
        return 1.0 + interactionRate * rateWeight;
    }

    /**
     * 计算质量调节衰减因子
     *
     * 衰减公式：adjustedDecay = (1 + hoursElapsed / decayBaseHours) ^ (decayExponent / qualityFactor)
     * 质量因子越大，有效衰减指数越小，高质量内容衰减更慢
     *
     * @param createdAt 创建时间
     * @param qualityFactor 质量因子
     * @return 衰减因子（>= 1.0）
     */
    public double calculateTimeDecay(LocalDateTime createdAt, double qualityFactor) {
        if (createdAt == null) {
            return 1.0;
        }

        long hoursElapsed = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());

        if (hoursElapsed < 0) {
            hoursElapsed = 0;
        }

        double normalizedTime = 1.0 + (hoursElapsed / decayBaseHours);
        double adjustedExponent = decayExponent / qualityFactor;
        return Math.pow(normalizedTime, adjustedExponent);
    }

    /**
     * 计算特殊标记加成分数
     *
     * @param post 帖子实体
     * @return 加成分数
     */
    public double calculateBonus(Posts post) {
        double bonus = 0.0;

        if (post != null && post.getIsEssential() != null && post.getIsEssential().getCode() == 1) {
            bonus += essentialBonus;
        }

        if (post != null && post.getIsPinned() != null && post.getIsPinned().getCode() == 1) {
            bonus += pinnedBonus;
        }

        return bonus;
    }

    /**
     * 安全获取整数值（防止 NPE）
     */
    private int safeGetInt(Integer value) {
        return value != null ? value : 0;
    }
}
