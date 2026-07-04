package com.aurachat.module.post.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "posts")
@CompoundIndex(name = "author_created_idx", def = "{'authorId': 1, 'createdAt': -1}")
@CompoundIndex(name = "original_post_idx", def = "{'originalPostId': 1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    private String id;

    @Indexed
    private String authorId;

    private String content;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    /** Bài gốc khi đây là bài chia sẻ */
    private String originalPostId;

    @Builder.Default
    private boolean deleted = false;

    @Indexed
    private Instant createdAt;

    private Instant updatedAt;
}
