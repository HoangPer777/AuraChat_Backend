package com.aurachat.module.friend.dto;

public record FriendNotificationDto(
    String type,
    FriendRequestDto request,
    FriendDto friend
) {
    public static FriendNotificationDto requestCreated(FriendRequestDto request) {
        return new FriendNotificationDto("FRIEND_REQUEST_CREATED", request, null);
    }

    public static FriendNotificationDto requestAccepted(FriendRequestDto request, FriendDto friend) {
        return new FriendNotificationDto("FRIEND_REQUEST_ACCEPTED", request, friend);
    }

    public static FriendNotificationDto requestDeclined(FriendRequestDto request) {
        return new FriendNotificationDto("FRIEND_REQUEST_DECLINED", request, null);
    }
}
