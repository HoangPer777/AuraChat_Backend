package com.aurachat.module.message.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.module.message.dto.AddMemberRequest;
import com.aurachat.module.message.dto.ConversationResponse;
import com.aurachat.module.message.dto.CreateConversationRequest;
import com.aurachat.module.message.entity.Conversation;
import com.aurachat.module.message.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationService conversationService;

    private static final String USER_A = "userA";
    private static final String USER_B = "userB";
    private static final String USER_C = "userC";

    private Conversation buildPrivateConv(String id) {
        return Conversation.builder()
            .id(id)
            .type("PRIVATE")
            .members(new ArrayList<>(List.of(
                Conversation.Member.builder().userId(USER_A).role("ADMIN").joinedAt(Instant.now()).build(),
                Conversation.Member.builder().userId(USER_B).role("MEMBER").joinedAt(Instant.now()).build()
            )))
            .createdBy(USER_A)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private Conversation buildGroupConv(String id) {
        return Conversation.builder()
            .id(id)
            .type("GROUP")
            .name("Test Group")
            .members(new ArrayList<>(List.of(
                Conversation.Member.builder().userId(USER_A).role("ADMIN").joinedAt(Instant.now()).build(),
                Conversation.Member.builder().userId(USER_B).role("MEMBER").joinedAt(Instant.now()).build()
            )))
            .createdBy(USER_A)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    // ─── createConversation PRIVATE ───────────────────────────────────────────

    @Test
    void createConversation_private_createsNewWhenNotExists() {
        when(conversationRepository.findPrivateConversation(USER_A, USER_B)).thenReturn(Optional.empty());
        Conversation saved = buildPrivateConv("conv1");
        when(conversationRepository.save(any())).thenReturn(saved);

        CreateConversationRequest req = new CreateConversationRequest("PRIVATE", USER_B, null, null);
        ConversationResponse result = conversationService.createConversation(USER_A, req);

        assertThat(result.id()).isEqualTo("conv1");
        assertThat(result.type()).isEqualTo("PRIVATE");
        verify(conversationRepository).save(any());
    }

    @Test
    void createConversation_private_returnsExistingWhenAlreadyExists() {
        Conversation existing = buildPrivateConv("existingConv");
        when(conversationRepository.findPrivateConversation(USER_A, USER_B)).thenReturn(Optional.of(existing));

        CreateConversationRequest req = new CreateConversationRequest("PRIVATE", USER_B, null, null);
        ConversationResponse result = conversationService.createConversation(USER_A, req);

        assertThat(result.id()).isEqualTo("existingConv");
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void createConversation_private_throwsWhenTargetUserIdMissing() {
        CreateConversationRequest req = new CreateConversationRequest("PRIVATE", null, null, null);
        assertThatThrownBy(() -> conversationService.createConversation(USER_A, req))
            .isInstanceOf(BusinessLogicException.class);
    }

    // ─── createConversation GROUP ─────────────────────────────────────────────

    @Test
    void createConversation_group_throwsWhenNoMembers() {
        CreateConversationRequest req = new CreateConversationRequest("GROUP", null, "Group", List.of());
        assertThatThrownBy(() -> conversationService.createConversation(USER_A, req))
            .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void createConversation_group_createsWithAdminRole() {
        Conversation saved = buildGroupConv("groupConv1");
        when(conversationRepository.save(any())).thenReturn(saved);

        CreateConversationRequest req = new CreateConversationRequest("GROUP", null, "Group", List.of(USER_B));
        ConversationResponse result = conversationService.createConversation(USER_A, req);

        assertThat(result.type()).isEqualTo("GROUP");
        verify(conversationRepository).save(argThat(c ->
            c.getMembers().stream().anyMatch(m -> m.getUserId().equals(USER_A) && "ADMIN".equals(m.getRole()))
        ));
    }

    // ─── getConversationById ──────────────────────────────────────────────────

    @Test
    void getConversationById_throwsWhenNotMember() {
        Conversation conv = buildPrivateConv("conv1");
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> conversationService.getConversationById("conv1", USER_C))
            .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void getConversationById_returnsConvWhenMember() {
        Conversation conv = buildPrivateConv("conv1");
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conv));

        ConversationResponse result = conversationService.getConversationById("conv1", USER_A);
        assertThat(result.id()).isEqualTo("conv1");
    }

    // ─── addMemberToGroup ─────────────────────────────────────────────────────

    @Test
    void addMemberToGroup_throwsWhenNotAdmin() {
        Conversation conv = buildGroupConv("groupConv1");
        when(conversationRepository.findById("groupConv1")).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> conversationService.addMemberToGroup("groupConv1", USER_B, new AddMemberRequest(USER_C)))
            .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void addMemberToGroup_addsNewMemberWhenAdmin() {
        Conversation conv = buildGroupConv("groupConv1");
        when(conversationRepository.findById("groupConv1")).thenReturn(Optional.of(conv));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConversationResponse result = conversationService.addMemberToGroup("groupConv1", USER_A, new AddMemberRequest(USER_C));
        assertThat(result.members()).anyMatch(m -> m.userId().equals(USER_C));
    }

    // ─── removeMemberFromGroup ────────────────────────────────────────────────

    @Test
    void removeMemberFromGroup_adminCanRemoveOther() {
        Conversation conv = buildGroupConv("groupConv1");
        when(conversationRepository.findById("groupConv1")).thenReturn(Optional.of(conv));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConversationResponse result = conversationService.removeMemberFromGroup("groupConv1", USER_A, USER_B);
        assertThat(result.members()).noneMatch(m -> m.userId().equals(USER_B));
    }

    @Test
    void leaveGroup_memberCanLeave() {
        Conversation conv = buildGroupConv("groupConv1");
        when(conversationRepository.findById("groupConv1")).thenReturn(Optional.of(conv));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConversationResponse result = conversationService.leaveGroup("groupConv1", USER_B);
        assertThat(result.members()).noneMatch(m -> m.userId().equals(USER_B));
    }
}
