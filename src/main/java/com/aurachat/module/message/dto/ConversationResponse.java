package com.aurachat.module.message.dto;

import com.aurachat.module.message.entity.Conversation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ConversationResponse(
    String id,
    String type,
    String name,
    String avatarUrl,
    List<MemberDto> members,
    LastMessageDto lastMessage,
    Instant updatedAt
) {
    /**
     * Build from Conversation only (no user info enrichment).
     * Used in tests and places where user lookup is not available.
     */
    public static ConversationResponse from(Conversation c) {
        return from(c, Map.of());
    }

    /**
     * Build from Conversation with optional user info map for enriching member display names.
     * @param userInfoMap map of userId → [displayName, avatarUrl] — may be empty
     */
    public static ConversationResponse from(Conversation c, Map<String, String[]> userInfoMap) {
        List<MemberDto> members = c.getMembers() == null ? List.of() :
            c.getMembers().stream()
                .map(m -> {
                    String[] info = userInfoMap.get(m.getUserId());
                    String displayName = info != null ? info[0] : null;
                    String avatarUrl = info != null ? info[1] : null;
                    return new MemberDto(m.getUserId(), m.getRole(), m.getJoinedAt(), displayName, avatarUrl);
                })
                .toList();

        LastMessageDto lastMsg = null;
        if (c.getLastMessage() != null) {
            var lm = c.getLastMessage();
            lastMsg = new LastMessageDto(lm.getContent(), lm.getSenderId(), lm.getSentAt(), lm.getType());
        }

        return new ConversationResponse(
            c.getId(), c.getType(), c.getName(), c.getAvatarUrl(),
            members, lastMsg, c.getUpdatedAt()
        );
    }
}
