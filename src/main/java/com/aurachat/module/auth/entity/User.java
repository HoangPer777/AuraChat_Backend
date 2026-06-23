package com.aurachat.module.auth.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

@Document(collection = "users")
@CompoundIndex(name = "provider_providerId_idx", def = "{'provider': 1, 'providerId': 1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    @TextIndexed
    private String email;

    private String passwordHash;

    @TextIndexed
    private String displayName;

    private String avatarUrl;

    private String bio;

    /** LOCAL | GOOGLE | FACEBOOK */
    @Builder.Default
    private String provider = "LOCAL";

    /** ID từ Google/Facebook */
    private String providerId;

    /** USER | ADMIN */
    @Indexed
    @Builder.Default
    private String role = "USER";

    /** ACTIVE | DEACTIVATED | TERMINATED */
    @Indexed
    @Builder.Default
    private String status = "ACTIVE";

    /** Thời điểm hoạt động cuối - dùng cho Presence */
    private Instant lastSeen;

    /** FCM device tokens cho push notification khi offline */
    @Builder.Default
    private List<String> fcmTokens = new ArrayList<>();

    @Indexed
    private Instant createdAt;

    private Instant updatedAt;
}
