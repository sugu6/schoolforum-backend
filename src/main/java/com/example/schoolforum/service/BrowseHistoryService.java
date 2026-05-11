package com.example.schoolforum.service;

import com.example.schoolforum.pojo.BrowseHistory;
import com.example.schoolforum.pojo.Posts;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * 浏览历史表 服务层。
 *
 * @author sugu
 * @since 2026-05-10
 */
public interface BrowseHistoryService extends IService<BrowseHistory> {

    BrowseHistory addBrowseHistory(Long userId, Long postId);

    void removeBrowseHistory(Long userId, Long postId);

    void clearBrowseHistory(Long userId);

    Page<Posts> list(Long userId, int pageNumber, int pageSize);

    Page<BrowseHistory> listPage(int pageNumber, int pageSize);
}
