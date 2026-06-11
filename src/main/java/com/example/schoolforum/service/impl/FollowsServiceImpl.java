package com.example.schoolforum.service.impl;

import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.UsersMapper;
import com.example.schoolforum.pojo.Follows;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.service.FollowsService;
import com.example.schoolforum.service.NotificationsService;
import com.example.schoolforum.service.UsersService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.schoolforum.mapper.FollowsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.schoolforum.pojo.table.FollowsTableDef.FOLLOWS;

/**
 * 关注表 服务层实现。
 *
 * @author sugu
 * @since 2026-03-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowsServiceImpl extends ServiceImpl<FollowsMapper, Follows> implements FollowsService {

    private final UsersMapper usersMapper;
    private final UsersService usersService;
    private final NotificationsService notificationsService;

    @Override
    @Transactional
    public Follows followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException("不能关注自己");
        }

        Users followingUser = usersMapper.selectOneById(followingId);
        if (followingUser == null) {
            throw new BusinessException("用户不存在");
        }

        Follows follow = Follows.builder()
                .followerId(followerId)
                .followingId(followingId)
                .createdAt(LocalDateTime.now())
                .build();
        try {
            this.save(follow);

            log.info("用户关注: followerId={}, followingId={}", followerId, followingId);

            Users follower = usersService.getCachedUserById(followerId);
            String username = follower != null ? follower.getUsername() : "用户";
            notificationsService.createNotification(
                    followingId,
                    "FOLLOW",
                    username + "关注了你",
                    username + "关注了你",
                    followerId,
                    null,
                    followerId
            );
        } catch (DuplicateKeyException e) {
            // 已关注，幂等处理，不发送通知
        }
        
        return follow;
    }

    @Override
    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(FOLLOWS.FOLLOWER_ID.eq(followerId))
                .and(FOLLOWS.FOLLOWING_ID.eq(followingId));
        int deleted = getMapper().deleteByQuery(wrapper);
        if (deleted == 0) {
            throw new BusinessException("未关注该用户");
        }
        log.info("用户取消关注: followerId={}, followingId={}", followerId, followingId);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(FOLLOWS.FOLLOWER_ID.eq(followerId))
                .and(FOLLOWS.FOLLOWING_ID.eq(followingId));
        return getMapper().selectCountByQuery(wrapper) > 0;
    }

    @Override
    public List<Users> getFollowingList(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select("f.*")
                .from("follows").as("f")
                .where(FOLLOWS.as("f").FOLLOWER_ID.eq(userId))
                .orderBy("f.created_at", false);
        List<Follows> follows = getMapper().selectListByQuery(wrapper);

        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> followingIds = follows.stream()
                .map(Follows::getFollowingId)
                .toList();

        QueryWrapper userWrapper = QueryWrapper.create()
                .in("id", followingIds);
        List<Users> users = usersMapper.selectListByQuery(userWrapper);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public List<Users> getFollowerList(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select("f.*")
                .from("follows").as("f")
                .where(FOLLOWS.as("f").FOLLOWING_ID.eq(userId))
                .orderBy("f.created_at", false);
        List<Follows> follows = getMapper().selectListByQuery(wrapper);

        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> followerIds = follows.stream()
                .map(Follows::getFollowerId)
                .toList();

        QueryWrapper userWrapper = QueryWrapper.create()
                .in("id", followerIds);
        List<Users> users = usersMapper.selectListByQuery(userWrapper);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public Page<Users> listFollowing(Long userId, int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select("u.*")
                .from("follows").as("f")
                .leftJoin("users").as("u").on("f.following_id = u.id")
                .where(FOLLOWS.as("f").FOLLOWER_ID.eq(userId))
                .orderBy("f.created_at", false);
        Page<Users> page = usersMapper.paginate(pageNumber, pageSize, wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }

    @Override
    public Page<Users> listFollowers(Long userId, int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select("u.*")
                .from("follows").as("f")
                .leftJoin("users").as("u").on("f.follower_id = u.id")
                .where(FOLLOWS.as("f").FOLLOWING_ID.eq(userId))
                .orderBy("f.created_at", false);
        Page<Users> page = usersMapper.paginate(pageNumber, pageSize, wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }

    @Override
    public Page<Follows> listPageFollowing(int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .from("follows").as("f")
                .orderBy("f.created_at", false);
        return mapper.paginate(pageNumber, pageSize, wrapper);
    }

    @Override
    public Page<Follows> listPageFollowers(int pageNumber, int pageSize) {
        return listPageFollowing(pageNumber, pageSize);
    }
}
