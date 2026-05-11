package com.example.schoolforum.pojo;

import com.example.schoolforum.enums.ActiveStatus;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("categories")
@Schema(description = "分类实体")
public class Categories implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "学习")
    private String name;

    @Schema(description = "父分类ID，一级分类为NULL")
    private Long parentId;

    @Schema(description = "层级：1-一级分类，2-二级分类")
    private Integer level;

    @Schema(description = "状态")
    private ActiveStatus status;

    @Schema(description = "帖子数量", example = "100")
    private Integer postCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Column(ignore = true)
    @Schema(description = "子分类列表")
    private List<Categories> children;
}
