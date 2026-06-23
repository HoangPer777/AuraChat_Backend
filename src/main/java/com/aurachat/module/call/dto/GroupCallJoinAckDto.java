package com.aurachat.module.call.dto;

import java.util.List;

public record GroupCallJoinAckDto(
    String callId,
    String conversationId,
    String groupName,
    String type,
    List<GroupCallParticipantDto> participants
) {
    public String signalType() {
        return "GROUP_JOIN_ACK";
    }
}
