package com.aurachat.module.message.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {

    @Id
    private String id;

    /** PRIVATE | GROUP */
    private String type;

    /** Tên nhóm — null nếu là chat cá nhân */
    private String name;

    private String avatarUrl;

    private List<Member> members;

    private LastMessage lastMessage;

    private String createdBy;

    private Instant createdAt;

    @Indexed
    private Instant updatedAt;

    // ─── Embedded: thành viên nhóm ───────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Member {
        private String userId;
        /** MEMBER | ADMIN */
        @Builder.Default
        private String role = "MEMBER";
        private Instant joinedAt;
    }

    // ─── Embedded: tin nhắn cuối ─────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LastMessage {
        private String content;
        private String senderId;
        private Instant sentAt;
        /** TEXT | IMAGE | FILE | CALL */
        private String type;
    }
}
