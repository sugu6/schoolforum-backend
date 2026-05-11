package com.example.schoolforum.pojo.document;

import com.example.schoolforum.pojo.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "schoolforum_users", createIndex = false)
public class UserDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String username;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Keyword, name = "avatar_url")
    private String avatarUrl;

    @Field(type = FieldType.Text)
    private String bio;

    @Field(type = FieldType.Integer)
    private Integer role;

    @Field(type = FieldType.Boolean, name = "is_active")
    private Boolean isActive;

    @Field(type = FieldType.Long, name = "created_at")
    private Long createdAt;

    public static UserDocument fromEntity(Users user) {
        return UserDocument.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole() != null ? user.getRole().getCode() : 2)
                .isActive(user.getIsActive() != null && user.getIsActive().name().equals("ACTIVE"))
                .createdAt(toTimestamp(user.getCreatedAt()))
                .build();
    }

    private static Long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
