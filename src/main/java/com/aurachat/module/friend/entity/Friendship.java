package com.aurachat.module.friend.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Mỗi cặp bạn bè tạo 2 document (A→B và B→A)
 * để query nhanh theo userId.
 */
@Document(collection = "friendships")
@CompoundIndex(name = "user_friend_idx", def = "{'userId': 1, 'friendId': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Friendship {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String friendId;

    private Instant createdAt;
}
