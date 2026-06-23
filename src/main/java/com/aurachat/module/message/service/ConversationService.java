package com.aurachat.module.message.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.message.dto.AddMemberRequest;
import com.aurachat.module.message.dto.ConversationResponse;
import com.aurachat.module.message.dto.CreateConversationRequest;
import com.aurachat.module.message.dto.UpdateConversationRequest;
import com.aurachat.module.message.entity.Conversation;
import com.aurachat.module.message.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final GroupAvatarUploadService groupAvatarUploadService;

    // ─── Create ──────────────────────────────────────────────────────────────

    public ConversationResponse createConversation(String requesterId, CreateConversationRequest req) {
        if ("PRIVATE".equals(req.type())) {
            if (req.targetUserId() == null || req.targetUserId().isBlank()) {
                throw new BusinessLogicException(ErrorCode.VALIDATION_REQUIRED_FIELD,
                    "targetUserId is required for PRIVATE conversation");
            }
            // Idempotent: trả về existing nếu đã tồn tại
            return conversationRepository.findPrivateConversation(requesterId, req.targetUserId())
                .map(this::enrichedResponse)
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
                    return enrichedResponse(conversationRepository.save(c));
                });
        }

        // GROUP
        if (req.name() == null || req.name().isBlank()) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_REQUIRED_FIELD,
                "name is required for GROUP conversation");
        }

        List<String> memberIds = req.memberIds() == null ? List.of() :
            req.memberIds().stream()
                .filter(id -> id != null && !id.isBlank() && !id.equals(requesterId))
                .distinct()
                .toList();

        if (memberIds.isEmpty()) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_FAILED,
                "GROUP conversation requires at least 1 other member");
        }

        List<Conversation.Member> members = new ArrayList<>();
        members.add(Conversation.Member.builder().userId(requesterId).role("ADMIN").joinedAt(Instant.now()).build());
        for (String memberId : memberIds) {
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

        return enrichedResponse(conversationRepository.save(c));
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public ConversationResponse getConversationById(String conversationId, String requesterId) {
        Conversation c = findAndValidateMember(conversationId, requesterId);
        return enrichedResponse(c);
    }

    public List<ConversationResponse> getUserConversations(String userId) {
        return conversationRepository
            .findByMembersUserId(userId, Sort.by(Sort.Direction.DESC, "updatedAt"))
            .stream()
            .map(this::enrichedResponse)
            .toList();
    }

    // ─── Member management ────────────────────────────────────────────────────

    public ConversationResponse addMemberToGroup(String conversationId, String requesterId, AddMemberRequest req) {
        Conversation c = findAndValidateAdmin(conversationId, requesterId);
        validateGroupConversation(c);

        boolean alreadyMember = c.getMembers().stream()
            .anyMatch(m -> m.getUserId().equals(req.userId()));
        if (alreadyMember) {
            throw new BusinessLogicException(ErrorCode.CONVERSATION_MEMBER_EXISTS,
                "User is already a member of this conversation");
        }

        if (!userRepository.existsById(req.userId())) {
            throw new BusinessLogicException(ErrorCode.USER_NOT_FOUND,
                "User not found");
        }

        List<Conversation.Member> updated = new ArrayList<>(c.getMembers());
        updated.add(Conversation.Member.builder()
            .userId(req.userId()).role("MEMBER").joinedAt(Instant.now()).build());
        c.setMembers(updated);
        c.setUpdatedAt(Instant.now());
        return enrichedResponse(conversationRepository.save(c));
    }

    public ConversationResponse removeMemberFromGroup(String conversationId, String requesterId, String targetUserId) {
        Conversation c = findAndValidateMember(conversationId, requesterId);
        validateGroupConversation(c);

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
        return enrichedResponse(conversationRepository.save(c));
    }

    public ConversationResponse leaveGroup(String conversationId, String userId) {
        return removeMemberFromGroup(conversationId, userId, userId);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public ConversationResponse updateGroupConversation(
        String conversationId,
        String requesterId,
        UpdateConversationRequest req
    ) {
        Conversation c = findAndValidateAdmin(conversationId, requesterId);
        validateGroupConversation(c);

        boolean hasName = req.name() != null && !req.name().isBlank();
        boolean hasAvatar = req.avatarUrl() != null && !req.avatarUrl().isBlank();
        if (!hasName && !hasAvatar) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_FAILED,
                "At least one of name or avatarUrl must be provided");
        }

        if (hasName) {
            c.setName(req.name().trim());
        }
        if (hasAvatar) {
            c.setAvatarUrl(req.avatarUrl().trim());
        }
        c.setUpdatedAt(Instant.now());
        return enrichedResponse(conversationRepository.save(c));
    }

    public ConversationResponse uploadGroupAvatar(
        String conversationId,
        String requesterId,
        MultipartFile file
    ) {
        Conversation c = findAndValidateAdmin(conversationId, requesterId);
        validateGroupConversation(c);

        String avatarUrl = groupAvatarUploadService.uploadGroupAvatar(conversationId, file);
        c.setAvatarUrl(avatarUrl);
        c.setUpdatedAt(Instant.now());
        return enrichedResponse(conversationRepository.save(c));
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

    private void validateGroupConversation(Conversation c) {
        if (!"GROUP".equals(c.getType())) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_FAILED,
                "This action is only supported for GROUP conversations");
        }
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

    /**
     * Build ConversationResponse enriched with user displayName and avatarUrl for each member.
     * Performs a single batch lookup to avoid N+1 queries.
     */
    private ConversationResponse enrichedResponse(Conversation c) {
        if (c.getMembers() == null || c.getMembers().isEmpty()) {
            return ConversationResponse.from(c);
        }
        Set<String> memberIds = c.getMembers().stream()
            .map(Conversation.Member::getUserId)
            .collect(Collectors.toSet());
        Map<String, String[]> userInfoMap = userRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(
                User::getId,
                u -> new String[]{ u.getDisplayName(), u.getAvatarUrl() }
            ));
        return ConversationResponse.from(c, userInfoMap);
    }
}
