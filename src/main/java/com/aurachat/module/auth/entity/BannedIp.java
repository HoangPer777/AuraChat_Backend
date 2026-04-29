package com.aurachat.module.auth.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "banned_ips")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BannedIp {

    @Id
    private String id;

    @Indexed(unique = true)
    private String ipAddress;

    private String reason;

    private String bannedBy;

    private Instant createdAt;
}
