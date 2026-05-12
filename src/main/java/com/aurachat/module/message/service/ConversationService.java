package com.aurachat.module.message.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.module.message.dto.AddMemberRequest;
import com.aurachat.module.message.dto.ConversationResponse;
import com.aurachat.module.message.dto.CreateConversationRequest;
import com.aurachat.module.message.entity.Conversation;
import com.aurachat.module.message.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    // ─── Create ──────────────────────────────────────────────────────────────

    public ConversationResponse createConversation(String requesterId, CreateConversationRequest req) {
        if ("PRIVATE".equals(req.type())) {
            if (req.targetUserId() == null || req.targetUserId().isBlank()) {
                throw new BusinessLogicException(ErrorCode.VALIDATION_REQUIRED_FIELD,
                    "targetUserId is required for PRIVATE conversation");
            }
            // Idempotent: trả về existing nếu đã tồn tại
            return conversationRepository.findPrivateConversation(requesterId, req.targetUserId())
                .map(ConversationResponse::from)
                .orElseGet(() -> {
                    Conversation c = Conversation.builder()
                        .type("PRIVATE")
                        .members(List.of(
                            Conversation.Member.builder().userId(requesterId).role("ADMIN").joinedAt(Instant.now()).build(),
                            Conversation.Member.builder().userId(req.targetUserId()).role("MEMBER").joinedAt(Instant.now()).build()
                        ))
                        .createdBy(requesterId)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                    return ConversationResponse.from(conversationRepository.save(c));
                });
        }

        // GROUP
        if (req.memberIds() == null || req.memberIds().size() < 1) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_FAILED,
                "GROUP conversation requires at least 1 other member");
        }

        List<Conversation.Member> members = new ArrayList<>();
        members.add(Conversation.Member.builder().userId(requesterId).role("ADMIN").joinedAt(Instant.now()).build());
        for (String memberId : req.memberIds()) {
            members.add(Conversation.Member.builder().userId(memberId).role("MEMBER").joinedAt(Instant.now()).build());
        }

        Conversation c = Conversation.builder()
            .type("GROUP")
            .name(req.name())
            .members(members)
            .createdBy(requesterId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        return ConversationResponse.from(conversationRepository.save(c));
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public ConversationResponse getConversationById(String conversationId, String requesterId) {
        Conversation c = findAndValidateMember(conversationId, requesterId);
        return ConversationResponse.from(c);
    }

    public List<ConversationResponse> getUserConversations(String userId) {
        return conversationRepository
            .findByMembersUserId(userId, Sort.by(Sort.Direction.DESC, "updatedAt"))
            .stream()
            .map(ConversationResponse::from)
            .toList();
    }

    // ─── Member management ────────────────────────────────────────────────────

    public ConversationResponse addMemberToGroup(String conversationId, String requesterId, AddMemberRequest req) {
        Conversation c = findAndValidateAdmin(conversationId, requesterId);

        boolean alreadyMember = c.getMembers().stream()
            .anyMatch(m -> m.getUserId().equals(req.userId()));
        if (alreadyMember) {
            throw new BusinessLogicException(ErrorCode.CONVERSATION_MEMBER_EXISTS,
                "User is already a member of this conversation");
        }

        List<Conversation.Member> updated = new ArrayList<>(c.getMembers());
        updated.add(Conversation.Member.builder()
            .userId(req.userId()).role("MEMBER").joinedAt(Instant.now()).build());
        c.setMembers(updated);
        c.setUpdatedAt(Instant.now());
        return ConversationResponse.from(conversationRepository.save(c));
    }

    public ConversationResponse removeMemberFromGroup(String conversationId, String requesterId, String targetUserId) {
        Conversation c = findAndValidateMember(conversationId, requesterId);

        // Chỉ ADMIN mới được xóa người khác; thành viên chỉ được tự rời
        if (!requesterId.equals(targetUserId)) {
            boolean isAdmin = c.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(requesterId) && "ADMIN".equals(m.getRole()));
            if (!isAdmin) {
                throw new AuthorizationException("conversation/" + conversationId, "ADMIN");
            }
        }

        List<Conversation.Member> updated = c.getMembers().stream()
            .filter(m -> !m.getUserId().equals(targetUserId))
            .toList();
        c.setMembers(updated);
        c.setUpdatedAt(Instant.now());
        return ConversationResponse.from(conversationRepository.save(c));
    }

    public ConversationResponse leaveGroup(String conversationId, String userId) {
        return removeMemberFromGroup(conversationId, userId, userId);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public Conversation findAndValidateMember(String conversationId, String userId) {
        Conversation c = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.MESSAGE_CONVERSATION_NOT_FOUND, "Conversation not found"));

        boolean isMember = c.getMembers().stream()
            .anyMatch(m -> m.getUserId().equals(userId));
        if (!isMember) {
            throw new AuthorizationException("conversation/" + conversationId, "MEMBER");
        }
        return c;
    }

    private Conversation findAndValidateAdmin(String conversationId, String requesterId) {
        Conversation c = findAndValidateMember(conversationId, requesterId);
        boolean isAdmin = c.getMembers().stream()
            .anyMatch(m -> m.getUserId().equals(requesterId) && "ADMIN".equals(m.getRole()));
        if (!isAdmin) {
            throw new BusinessLogicException(ErrorCode.NOT_GROUP_ADMIN,
                "Only group admin can perform this action");
        }
        return c;
    }
}
