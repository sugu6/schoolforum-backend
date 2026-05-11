package com.example.schoolforum.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.example.schoolforum.enums.DeletionStatus;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.AccountDeletionRequestMapper;
import com.example.schoolforum.mapper.CommentsMapper;
import com.example.schoolforum.mapper.FavoritesMapper;
import com.example.schoolforum.mapper.FollowsMapper;
import com.example.schoolforum.mapper.NotificationsMapper;
import com.example.schoolforum.mapper.PostsMapper;
import com.example.schoolforum.mapper.PrivateMessageMapper;
import com.example.schoolforum.mapper.UsersMapper;
import com.example.schoolforum.pojo.AccountDeletionRequest;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.pojo.vo.AccountDeletionRequestVO;
import com.example.schoolforum.service.AccountDeletionService;
import com.example.schoolforum.service.SearchService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionServiceImpl extends ServiceImpl<AccountDeletionRequestMapper, AccountDeletionRequest> implements AccountDeletionService {

    private final UsersMapper usersMapper;
    private final PostsMapper postsMapper;
    private final CommentsMapper commentsMapper;
    private final FollowsMapper followsMapper;
    private final FavoritesMapper favoritesMapper;
    private final NotificationsMapper notificationsMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final SearchService searchService;

    private static final int COOLING_PERIOD_DAYS = 7;

    @Override
    @Transactional
    public AccountDeletionRequest requestDeletion(Long userId, String reason) {
        Users user = usersMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        AccountDeletionRequest existingRequest = getPendingRequest(userId);
        if (existingRequest != null) {
            throw new BusinessException("您已提交过注销申请，请等待冷静期结束或撤销申请");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = now.plusDays(COOLING_PERIOD_DAYS);

        AccountDeletionRequest request = AccountDeletionRequest.builder()
                .userId(userId)
                .reason(reason)
                .status(DeletionStatus.PENDING)
                .requestedAt(now)
                .scheduledAt(scheduledAt)
                .createdAt(now)
                .build();

        this.save(request);

        if (StpUtil.isLogin(userId)) {
            StpUtil.logout(userId);
        }

        log.info("账户注销申请已提交: userId={}, scheduledAt={}", userId, scheduledAt);
        return request;
    }

    @Override
    @Transactional
    public void cancelDeletion(Long userId) {
        AccountDeletionRequest pendingRequest = getPendingRequest(userId);
        if (pendingRequest == null) {
            throw new BusinessException("没有待处理的注销申请");
        }

        AccountDeletionRequest updateRequest = UpdateEntity.of(AccountDeletionRequest.class, pendingRequest.getId());
        updateRequest.setStatus(DeletionStatus.CANCELLED);
        updateRequest.setCompletedAt(LocalDateTime.now());
        getMapper().update(updateRequest);

        log.info("账户注销申请已撤销: userId={}", userId);
    }

    @Override
    public AccountDeletionRequest getPendingRequest(Long userId) {
        return getMapper().selectOneByQuery(
                QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("status", DeletionStatus.PENDING)
        );
    }

    @Override
    @Transactional
    public int processExpiredDeletionRequests() {
        LocalDateTime now = LocalDateTime.now();

        List<AccountDeletionRequest> expiredRequests = getMapper().selectListByQuery(
                QueryWrapper.create()
                        .eq("status", DeletionStatus.PENDING)
                        .le("scheduled_at", now)
        );

        int processedCount = 0;
        for (AccountDeletionRequest request : expiredRequests) {
            try {
                deleteUser(request.getUserId());

                AccountDeletionRequest updateRequest = UpdateEntity.of(AccountDeletionRequest.class, request.getId());
                updateRequest.setStatus(DeletionStatus.COMPLETED);
                updateRequest.setCompletedAt(now);
                getMapper().update(updateRequest);

                processedCount++;
                log.info("账户注销完成: userId={}", request.getUserId());
            } catch (Exception e) {
                log.error("账户注销失败: userId={}, error={}", request.getUserId(), e.getMessage(), e);
            }
        }

        return processedCount;
    }

    @Override
    public Page<AccountDeletionRequestVO> listPage(Integer pageNumber, Integer pageSize, DeletionStatus status) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderBy("requested_at", false);
        
        Page<AccountDeletionRequest> requestPage = this.page(new Page<>(pageNumber, pageSize), queryWrapper);
        
        Set<Long> userIds = requestPage.getRecords().stream()
                .map(AccountDeletionRequest::getUserId)
                .collect(Collectors.toSet());
        
        Map<Long, String> usernameMap = Map.of();
        if (!userIds.isEmpty()) {
            List<Users> users = usersMapper.selectListByIds(userIds);
            usernameMap = users.stream()
                    .collect(Collectors.toMap(Users::getId, Users::getUsername, (v1, v2) -> v1));
        }
        
        Map<Long, String> finalUsernameMap = usernameMap;
        Page<AccountDeletionRequestVO> voPage = new Page<>();
        voPage.setPageNumber(requestPage.getPageNumber());
        voPage.setPageSize(requestPage.getPageSize());
        voPage.setTotalRow(requestPage.getTotalRow());
        voPage.setRecords(requestPage.getRecords().stream()
                .map(request -> AccountDeletionRequestVO.from(request, finalUsernameMap.get(request.getUserId())))
                .collect(Collectors.toList()));
        
        return voPage;
    }

    private void deleteUser(Long userId) {
        notificationsMapper.deleteByQuery(QueryWrapper.create().eq("user_id", userId));
        notificationsMapper.deleteByQuery(QueryWrapper.create().eq("sender_id", userId));

        favoritesMapper.deleteByQuery(QueryWrapper.create().eq("user_id", userId));

        followsMapper.deleteByQuery(QueryWrapper.create().eq("follower_id", userId));
        followsMapper.deleteByQuery(QueryWrapper.create().eq("following_id", userId));

        privateMessageMapper.deleteByQuery(QueryWrapper.create().eq("sender_id", userId));
        privateMessageMapper.deleteByQuery(QueryWrapper.create().eq("receiver_id", userId));

        List<Long> postIds = postsMapper.selectListByQuery(
                QueryWrapper.create().eq("author_id", userId).select("id")
        ).stream().map(post -> post.getId()).toList();

        for (Long postId : postIds) {
            commentsMapper.deleteByQuery(QueryWrapper.create().eq("post_id", postId));
            searchService.deletePost(postId);
        }

        postsMapper.deleteByQuery(QueryWrapper.create().eq("author_id", userId));

        commentsMapper.deleteByQuery(QueryWrapper.create().eq("author_id", userId));

        searchService.deleteUser(userId);

        usersMapper.deleteById(userId);

        log.info("用户数据已删除: userId={}", userId);
    }
}