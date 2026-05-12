package com.aurachat.module.message.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateConversationRequest(
    @NotNull @Pattern(regexp = "PRIVATE|GROUP") String type,
    /** Dùng cho PRIVATE: userId của người kia */
    String targetUserId,
    /** Dùng cho GROUP: tên nhóm */
    String name,
    /** Dùng cho GROUP: danh sách thành viên (không bao gồm người tạo) */
    List<String> memberIds
) {}
