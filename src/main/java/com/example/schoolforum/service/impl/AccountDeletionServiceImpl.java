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

import static com.example.schoolforum.pojo.table.AccountDeletionRequestTableDef.ACCOUNT_DELETION_REQUEST;
import static com.example.schoolforum.pojo.table.CommentsTableDef.COMMENTS;
import static com.example.schoolforum.pojo.table.FavoritesTableDef.FAVORITES;
import static com.example.schoolforum.pojo.table.FollowsTableDef.FOLLOWS;
import static com.example.schoolforum.pojo.table.NotificationsTableDef.NOTIFICATIONS;
import static com.example.schoolforum.pojo.table.PostsTableDef.POSTS;
import static com.example.schoolforum.pojo.table.PrivateMessageTableDef.PRIVATE_MESSAGE;

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
                        .where(ACCOUNT_DELETION_REQUEST.USER_ID.eq(userId))
                        .and(ACCOUNT_DELETION_REQUEST.STATUS.eq(DeletionStatus.PENDING))
        );
    }

    @Override
    @Transactional
    public int processExpiredDeletionRequests() {
        LocalDateTime now = LocalDateTime.now();

        List<AccountDeletionRequest> expiredRequests = getMapper().selectListByQuery(
                QueryWrapper.create()
                        .where(ACCOUNT_DELETION_REQUEST.STATUS.eq(DeletionStatus.PENDING))
                        .and(ACCOUNT_DELETION_REQUEST.SCHEDULED_AT.le(now))
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
            queryWrapper.where(ACCOUNT_DELETION_REQUEST.STATUS.eq(status));
        }
        queryWrapper.orderBy(ACCOUNT_DELETION_REQUEST.REQUESTED_AT.desc());
        
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
        notificationsMapper.deleteByQuery(QueryWrapper.create().where(NOTIFICATIONS.USER_ID.eq(userId)));
        notificationsMapper.deleteByQuery(QueryWrapper.create().where(NOTIFICATIONS.SENDER_ID.eq(userId)));

        favoritesMapper.deleteByQuery(QueryWrapper.create().where(FAVORITES.USER_ID.eq(userId)));

        followsMapper.deleteByQuery(QueryWrapper.create().where(FOLLOWS.FOLLOWER_ID.eq(userId)));
        followsMapper.deleteByQuery(QueryWrapper.create().where(FOLLOWS.FOLLOWING_ID.eq(userId)));

        privateMessageMapper.deleteByQuery(QueryWrapper.create().where(PRIVATE_MESSAGE.SENDER_ID.eq(userId)));
        privateMessageMapper.deleteByQuery(QueryWrapper.create().where(PRIVATE_MESSAGE.RECEIVER_ID.eq(userId)));

        List<Long> postIds = postsMapper.selectListByQuery(
                QueryWrapper.create().where(POSTS.AUTHOR_ID.eq(userId)).select("id")
        ).stream().map(post -> post.getId()).toList();

        for (Long postId : postIds) {
            commentsMapper.deleteByQuery(QueryWrapper.create().where(COMMENTS.POST_ID.eq(postId)));
            searchService.deletePost(postId);
        }

        postsMapper.deleteByQuery(QueryWrapper.create().where(POSTS.AUTHOR_ID.eq(userId)));

        commentsMapper.deleteByQuery(QueryWrapper.create().where(COMMENTS.AUTHOR_ID.eq(userId)));

        searchService.deleteUser(userId);

        usersMapper.deleteById(userId);

        log.info("用户数据已删除: userId={}", userId);
    }
}