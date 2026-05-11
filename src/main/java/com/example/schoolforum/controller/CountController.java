package com.example.schoolforum.controller;

import com.example.schoolforum.pojo.dto.CountVO;
import com.example.schoolforum.service.AnnouncementsService;
import com.example.schoolforum.service.CategoriesService;
import com.example.schoolforum.service.CommentsService;
import com.example.schoolforum.service.PostsService;
import com.example.schoolforum.service.TagsService;
import com.example.schoolforum.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/count")
@RequiredArgsConstructor
@Tag(name = "统计计数")
public class CountController {

    private final UsersService usersService;
    private final CommentsService commentsService;
    private final PostsService postsService;
    private final AnnouncementsService announcementsService;
    private final CategoriesService categoriesService;
    private final TagsService tagsService;

    @GetMapping
    @Operation(summary = "获取统计数据", description = "获取用户总数、评论总数、帖子总数、公告总数、分类总数、标签总数")
    public CountVO getCount() {
        return CountVO.builder()
                .userCount(usersService.count())
                .commentCount(commentsService.count())
                .postCount(postsService.count())
                .announcementCount(announcementsService.count())
                .categoryCount(categoriesService.count())
                .tagCount(tagsService.count())
                .build();
    }
}
