package com.example.schoolforum.service;

import com.example.schoolforum.pojo.Follows;
import com.example.schoolforum.pojo.Users;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 关注表 服务层。
 *
 * @author sugu
 * @since 2026-03-07
 */
public interface FollowsService extends IService<Follows> {

    Follows followUser(Long followerId, Long followingId);

    void unfollowUser(Long followerId, Long followingId);

    boolean isFollowing(Long followerId, Long followingId);

    List<Users> getFollowingList(Long userId);

    List<Users> getFollowerList(Long userId);

    Page<Users> listFollowing(Long userId, int pageNumber, int pageSize);

    Page<Users> listFollowers(Long userId, int pageNumber, int pageSize);

    Page<Follows> listPageFollowing(int pageNumber, int pageSize);

    Page<Follows> listPageFollowers(int pageNumber, int pageSize);
}
