package com.example.schoolforum.service;

import com.example.schoolforum.enums.EssentialStatus;
import com.example.schoolforum.enums.PinnedStatus;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.example.schoolforum.pojo.Posts;

import java.util.List;

public interface PostsService extends IService<Posts> {

    List<Posts> getHotList(int limit);

    List<Posts> getLatestList(int limit);

    Page<Posts> getHotListPage(int pageNumber, int pageSize, Long categoryId);

    Page<Posts> getLatestListPage(int pageNumber, int pageSize, Long categoryId);

    Posts createPost(Long authorId, String title, String content, List<Long> tagIds, Long categoryId, String coverImage);

    Posts updatePost(Long postId, String title, String content, List<Long> tagIds, Long categoryId, String coverImage, Long userId);

    void deletePost(Long postId, Long userId);

    Posts getPostWithViewCount(Long postId, boolean increment);

    List<Posts> listAllWithAuthor();

    Posts getPostWithAuthorById(Long postId);

    Page<Posts> list(int pageNumber, int pageSize);

    Page<Posts> listByCategory(Long categoryId, int pageNumber, int pageSize);

    Page<Posts> listPage(int pageNumber, int pageSize);

    void likePost(Long postId);

    void unlikePost(Long postId);

    void favoritePost(Long postId);

    void unfavoritePost(Long postId);

    void setPinned(Long postId, boolean pinned);

    void setEssential(Long postId, boolean essential);

    Page<Posts> listEssential(int pageNumber, int pageSize, Long categoryId);

    Page<Posts> listByAuthor(Long authorId, int pageNumber, int pageSize);

    List<Posts> getRelatedPosts(Long postId, int limit);
}
