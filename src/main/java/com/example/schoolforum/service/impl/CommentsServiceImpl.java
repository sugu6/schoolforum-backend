package com.example.schoolforum.service.impl;

import com.example.schoolforum.component.PostStatsCache;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.UsersMapper;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.pojo.vo.CommentListVO;
import com.example.schoolforum.pojo.vo.CommentVO;
import com.example.schoolforum.service.CommentsService;
import com.example.schoolforum.service.NotificationsService;
import com.example.schoolforum.service.PostsService;
import com.example.schoolforum.service.UsersService;
import com.example.schoolforum.util.PermissionUtil;
import com.example.schoolforum.websocket.PostStatsWebSocketHandler;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.schoolforum.mapper.CommentsMapper;
import com.example.schoolforum.pojo.Comments;
import com.example.schoolforum.service.CommentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.schoolforum.pojo.table.CommentsTableDef.COMMENTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments> implements CommentsService {

    private final UsersMapper usersMapper;
    private final PostsService postsService;
    private final PostStatsCache postStatsCache;
    private final NotificationsService notificationsService;
    private final UsersService usersService;
    private final PostStatsWebSocketHandler postStatsWebSocketHandler;

    @Override
    @Transactional
    public Comments addComment(Long authorId, Long postId, Long parentId, String content) {
        Posts post = postsService.getById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        
        Comments parentComment = null;
        if (parentId != null && parentId > 0) {
            parentComment = this.getById(parentId);
            if (parentComment == null || !parentComment.getPostId().equals(postId)) {
                throw new BusinessException("父评论不存在或不属于该帖子");
            }
        }
        
        Comments comment = new Comments();
        comment.setAuthorId(authorId);
        comment.setContent(content);
        comment.setPostId(postId);
        comment.setParentId(parentId);
        comment.setLikeCount(0);
        comment.setIsDeleted(0);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        
        this.save(comment);
        
        postStatsCache.incrementCommentCount(postId);
        
        Integer commentCount = postStatsCache.getRealTimeCommentCount(postId);
        postStatsWebSocketHandler.broadcastCommentCount(postId, commentCount);
        
        log.info("评论创建成功: commentId={}, postId={}, authorId={}", comment.getId(), postId, authorId);
        
        Users author = usersService.getCachedUserById(authorId);
        String username = author != null ? author.getUsername() : "用户";
        
        if (parentComment != null) {
            if (parentComment.getAuthorId() != null && !parentComment.getAuthorId().equals(authorId)) {
                notificationsService.createNotification(
                        parentComment.getAuthorId(),
                        "REPLY",
                        username + "回复了你的评论",
                        username + "回复了你在《" + post.getTitle() + "》中的评论",
                        comment.getId(),
                        "COMMENT",
                        authorId
                );
            }
        } else {
            if (!post.getAuthorId().equals(authorId)) {
                notificationsService.createNotification(
                        post.getAuthorId(),
                        "COMMENT",
                        username + "评论了你的帖子",
                        username + "评论了你的帖子《" + post.getTitle() + "》",
                        comment.getId(),
                        "COMMENT",
                        authorId
                );
            }
        }
        
        return comment;
    }

    @Override
    @Transactional
    public Comments updateComment(Long commentId, String content, Long userId) {
        Comments comment = getCommentOrThrow(commentId);
        PermissionUtil.checkOwnerOrAdmin(comment.getAuthorId(), "无权限操作此评论");
        
        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());
        this.updateById(comment);
        
        log.info("评论更新成功: commentId={}, userId={}", commentId, userId);
        return comment;
    }

    @Override
    @Transactional
    public String deleteComment(Long commentId, Long userId) {
        Comments comment = getCommentOrThrow(commentId);
        PermissionUtil.checkOwnerOrAdmin(comment.getAuthorId(), "无权限操作此评论");
        
        boolean hasChildren = hasChildComments(commentId);
        
        Comments update = UpdateEntity.of(Comments.class, commentId);
        update.setIsDeleted(1);
        update.setContent("该评论已被删除");
        update.setAuthorId(null);
        update.setUpdatedAt(LocalDateTime.now());
        getMapper().update(update);
        
        postStatsCache.decrementCommentCount(comment.getPostId());
        
        Integer commentCount = postStatsCache.getRealTimeCommentCount(comment.getPostId());
        postStatsWebSocketHandler.broadcastCommentCount(comment.getPostId(), commentCount);
        
        log.info("评论删除成功: commentId={}, userId={}", commentId, userId);
        
        if (hasChildren) {
            return "评论已删除，其子评论仍可查看";
        }
        return "评论删除成功";
    }
    
    private boolean hasChildComments(Long commentId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(COMMENTS.PARENT_ID.eq(commentId));
        return getMapper().selectCountByQuery(wrapper) > 0;
    }

    @Override
    public Comments getCommentById(Long commentId) {
        return getCommentOrThrow(commentId);
    }

    @Override
    public CommentListVO listByPostId(Long postId) {
        QueryWrapper countWrapper = QueryWrapper.create()
                .where(COMMENTS.POST_ID.eq(postId))
                .and(COMMENTS.IS_DELETED.eq(0));
        Long total = getMapper().selectCountByQuery(countWrapper);

        QueryWrapper wrapper = QueryWrapper.create()
                .where(COMMENTS.POST_ID.eq(postId))
                .and(COMMENTS.IS_DELETED.eq(0))
                .orderBy(COMMENTS.CREATED_AT, true);
        List<Comments> allComments = getMapper().selectListByQuery(wrapper);
        
        if (allComments.isEmpty()) {
            return CommentListVO.of(total, new ArrayList<>());
        }
        
        List<Long> authorIds = allComments.stream()
                .filter(c -> c.getAuthorId() != null)
                .map(Comments::getAuthorId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, Users> authorMap = getAuthorMap(authorIds);
        
        Map<Long, List<Comments>> parentChildrenMap = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comments::getParentId));
        
        List<CommentVO> list = allComments.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> buildCommentTree(c, parentChildrenMap, authorMap))
                .collect(Collectors.toList());
        
        return CommentListVO.of(total, list);
    }
    
    private CommentVO buildCommentTree(Comments comment, Map<Long, List<Comments>> parentChildrenMap, 
                                        Map<Long, Users> authorMap) {
        Users author = authorMap.get(comment.getAuthorId());
        String authorName = author != null ? author.getUsername() : null;
        String authorAvatar = author != null ? author.getAvatarUrl() : null;
        CommentVO vo = CommentVO.from(comment, authorName, authorAvatar);
        
        List<Comments> children = parentChildrenMap.get(comment.getId());
        if (children != null && !children.isEmpty()) {
            List<CommentVO> replies = children.stream()
                    .map(c -> buildCommentTree(c, parentChildrenMap, authorMap))
                    .collect(Collectors.toList());
            vo.setReplies(replies);
        }
        
        return vo;
    }

    @Override
    public Page<Comments> list(int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(COMMENTS.IS_DELETED.eq(0))
                .orderBy(COMMENTS.CREATED_AT, false);
        return getMapper().paginate(pageNumber, pageSize, wrapper);
    }

    @Override
    public Page<Comments> listPage(int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(COMMENTS.IS_DELETED.eq(0))
                .orderBy(COMMENTS.CREATED_AT, false);
        return getMapper().paginate(pageNumber, pageSize, wrapper);
    }

    @Override
    @Transactional
    public void likeComment(Long commentId) {
        Comments comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        
        Comments update = UpdateEntity.of(Comments.class, commentId);
        UpdateWrapper<Comments> wrapper = UpdateWrapper.of(update);
        wrapper.set(COMMENTS.LIKE_COUNT, COMMENTS.LIKE_COUNT.add(1));
        int updated = getMapper().update(update);
        if (updated > 0) {
            log.info("评论点赞: commentId={}", commentId);
        }
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId) {
        Comments update = UpdateEntity.of(Comments.class, commentId);
        UpdateWrapper<Comments> wrapper = UpdateWrapper.of(update);
        wrapper.set(COMMENTS.LIKE_COUNT, COMMENTS.LIKE_COUNT.add(-1));
        int updated = getMapper().update(update);
        if (updated > 0) {
            log.info("评论取消点赞: commentId={}", commentId);
        }
    }

    @Override
    public List<Comments> listHotByPostId(Long postId, int limit) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(COMMENTS.POST_ID.eq(postId))
                .and(COMMENTS.IS_DELETED.eq(0))
                .orderBy(COMMENTS.LIKE_COUNT, false)
                .orderBy(COMMENTS.CREATED_AT, false);
        Page<Comments> page = getMapper().paginate(1, limit, wrapper);
        return page.getRecords();
    }

    private Comments getCommentOrThrow(Long id) {
        Comments comment = this.getById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        return comment;
    }

    private Map<Long, Users> getAuthorMap(List<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Map.of();
        }

        QueryWrapper wrapper = QueryWrapper.create()
                .where(Users::getId).in(authorIds);
        List<Users> users = usersMapper.selectListByQuery(wrapper);

        return users.stream()
                .collect(Collectors.toMap(Users::getId, user -> user));
    }

    @Override
    public int countByPostId(Long postId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(COMMENTS.POST_ID.eq(postId))
                .and(COMMENTS.IS_DELETED.eq(0));
        return Math.toIntExact(getMapper().selectCountByQuery(wrapper));
    }
}
