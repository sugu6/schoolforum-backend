package com.example.schoolforum.pojo;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("follows")
@Schema(description = "关注实体")
public class Follows implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "关注ID", example = "1")
    private Long id;

    @Schema(description = "关注者ID", example = "1")
    private Long followerId;

    @Schema(description = "被关注者ID", example = "2")
    private Long followingId;

    @Schema(description = "关注时间")
    private LocalDateTime createdAt;
}
