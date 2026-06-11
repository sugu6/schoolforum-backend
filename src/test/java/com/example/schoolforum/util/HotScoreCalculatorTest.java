package com.example.schoolforum.util;

import com.example.schoolforum.enums.EssentialStatus;
import com.example.schoolforum.enums.PinnedStatus;
import com.example.schoolforum.pojo.Posts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HotScoreCalculatorTest {

    private HotScoreCalculator calculator;

    @BeforeEach
    void setUp() throws Exception {
        calculator = new HotScoreCalculator();
        setField("likeWeight", 3.0);
        setField("commentWeight", 5.0);
        setField("favoriteWeight", 4.0);
        setField("viewWeight", 0.1);
        setField("decayBaseHours", 24.0);
        setField("decayExponent", 1.5);
        setField("essentialBonus", 50.0);
        setField("pinnedBonus", 30.0);
        setField("rateWeight", 5.0);
        setField("maxRate", 1.0);
        setField("minViewsForRate", 50);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = HotScoreCalculator.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(calculator, value);
    }

    private Posts createPost(int likes, int comments, int favorites, int views,
                             LocalDateTime createdAt) {
        Posts post = new Posts();
        post.setId(1L);
        post.setLikeCount(likes);
        post.setCommentCount(comments);
        post.setFavoriteCount(favorites);
        post.setViewCount(views);
        post.setCreatedAt(createdAt);
        post.setIsEssential(EssentialStatus.NOT_ESSENTIAL);
        post.setIsPinned(PinnedStatus.NOT_PINNED);
        return post;
    }

    @Test
    void calculateScore_nullPost_returnsZero() {
        assertEquals(0.0, calculator.calculateScore(null));
    }

    @Test
    void calculateInteractionRate_normalCase() {
        Posts post = createPost(10, 5, 5, 100, LocalDateTime.now());
        double rate = calculator.calculateInteractionRate(post);
        assertEquals(0.2, rate, 0.001);
    }

    @Test
    void calculateInteractionRate_zeroViews() {
        Posts post = createPost(10, 5, 5, 0, LocalDateTime.now());
        double rate = calculator.calculateInteractionRate(post);
        assertEquals(1.0, rate, 0.001);
    }

    @Test
    void calculateQualityFactor_belowMinViews_returnsOne() {
        Posts post = createPost(10, 5, 5, 30, LocalDateTime.now());
        double factor = calculator.calculateQualityFactor(post);
        assertEquals(1.0, factor, 0.001);
    }

    @Test
    void calculateQualityFactor_aboveMinViews() {
        Posts post = createPost(10, 5, 5, 100, LocalDateTime.now());
        double factor = calculator.calculateQualityFactor(post);
        assertEquals(2.0, factor, 0.001);
    }

    @Test
    void calculateScore_highInteractionRate_decaysSlower() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(24);

        Posts lowRatePost = createPost(100, 50, 30, 5000, createdAt);
        Posts highRatePost = createPost(50, 30, 20, 500, createdAt);

        double lowRateFactor = calculator.calculateQualityFactor(lowRatePost);
        double highRateFactor = calculator.calculateQualityFactor(highRatePost);
        assertTrue(highRateFactor > lowRateFactor);
    }

    @Test
    void calculateScore_essentialBonus_applied() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        Posts normalPost = createPost(10, 5, 5, 100, createdAt);
        Posts essentialPost = createPost(10, 5, 5, 100, createdAt);
        essentialPost.setIsEssential(EssentialStatus.ESSENTIAL);

        double normalScore = calculator.calculateScore(normalPost);
        double essentialScore = calculator.calculateScore(essentialPost);

        assertEquals(50.0, essentialScore - normalScore, 0.001);
    }

    @Test
    void calculateScore_newPost_minimalDecay() {
        Posts post = createPost(10, 5, 5, 100, LocalDateTime.now());
        double qualityFactor = calculator.calculateQualityFactor(post);
        double decay = calculator.calculateTimeDecay(post.getCreatedAt(), qualityFactor);
        assertTrue(decay < 1.01);
    }
}
