package com.aurachat.module.message.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.message.dto.MessageResponse;
import com.aurachat.module.message.dto.SendMessageRequest;
import com.aurachat.module.message.entity.Conversation;
import com.aurachat.module.message.entity.Message;
import com.aurachat.module.message.pubsub.MessagePublisher;
import com.aurachat.module.message.repository.ConversationRepository;
import com.aurachat.module.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final MessagePublisher messagePublisher;

    // ─── Send ─────────────────────────────────────────────────────────────────

    public MessageResponse sendMessage(String senderId, SendMessageRequest req) {
        validateMessagePayload(req);
        // Validate membership (throws AuthorizationException if not member)
        Conversation conv = conversationService.findAndValidateMember(req.conversationId(), senderId);

        Message msg = Message.builder()
            .conversationId(req.conversationId())
            .senderId(senderId)
            .type(req.type())
            .content(req.content())
            .fileUrl(req.fileUrl())
            .fileName(req.fileName())
            .fileSize(req.fileSize())
            .seenBy(new ArrayList<>())
            .createdAt(Instant.now())
            .build();

        Message saved = messageRepository.save(msg);

        // Update conversation lastMessage
        String preview = req.content() != null && req.content().length() > 100
            ? req.content().substring(0, 100) : req.content();
        conv.setLastMessage(Conversation.LastMessage.builder()
            .content(preview)
            .senderId(senderId)
            .sentAt(saved.getCreatedAt())
            .type(req.type())
            .build());
        conv.setUpdatedAt(Instant.now());
        conversationRepository.save(conv);

        MessageResponse response = MessageResponse.from(saved);

        // Publish to Redis pub/sub (best-effort — không fail nếu Redis down)
        try {
            messagePublisher.publish(req.conversationId(), response);
        } catch (Exception e) {
            log.warn("Redis pub/sub unavailable, message saved but not broadcast: {}", e.getMessage());
        }

        return response;
    }

    private void validateMessagePayload(SendMessageRequest req) {
        if (req == null) {
            throw new ValidationException(
                ErrorCode.VALIDATION_REQUIRED_FIELD,
                "message",
                null,
                "Message payload is required"
            );
        }

        if ("TEXT".equals(req.type())) {
            if (req.content() == null || req.content().trim().isEmpty()) {
                throw new ValidationException(
                    ErrorCode.VALIDATION_REQUIRED_FIELD,
                    "content",
                    req.content(),
                    "Message content is required"
                );
            }
        }

        if ("IMAGE".equals(req.type()) || "FILE".equals(req.type())) {
            if (req.fileUrl() == null || req.fileUrl().trim().isEmpty()) {
                throw new ValidationException(
                    ErrorCode.VALIDATION_REQUIRED_FIELD,
                    "fileUrl",
                    req.fileUrl(),
                    "File URL is required"
                );
            }
        }

        if ("FILE".equals(req.type())) {
            if (req.fileName() == null || req.fileName().trim().isEmpty()) {
                throw new ValidationException(
                    ErrorCode.VALIDATION_REQUIRED_FIELD,
                    "fileName",
                    req.fileName(),
                    "File name is required"
                );
            }
            if (req.fileSize() == null || req.fileSize() <= 0) {
                throw new ValidationException(
                    ErrorCode.VALIDATION_INVALID_FORMAT,
                    "fileSize",
                    req.fileSize(),
                    "File size must be greater than 0"
                );
            }
        }
    }

    // ─── History ──────────────────────────────────────────────────────────────

    public List<MessageResponse> getMessageHistory(String conversationId, String requesterId, Pageable pageable) {
        conversationService.findAndValidateMember(conversationId, requesterId);
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
            .stream()
            .filter(m -> !m.isDeleted())
            .map(MessageResponse::from)
            .toList();
    }

    // ─── Seen ─────────────────────────────────────────────────────────────────

    public MessageResponse markAsSeen(String messageId, String userId, String conversationId) {
        conversationService.findAndValidateMember(conversationId, userId);

        Message msg = messageRepository.findById(messageId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.MESSAGE_NOT_FOUND, "Message not found"));

        List<Message.SeenEntry> seenBy = msg.getSeenBy() == null ? new ArrayList<>() : new ArrayList<>(msg.getSeenBy());

        // Idempotent: chỉ thêm nếu chưa có
        boolean alreadySeen = seenBy.stream().anyMatch(e -> e.getUserId().equals(userId));
        if (!alreadySeen) {
            seenBy.add(Message.SeenEntry.builder().userId(userId).seenAt(Instant.now()).build());
            msg.setSeenBy(seenBy);
            msg = messageRepository.save(msg);
        }

        return MessageResponse.from(msg);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public MessageResponse deleteMessage(String messageId, String requesterId) {
        Message msg = messageRepository.findById(messageId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.MESSAGE_NOT_FOUND, "Message not found"));

        if (!msg.getSenderId().equals(requesterId)) {
            throw new AuthorizationException("message/" + messageId, "OWNER");
        }

        msg.setDeleted(true);
        return MessageResponse.from(messageRepository.save(msg));
    }
}
