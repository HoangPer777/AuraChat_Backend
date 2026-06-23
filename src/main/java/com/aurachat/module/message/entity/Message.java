package com.aurachat.module.message.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "messages")
@CompoundIndex(name = "conv_time_idx", def = "{'conversationId': 1, 'createdAt': -1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Message {

    @Id
    private String id;

    private String conversationId;

    @Indexed
    private String senderId;

    /** TEXT | IMAGE | FILE | CALL_LOG | VOICE */
    private String type;

    private String content;

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private List<SeenEntry> seenBy;

    @Builder.Default
    private boolean isDeleted = false;

    @Indexed
    private Instant createdAt;

    // ─── Embedded: trạng thái đã xem ─────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SeenEntry {
        private String userId;
        private Instant seenAt;
    }
}
