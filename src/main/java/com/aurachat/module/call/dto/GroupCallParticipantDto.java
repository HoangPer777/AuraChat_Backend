package com.aurachat.module.call.dto;

import java.util.List;

public record GroupCallParticipantDto(
    String userId,
    String displayName,
    String avatarUrl
) {}
