package com.aurachat.module.media.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "media")
@CompoundIndex(name = "owner_created_idx", def = "{'ownerId': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

    @Id
    private String id;

    @Indexed
    private String ownerId;

    private String fileId;

    private String url;

    private String fileName;

    private String originalFileName;

    private String contentType;

    private long size;

    /** IMAGE | FILE */
    private String mediaType;

    private String provider;

    @Builder.Default
    private boolean deleted = false;

    @Indexed
    private Instant createdAt;

    private Instant deletedAt;
}
