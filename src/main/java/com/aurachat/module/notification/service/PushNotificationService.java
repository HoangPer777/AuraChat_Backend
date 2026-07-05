package com.aurachat.module.notification.service;

import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.auth.service.FcmTokenService;
import com.aurachat.module.call.dto.CallOfferDto;
import com.aurachat.module.call.dto.GroupCallInviteDto;
import com.aurachat.module.friend.dto.FriendRequestDto;
import com.aurachat.module.message.dto.MessageResponse;
import com.aurachat.module.message.util.CallLogContent;
import com.aurachat.module.presence.service.PresenceService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final FcmTokenService fcmTokenService;

    public void notifyNewMessage(String recipientId, MessageResponse message) {
        if (message == null || recipientId == null || recipientId.equals(message.senderId())) {
            return;
        }
        if (presenceService.isOnline(recipientId)) {
            return;
        }

        String preview = buildMessagePreview(message);
        sendToUser(recipientId, "MESSAGE", "Tin nhắn mới", preview, Map.of(
            "route", "/chat/window",
            "conversationId", message.conversationId() == null ? "" : message.conversationId(),
            "messageId", message.id() == null ? "" : message.id(),
            "tag", "msg-" + message.conversationId()
        ));
    }

    public void notifyFriendRequest(String recipientId, FriendRequestDto request) {
        if (recipientId == null || request == null) {
            return;
        }
        if (presenceService.isOnline(recipientId)) {
            return;
        }

        String senderName = request.sender() != null && request.sender().displayName() != null
            ? request.sender().displayName()
            : "Ai đó";

        sendToUser(recipientId, "FRIEND_REQUEST", "Lời mời kết bạn",
            senderName + " muốn kết bạn với bạn", Map.of(
                "route", "/notifications",
                "requestId", request.id() == null ? "" : request.id(),
                "tag", "friend-" + request.id()
            ));
    }

    public void notifyIncomingCall(String recipientId, CallOfferDto offer) {
        if (recipientId == null || offer == null) {
            return;
        }
        if (presenceService.isOnline(recipientId)) {
            return;
        }

        String callerName = userRepository.findById(offer.callerId())
            .map(User::getDisplayName)
            .orElse("Cuộc gọi đến");
        String callType = "AUDIO".equals(offer.type()) ? "thoại" : "video";

        sendToUser(recipientId, "CALL", callerName,
            "Cuộc gọi " + callType + " đến", Map.of(
                "route", "/call/incoming",
                "callId", offer.callId() == null ? "" : offer.callId(),
                "tag", "call-" + offer.callId()
            ));
    }

    public void notifyGroupCallInvite(String recipientId, GroupCallInviteDto invite) {
        if (recipientId == null || invite == null) {
            return;
        }
        if (presenceService.isOnline(recipientId)) {
            return;
        }

        String groupName = invite.groupName() != null && !invite.groupName().isBlank()
            ? invite.groupName()
            : "Cuộc gọi nhóm";
        String callType = "AUDIO".equals(invite.type()) ? "thoại" : "video";

        sendToUser(recipientId, "CALL", groupName,
            "Cuộc gọi " + callType + " nhóm", Map.of(
                "route", "/call/incoming",
                "callId", invite.callId() == null ? "" : invite.callId(),
                "conversationId", invite.conversationId() == null ? "" : invite.conversationId(),
                "tag", "call-" + invite.callId()
            ));
    }

    public void notifyModerationWarning(String userId, String message) {
        if (userId == null || message == null || message.isBlank()) {
            return;
        }
        sendToUser(userId, "MODERATION", "Cảnh báo nội dung",
            message, Map.of(
                "route", "/profile",
                "tag", "moderation-warn"
            ), true);
    }

    private void sendToUser(String userId, String type, String title, String body, Map<String, String> data) {
        sendToUser(userId, type, title, body, data, false);
    }

    private void sendToUser(
        String userId,
        String type,
        String title,
        String body,
        Map<String, String> data,
        boolean forceWhenOnline
    ) {
        if (!isFirebaseReady()) {
            return;
        }
        if (!forceWhenOnline && presenceService.isOnline(userId)) {
            return;
        }

        userRepository.findById(userId).ifPresent(user -> {
            List<String> tokens = user.getFcmTokens();
            if (tokens == null || tokens.isEmpty()) {
                return;
            }

            Map<String, String> payload = new HashMap<>(data);
            payload.put("type", type);
            payload.put("title", title);
            payload.put("body", body);

            List<String> invalidTokens = new ArrayList<>();
            for (String token : tokens) {
                try {
                    Message message = Message.builder()
                        .setToken(token)
                        .putAllData(payload)
                        .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                        .build();
                    FirebaseMessaging.getInstance().send(message);
                } catch (FirebaseMessagingException ex) {
                    log.warn("FCM send failed for user {} token {}: {}", userId, token, ex.getMessage());
                    if (isInvalidToken(ex)) {
                        invalidTokens.add(token);
                    }
                } catch (Exception ex) {
                    log.warn("FCM send error for user {}: {}", userId, ex.getMessage());
                }
            }

            invalidTokens.forEach(token -> fcmTokenService.removeInvalidToken(userId, token));
        });
    }

    private boolean isFirebaseReady() {
        return !FirebaseApp.getApps().isEmpty();
    }

    private boolean isInvalidToken(FirebaseMessagingException ex) {
        String code = ex.getMessagingErrorCode() == null ? "" : ex.getMessagingErrorCode().name();
        return "UNREGISTERED".equals(code)
            || "INVALID_ARGUMENT".equals(code)
            || "NOT_FOUND".equals(code);
    }

    private String buildMessagePreview(MessageResponse message) {
        if ("IMAGE".equals(message.type())) {
            return "Đã gửi một hình ảnh";
        }
        if ("STICKER".equals(message.type())) {
            return "Sticker";
        }
        if ("VOICE".equals(message.type())) {
            return "Tin nhắn thoại";
        }
        if ("FILE".equals(message.type())) {
            return "Đã gửi một tệp";
        }
        if ("CALL_LOG".equals(message.type())) {
            return CallLogContent.toPreview(message.content());
        }
        if (message.content() == null || message.content().isBlank()) {
            return "Tin nhắn mới";
        }
        return message.content().length() > 100
            ? message.content().substring(0, 100)
            : message.content();
    }
}
