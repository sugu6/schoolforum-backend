package com.example.schoolforum.service;

import com.example.schoolforum.pojo.Favorites;
import com.example.schoolforum.pojo.Posts;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 收藏表 服务层。
 *
 * @author sugu
 * @since 2026-03-06
 */
public interface FavoritesService extends IService<Favorites> {

    Favorites addFavorite(Long userId, Long postId);

    void removeFavorite(Long userId, Long postId);

    boolean isFavorited(Long userId, Long postId);

    List<Posts> getUserFavorites(Long userId);

    Page<Posts> list(Long userId, int pageNumber, int pageSize);

    Page<Favorites> listPage(int pageNumber, int pageSize);

    int countByPostId(Long postId);
}
