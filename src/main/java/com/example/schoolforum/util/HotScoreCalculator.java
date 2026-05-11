package com.example.schoolforum.util;

import com.example.schoolforum.pojo.Posts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 增强版热门榜评分算法
 * 
 * 核心特性：
 * 1. 多维度综合评分（点赞、评论、收藏、浏览量）
 * 2. 时间衰减机制（保证新鲜内容有机会上榜）
 * 3. 可配置的权重参数
 * 4. 精华/置顶加成
 * 
 * 算法公式：
 * score = (likeCount * likeWeight + commentCount * commentWeight + favoriteCount * favoriteWeight + viewCount * viewWeight) / timeDecay^G
 * 其中 timeDecay = (1 + hoursSinceCreation / decayBaseHours)
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

        double baseScore = calculateBaseScore(post);
        double timeDecay = calculateTimeDecay(post.getCreatedAt());
        double bonus = calculateBonus(post);

        double finalScore = (baseScore / timeDecay) + bonus;

        log.debug("帖子[{}]热度分数计算: baseScore={}, timeDecay={}, bonus={}, finalScore={}",
                post.getId(), baseScore, timeDecay, bonus, finalScore);

        return finalScore;
    }

    /**
     * 计算基础互动分（不考虑时间衰减）
     * 
     * 设计思路：
     * - 评论权重最高(5.0)：评论代表深度参与，价值最高
     * - 收藏权重次之(4.0)：收藏代表长期价值认可
     * - 点赞权重中等(3.0)：点赞门槛低但仍有参考价值
     * - 浏览量权重最低(0.1)：防止刷浏览量作弊
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
     * 计算时间衰减因子
     * 
     * 衰减公式：timeDecay = (1 + hoursElapsed / decayBaseHours) ^ decayExponent
     * 
     * 特性：
     * - 新发布的内容衰减因子接近 1（几乎不衰减）
     * - 随时间推移，衰减因子逐渐增大
     * - decayBaseHours 控制衰减速度（默认24小时）
     * - decayExponent 控制衰减曲线形状（>1 为加速衰减）
     *
     * @param createdAt 创建时间
     * @return 衰减因子（>= 1.0）
     */
    public double calculateTimeDecay(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 1.0;
        }

        long hoursElapsed = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
        
        if (hoursElapsed < 0) {
            hoursElapsed = 0;
        }

        double normalizedTime = 1.0 + (hoursElapsed / decayBaseHours);
        return Math.pow(normalizedTime, decayExponent);
    }

    /**
     * 计算特殊标记加成分数
     * 
     * 精华帖子和置顶帖子获得额外加成，
     * 保证优质内容在热门榜中有更好的展示位置
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
