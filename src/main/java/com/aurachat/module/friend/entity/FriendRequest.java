package com.aurachat.module.friend.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "friend_requests")
@CompoundIndex(name = "sender_receiver_idx", def = "{'senderId': 1, 'receiverId': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FriendRequest {

    @Id
    private String id;

    private String senderId;

    private String receiverId;

    /** PENDING | ACCEPTED | DECLINED */
    @Builder.Default
    private String status = "PENDING";

    private Instant createdAt;

    private Instant updatedAt;
}
