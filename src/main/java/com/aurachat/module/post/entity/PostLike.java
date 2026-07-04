package com.aurachat.module.post.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "post_likes")
@CompoundIndex(name = "post_user_unique_idx", def = "{'postId': 1, 'userId': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostLike {

    @Id
    private String id;

    private String postId;
    private String userId;
    private Instant createdAt;
}
