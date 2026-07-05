package com.aurachat.module.moderation.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "moderation_flags")
@CompoundIndex(name = "content_status_idx", def = "{'contentType': 1, 'contentId': 1, 'status': 1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModerationFlag {

    @Id
    private String id;

    /** POST | COMMENT | MEDIA */
    @Indexed
    private String contentType;

    @Indexed
    private String contentId;

    @Indexed
    private String authorId;

    private String preview;

    @Builder.Default
    private List<String> matchedKeywords = new ArrayList<>();

    /** SENSITIVE_TEXT | SENSITIVE_IMAGE | MANUAL */
    private String reason;

    /** PENDING | DISMISSED | REMOVED | WARNED */
    @Indexed
    @Builder.Default
    private String status = "PENDING";

    private String reviewedBy;
    private Instant reviewedAt;
    private String adminNote;

    @Indexed
    private Instant createdAt;
}
