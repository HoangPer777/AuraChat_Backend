package com.aurachat.module.message.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.module.message.dto.MessageResponse;
import com.aurachat.module.message.dto.SendMessageRequest;
import com.aurachat.module.message.entity.Conversation;
import com.aurachat.module.message.entity.Message;
import com.aurachat.module.message.pubsub.MessagePublisher;
import com.aurachat.module.message.repository.ConversationRepository;
import com.aurachat.module.message.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationService conversationService;
    @Mock
    private MessagePublisher messagePublisher;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    private static final String SENDER = "userA";
    private static final String OTHER = "userB";
    private static final String CONV_ID = "conv1";
    private static final String MSG_ID = "msg1";

    private Conversation buildConv() {
        return Conversation.builder()
            .id(CONV_ID)
            .type("PRIVATE")
            .members(List.of(
                Conversation.Member.builder().userId(SENDER).role("ADMIN").joinedAt(Instant.now()).build(),
                Conversation.Member.builder().userId(OTHER).role("MEMBER").joinedAt(Instant.now()).build()
            ))
            .createdBy(SENDER)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private Message buildMessage(String id, String senderId) {
        return Message.builder()
            .id(id)
            .conversationId(CONV_ID)
            .senderId(senderId)
            .type("TEXT")
            .content("Hello")
            .seenBy(new ArrayList<>())
            .createdAt(Instant.now())
            .build();
    }

    // ─── sendMessage ──────────────────────────────────────────────────────────

    @Test
    void sendMessage_savesMessageAndUpdatesLastMessage() {
        Conversation conv = buildConv();
        when(conversationService.findAndValidateMember(CONV_ID, SENDER)).thenReturn(conv);
        Message saved = buildMessage(MSG_ID, SENDER);
        when(messageRepository.save(any())).thenReturn(saved);
        when(conversationRepository.save(any())).thenReturn(conv);

        SendMessageRequest req = new SendMessageRequest(CONV_ID, "Hello", "TEXT", null, null, null);
        MessageResponse result = messageService.sendMessage(SENDER, req);

        assertThat(result.id()).isEqualTo(MSG_ID);
        assertThat(result.senderId()).isEqualTo(SENDER);
        verify(messageRepository).save(any());
        verify(conversationRepository).save(argThat(c -> c.getLastMessage() != null));
    }

    @Test
    void sendMessage_throwsWhenNotMember() {
        when(conversationService.findAndValidateMember(CONV_ID, OTHER))
            .thenThrow(new AuthorizationException("conversation/" + CONV_ID, "MEMBER"));

        SendMessageRequest req = new SendMessageRequest(CONV_ID, "Hello", "TEXT", null, null, null);
        assertThatThrownBy(() -> messageService.sendMessage(OTHER, req))
            .isInstanceOf(AuthorizationException.class);
    }

    // ─── getMessageHistory ────────────────────────────────────────────────────

    @Test
    void getMessageHistory_returnsOnlyNonDeletedMessages() {
        Conversation conv = buildConv();
        when(conversationService.findAndValidateMember(CONV_ID, SENDER)).thenReturn(conv);

        Message active = buildMessage("msg1", SENDER);
        Message deleted = buildMessage("msg2", SENDER);
        deleted.setDeleted(true);

        Pageable pageable = PageRequest.of(0, 50);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(CONV_ID, pageable))
            .thenReturn(List.of(active, deleted));

        List<MessageResponse> result = messageService.getMessageHistory(CONV_ID, SENDER, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("msg1");
    }

    // ─── markAsSeen ───────────────────────────────────────────────────────────

    @Test
    void markAsSeen_addsSeenEntryWhenNotPresent() {
        Conversation conv = buildConv();
        when(conversationService.findAndValidateMember(CONV_ID, OTHER)).thenReturn(conv);
        Message msg = buildMessage(MSG_ID, SENDER);
        when(messageRepository.findById(MSG_ID)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse result = messageService.markAsSeen(MSG_ID, OTHER, CONV_ID);

        assertThat(result.seenBy()).anyMatch(e -> e.userId().equals(OTHER));
        verify(messageRepository).save(any());
    }

    @Test
    void markAsSeen_isIdempotent_doesNotAddDuplicate() {
        Conversation conv = buildConv();
        when(conversationService.findAndValidateMember(CONV_ID, OTHER)).thenReturn(conv);
        Message msg = buildMessage(MSG_ID, SENDER);
        msg.setSeenBy(new ArrayList<>(List.of(
            Message.SeenEntry.builder().userId(OTHER).seenAt(Instant.now()).build()
        )));
        when(messageRepository.findById(MSG_ID)).thenReturn(Optional.of(msg));

        MessageResponse result = messageService.markAsSeen(MSG_ID, OTHER, CONV_ID);

        assertThat(result.seenBy()).hasSize(1);
        verify(messageRepository, never()).save(any());
    }

    // ─── deleteMessage ────────────────────────────────────────────────────────

    @Test
    void deleteMessage_softDeletesWhenOwner() {
        Message msg = buildMessage(MSG_ID, SENDER);
        when(messageRepository.findById(MSG_ID)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse result = messageService.deleteMessage(MSG_ID, SENDER);

        assertThat(result.isDeleted()).isTrue();
        verify(messageRepository).save(argThat(m -> m.isDeleted()));
    }

    @Test
    void deleteMessage_throwsWhenNotOwner() {
        Message msg = buildMessage(MSG_ID, SENDER);
        when(messageRepository.findById(MSG_ID)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> messageService.deleteMessage(MSG_ID, OTHER))
            .isInstanceOf(AuthorizationException.class);
    }
}
