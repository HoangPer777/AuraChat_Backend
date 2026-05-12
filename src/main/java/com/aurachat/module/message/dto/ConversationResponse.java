package com.aurachat.module.message.dto;

import com.aurachat.module.message.entity.Conversation;

import java.time.Instant;
import java.util.List;

public record ConversationResponse(
    String id,
    String type,
    String name,
    String avatarUrl,
    List<MemberDto> members,
    LastMessageDto lastMessage,
    Instant updatedAt
) {
    public static ConversationResponse from(Conversation c) {
        List<MemberDto> members = c.getMembers() == null ? List.of() :
            c.getMembers().stream()
                .map(m -> new MemberDto(m.getUserId(), m.getRole(), m.getJoinedAt()))
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
