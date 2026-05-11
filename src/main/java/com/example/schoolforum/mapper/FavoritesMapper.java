package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.example.schoolforum.pojo.Favorites;

/**
 * 收藏表 映射层。
 *
 * @author sugu
 * @since 2026-03-06
 */
@Mapper
public interface FavoritesMapper extends BaseMapper<Favorites> {

    default int countByPostId(Long postId) {
        return Math.toIntExact(selectCountByQuery(
            QueryWrapper.create().where(Favorites::getPostId).eq(postId)
        ));
    }
}
