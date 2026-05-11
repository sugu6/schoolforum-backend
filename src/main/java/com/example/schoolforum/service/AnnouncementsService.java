package com.example.schoolforum.service;

import com.example.schoolforum.pojo.Announcements;
import com.example.schoolforum.pojo.dto.AnnouncementCreateRequest;
import com.example.schoolforum.pojo.dto.AnnouncementUpdateRequest;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * 系统公告 服务层。
 */
public interface AnnouncementsService extends IService<Announcements> {

    Announcements createAnnouncement(AnnouncementCreateRequest request, Long publisherId);

    Announcements updateAnnouncement(Long id, AnnouncementUpdateRequest request);

    void deleteAnnouncement(Long id);

    Announcements publishAnnouncement(Long id);

    Announcements offlineAnnouncement(Long id);

    void toggleTop(Long id);

    Page<Announcements> listPublished(int pageNumber, int pageSize);

    Page<Announcements> listAll(int pageNumber, int pageSize);

    Announcements getAnnouncementDetail(Long id);
}
