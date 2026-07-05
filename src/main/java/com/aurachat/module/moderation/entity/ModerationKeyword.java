package com.aurachat.module.moderation.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "moderation_keywords")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModerationKeyword {

    @Id
    private String id;

    @Indexed(unique = true)
    private String word;

    @Builder.Default
    private boolean enabled = true;

    private Instant createdAt;
}
