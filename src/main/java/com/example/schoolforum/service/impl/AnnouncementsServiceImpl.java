package com.example.schoolforum.service.impl;

import com.example.schoolforum.enums.AnnouncementStatus;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.AnnouncementsMapper;
import com.example.schoolforum.pojo.Announcements;
import com.example.schoolforum.pojo.dto.AnnouncementCreateRequest;
import com.example.schoolforum.pojo.dto.AnnouncementUpdateRequest;
import com.example.schoolforum.service.AnnouncementsService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.schoolforum.pojo.table.AnnouncementsTableDef.ANNOUNCEMENTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementsServiceImpl extends ServiceImpl<AnnouncementsMapper, Announcements> implements AnnouncementsService {

    @Override
    @Transactional
    public Announcements createAnnouncement(AnnouncementCreateRequest request, Long publisherId) {
        Announcements announcement = Announcements.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType())
                .status(Boolean.TRUE.equals(request.getPublish()) ? AnnouncementStatus.PUBLISHED : AnnouncementStatus.DRAFT)
                .isTop(0)
                .publisherId(publisherId)
                .build();

        this.save(announcement);
        log.info("公告创建成功: id={}, title={}, status={}", announcement.getId(), announcement.getTitle(), announcement.getStatus());
        return announcement;
    }

    @Override
    @Transactional
    public Announcements updateAnnouncement(Long id, AnnouncementUpdateRequest request) {
        Announcements announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }

        if (request.getTitle() != null) {
            announcement.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            announcement.setContent(request.getContent());
        }
        if (request.getType() != null) {
            announcement.setType(request.getType());
        }

        this.updateById(announcement);
        log.info("公告更新成功: id={}", id);
        return announcement;
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcements announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }

        this.removeById(id);
        log.info("公告删除成功: id={}, title={}", id, announcement.getTitle());
    }

    @Override
    @Transactional
    public Announcements publishAnnouncement(Long id) {
        Announcements announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw new BusinessException("公告已处于发布状态");
        }

        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        this.updateById(announcement);
        log.info("公告发布成功: id={}", id);
        return announcement;
    }

    @Override
    @Transactional
    public Announcements offlineAnnouncement(Long id) {
        Announcements announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new BusinessException("只能下架已发布的公告");
        }

        announcement.setStatus(AnnouncementStatus.OFFLINE);
        this.updateById(announcement);
        log.info("公告下架成功: id={}", id);
        return announcement;
    }

    @Override
    @Transactional
    public void toggleTop(Long id) {
        Announcements announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new BusinessException("只能置顶已发布的公告");
        }

        announcement.setIsTop(announcement.getIsTop() == 1 ? 0 : 1);
        this.updateById(announcement);
        log.info("公告置顶状态切换: id={}, isTop={}", id, announcement.getIsTop());
    }

    @Override
    public Page<Announcements> listPublished(int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(ANNOUNCEMENTS.STATUS.eq(AnnouncementStatus.PUBLISHED))
                .orderBy(ANNOUNCEMENTS.IS_TOP, false)
                .orderBy(ANNOUNCEMENTS.CREATED_AT, false);
        return this.page(new Page<>(pageNumber, pageSize), wrapper);
    }

    @Override
    public Page<Announcements> listAll(int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .orderBy(ANNOUNCEMENTS.CREATED_AT, false);
        return this.page(new Page<>(pageNumber, pageSize), wrapper);
    }

    @Override
    public Announcements getAnnouncementDetail(Long id) {
        Announcements announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        return announcement;
    }
}
