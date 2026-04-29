package com.aurachat.module.call.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "call_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CallLog {

    @Id
    private String id;

    private String conversationId;

    @Indexed
    private String callerId;

    @Indexed
    private String receiverId;

    /** VIDEO | AUDIO */
    private String type;

    /** MISSED | DECLINED | COMPLETED */
    private String status;

    private Instant startedAt;

    private Instant endedAt;

    private Long durationSeconds;

    private Instant createdAt;
}
