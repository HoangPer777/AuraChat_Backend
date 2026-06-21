package com.aurachat.module.admin.dto;

import com.aurachat.module.auth.entity.BannedIp;
import java.time.Instant;

public record BannedIpDto(String id, String ipAddress, String reason, String bannedBy, Instant createdAt) {
    public static BannedIpDto from(BannedIp bannedIp) {
        return new BannedIpDto(bannedIp.getId(), bannedIp.getIpAddress(), bannedIp.getReason(),
            bannedIp.getBannedBy(), bannedIp.getCreatedAt());
    }
}
