package com.aurachat.module.message.integration;

import com.aurachat.module.message.dto.ConversationResponse;
import com.aurachat.module.message.dto.CreateConversationRequest;
import com.aurachat.module.message.dto.MessageResponse;
import com.aurachat.module.message.dto.SendMessageRequest;
import com.aurachat.module.message.entity.Conversation;
import com.aurachat.module.message.entity.Message;
import com.aurachat.module.message.pubsub.MessagePublisher;
import com.aurachat.module.message.repository.ConversationRepository;
import com.aurachat.module.message.repository.MessageRepository;
import com.aurachat.module.message.service.ConversationService;
import com.aurachat.module.message.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests cho Message module với MongoDB thật (Testcontainers).
 * Redis pub/sub được mock để tập trung vào persistence logic.
 */
@SpringBootTest
@Testcontainers
class MessageIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("imagekit.url-endpoint", () -> "https://ik.imagekit.io/test");
        registry.add("imagekit.public-key", () -> "test-public-key");
        registry.add("imagekit.private-key", () -> "test-private-key");
        registry.add("jwt.secret", () -> "test-jwt-secret-32-bytes-minimum-1234567890");
    }

    @Autowired
    private ConversationService conversationService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;

    // Mock Redis để không cần Redis container cho integration test này
    @MockBean
    private MessagePublisher messagePublisher;

    @AfterEach
    void cleanup() {
        conversationRepository.deleteAll();
        messageRepository.deleteAll();
    }

    @Test
    void sendMessage_persistsToMongoDBAndUpdatesLastMessage() {
        // Tạo conversation
        CreateConversationRequest convReq = new CreateConversationRequest("PRIVATE", "userB", null, null);
        ConversationResponse conv = conversationService.createConversation("userA", convReq);

        // Gửi tin nhắn
        SendMessageRequest msgReq = new SendMessageRequest(conv.id(), "Hello from integration test", "TEXT", null, null, null);
        MessageResponse sent = messageService.sendMessage("userA", msgReq);

        // Verify: message được lưu vào MongoDB
        assertThat(sent.id()).isNotNull();
        assertThat(sent.content()).isEqualTo("Hello from integration test");

        // Verify: conversation lastMessage được cập nhật
        Conversation updatedConv = conversationRepository.findById(conv.id()).orElseThrow();
        assertThat(updatedConv.getLastMessage()).isNotNull();
        assertThat(updatedConv.getLastMessage().getContent()).isEqualTo("Hello from integration test");
    }

    @Test
    void getMessageHistory_paginatesCorrectly() {
        // Tạo conversation
        CreateConversationRequest convReq = new CreateConversationRequest("PRIVATE", "userB", null, null);
        ConversationResponse conv = conversationService.createConversation("userA", convReq);

        // Gửi 5 tin nhắn
        for (int i = 0; i < 5; i++) {
            SendMessageRequest req = new SendMessageRequest(conv.id(), "Message " + i, "TEXT", null, null, null);
            messageService.sendMessage("userA", req);
        }

        // Lấy trang đầu với size=3
        List<MessageResponse> page1 = messageService.getMessageHistory(conv.id(), "userA", PageRequest.of(0, 3));
        assertThat(page1).hasSize(3);

        // Lấy trang 2
        List<MessageResponse> page2 = messageService.getMessageHistory(conv.id(), "userA", PageRequest.of(1, 3));
        assertThat(page2).hasSize(2);

        // Verify: sorted DESC
        for (int i = 0; i < page1.size() - 1; i++) {
            assertThat(page1.get(i).createdAt()).isAfterOrEqualTo(page1.get(i + 1).createdAt());
        }
    }

    @Test
    void markAsSeen_updatesSeenByInMongoDB() {
        // Tạo conversation và gửi tin nhắn
        CreateConversationRequest convReq = new CreateConversationRequest("PRIVATE", "userB", null, null);
        ConversationResponse conv = conversationService.createConversation("userA", convReq);
        SendMessageRequest msgReq = new SendMessageRequest(conv.id(), "Hello", "TEXT", null, null, null);
        MessageResponse sent = messageService.sendMessage("userA", msgReq);

        // userB đánh dấu đã xem
        MessageResponse seen = messageService.markAsSeen(sent.id(), "userB", conv.id());

        assertThat(seen.seenBy()).anyMatch(e -> e.userId().equals("userB"));

        // Verify trong MongoDB
        Message msgInDb = messageRepository.findById(sent.id()).orElseThrow();
        assertThat(msgInDb.getSeenBy()).anyMatch(e -> e.getUserId().equals("userB"));
    }
}
