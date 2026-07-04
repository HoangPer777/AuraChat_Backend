package com.aurachat.module.post.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "post_comments")
@CompoundIndex(name = "post_created_idx", def = "{'postId': 1, 'createdAt': 1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostComment {

    @Id
    private String id;

    private String postId;
    private String authorId;
    private String content;
    private Instant createdAt;
}
