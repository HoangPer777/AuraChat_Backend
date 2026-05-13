package com.aurachat.module.friend.controller;

import com.aurachat.module.friend.dto.FriendDto;
import com.aurachat.module.friend.dto.FriendNotificationDto;
import com.aurachat.module.friend.dto.FriendRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class FriendWebSocketController {

    private static final String FRIEND_REQUESTS_DESTINATION = "/queue/friend-requests";

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyFriendRequestCreated(String receiverId, FriendRequestDto request) {
        FriendNotificationDto payload = FriendNotificationDto.requestCreated(request);
        messagingTemplate.convertAndSendToUser(receiverId, FRIEND_REQUESTS_DESTINATION, payload);
    }

    public void notifyFriendRequestAccepted(String senderId, FriendRequestDto request, FriendDto friend) {
        FriendNotificationDto payload = FriendNotificationDto.requestAccepted(request, friend);
        messagingTemplate.convertAndSendToUser(senderId, FRIEND_REQUESTS_DESTINATION, payload);
    }

    public void notifyFriendRequestDeclined(String senderId, FriendRequestDto request) {
        FriendNotificationDto payload = FriendNotificationDto.requestDeclined(request);
        messagingTemplate.convertAndSendToUser(senderId, FRIEND_REQUESTS_DESTINATION, payload);
    }
}
