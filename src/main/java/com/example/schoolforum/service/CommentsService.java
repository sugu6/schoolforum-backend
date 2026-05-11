package com.example.schoolforum.service;

import com.example.schoolforum.pojo.Comments;
import com.example.schoolforum.pojo.vo.CommentListVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface CommentsService extends IService<Comments> {

    Comments addComment(Long authorId, Long postId, Long parentId, String content);

    Comments updateComment(Long commentId, String content, Long userId);

    String deleteComment(Long commentId, Long userId);

    Comments getCommentById(Long commentId);

    CommentListVO listByPostId(Long postId);

    Page<Comments> list(int pageNumber, int pageSize);

    Page<Comments> listPage(int pageNumber, int pageSize);

    void likeComment(Long commentId);

    void unlikeComment(Long commentId);

    List<Comments> listHotByPostId(Long postId, int limit);

    int countByPostId(Long postId);
}
