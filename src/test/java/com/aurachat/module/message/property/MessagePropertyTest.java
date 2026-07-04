package com.aurachat.module.message.property;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.module.message.dto.SendMessageRequest;
import com.aurachat.module.message.entity.Conversation;
import com.aurachat.module.message.entity.Message;
import com.aurachat.module.message.pubsub.MessagePublisher;
import com.aurachat.module.message.repository.ConversationRepository;
import com.aurachat.module.message.repository.MessageRepository;
import com.aurachat.module.message.service.ConversationService;
import com.aurachat.module.message.service.MessageService;
import com.aurachat.module.notification.service.PushNotificationService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Size;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Message module.
 * Kiểm tra các tính chất đúng đắn bất biến.
 */
class MessagePropertyTest {

    private final MessageRepository messageRepository = Mockito.mock(MessageRepository.class);
    private final ConversationRepository conversationRepository = Mockito.mock(ConversationRepository.class);
    private final ConversationService conversationService = Mockito.mock(ConversationService.class);
    private final MessagePublisher messagePublisher = Mockito.mock(MessagePublisher.class);
    private final SimpMessagingTemplate messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
    private final PushNotificationService pushNotificationService = Mockito.mock(PushNotificationService.class);
    private final MessageService messageService = new MessageService(
        messageRepository, conversationRepository, conversationService, messagePublisher, messagingTemplate, pushNotificationService
    );

    /**
     * Property 1: Tin nhắn luôn được trả về theo thứ tự createdAt tăng dần (cũ → mới).
     * Với bất kỳ danh sách timestamps nào, kết quả phải sorted ASC.
     */
    @Property
    void messagesShouldBeOrderedByCreatedAtAsc(
            @ForAll @Size(min = 1, max = 20) List<Long> epochSeconds) {

        String convId = "conv-prop-test";
        String userId = "user-prop";

        Conversation conv = Conversation.builder()
            .id(convId)
            .type("PRIVATE")
            .members(List.of(
                Conversation.Member.builder().userId(userId).role("MEMBER").joinedAt(Instant.now()).build()
            ))
            .createdBy(userId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(conversationService.findAndValidateMember(convId, userId)).thenReturn(conv);

        // Tạo messages với timestamps từ input
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < epochSeconds.size(); i++) {
            messages.add(Message.builder()
                .id("msg-" + i)
                .conversationId(convId)
                .senderId(userId)
                .type("TEXT")
                .content("msg " + i)
                .seenBy(new ArrayList<>())
                .createdAt(Instant.ofEpochSecond(Math.abs(epochSeconds.get(i)) % 1_000_000_000L))
                .build());
        }

        // Repository trả về messages đã sorted ASC (simulate MongoDB sort)
        List<Message> sorted = messages.stream()
            .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
            .toList();
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(eq(convId), any()))
            .thenReturn(sorted);

        var result = messageService.getMessageHistory(convId, userId, PageRequest.of(0, 100));

        // Verify: kết quả phải sorted ASC (cũ → mới)
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).createdAt())
                .isBeforeOrEqualTo(result.get(i + 1).createdAt());
        }
    }

    /**
     * Property 2: Người không phải thành viên không thể gửi tin nhắn.
     * Với bất kỳ userId nào không trong memberIds, sendMessage phải throw AuthorizationException.
     */
    @Property
    void nonMemberCannotSendMessage(
            @ForAll @NotBlank String nonMemberId,
            @ForAll @NotBlank String convId) {

        // Assume nonMemberId không phải member
        when(conversationService.findAndValidateMember(convId, nonMemberId))
            .thenThrow(new AuthorizationException("conversation/" + convId, "MEMBER"));

        SendMessageRequest req = new SendMessageRequest(convId, "Hello", "TEXT", null, null, null);

        assertThatThrownBy(() -> messageService.sendMessage(nonMemberId, req))
            .isInstanceOf(AuthorizationException.class);
    }

    /**
     * Property 3: markAsSeen là idempotent.
     * Gọi nhiều lần với cùng userId thì seenBy chỉ có 1 entry cho userId đó.
     */
    @Property
    @Report(Reporting.GENERATED)
    void markAsSeenIsIdempotent(@ForAll @NotBlank String userId) {
        String convId = "conv-seen";
        String msgId = "msg-seen";

        Conversation conv = Conversation.builder()
            .id(convId)
            .type("PRIVATE")
            .members(List.of(
                Conversation.Member.builder().userId(userId).role("MEMBER").joinedAt(Instant.now()).build()
            ))
            .createdBy("other")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(conversationService.findAndValidateMember(convId, userId)).thenReturn(conv);

        // Lần 1: chưa có seen entry
        Message msg = Message.builder()
            .id(msgId)
            .conversationId(convId)
            .senderId("other")
            .type("TEXT")
            .content("hi")
            .seenBy(new ArrayList<>())
            .createdAt(Instant.now())
            .build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result1 = messageService.markAsSeen(msgId, userId, convId);
        assertThat(result1.seenBy().stream().filter(e -> e.userId().equals(userId)).count()).isEqualTo(1);

        // Lần 2: đã có seen entry — simulate với message đã có entry
        Message msgWithSeen = Message.builder()
            .id(msgId)
            .conversationId(convId)
            .senderId("other")
            .type("TEXT")
            .content("hi")
            .seenBy(new ArrayList<>(List.of(
                Message.SeenEntry.builder().userId(userId).seenAt(Instant.now()).build()
            )))
            .createdAt(Instant.now())
            .build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msgWithSeen));

        var result2 = messageService.markAsSeen(msgId, userId, convId);
        assertThat(result2.seenBy().stream().filter(e -> e.userId().equals(userId)).count()).isEqualTo(1);
    }
}
