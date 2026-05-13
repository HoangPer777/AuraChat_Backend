package com.aurachat.module.friend.dto;

import jakarta.validation.constraints.NotBlank;

public record SendFriendRequestDto(
    @NotBlank
    String receiverId
) {}
