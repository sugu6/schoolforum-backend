package com.example.schoolforum.pojo;

import com.example.schoolforum.enums.AnnouncementStatus;
import com.example.schoolforum.enums.AnnouncementType;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("announcements")
@Schema(description = "系统公告实体")
public class Announcements implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "公告ID", example = "1")
    private Long id;

    @Schema(description = "公告标题", example = "系统维护通知", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "公告内容（Markdown格式）", example = "# 维护通知\n\n系统将于今晚进行维护...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "公告类型：INFO-普通通知、IMPORTANT-重要公告、URGENT-紧急公告", example = "INFO")
    private AnnouncementType type;

    @Schema(description = "公告状态：DRAFT-草稿、PUBLISHED-已发布、OFFLINE-已下架", example = "DRAFT")
    private AnnouncementStatus status;

    @Schema(description = "是否置顶：0-否、1-是", example = "0")
    private Integer isTop;

    @Schema(description = "发布者ID（管理员）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long publisherId;

    @RelationManyToOne(selfField = "publisherId", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "发布者信息")
    private Users publisher;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
