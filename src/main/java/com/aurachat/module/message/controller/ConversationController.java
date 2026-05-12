package com.aurachat.module.message.controller;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.message.dto.AddMemberRequest;
import com.aurachat.module.message.dto.ConversationResponse;
import com.aurachat.module.message.dto.CreateConversationRequest;
import com.aurachat.module.message.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /** GET /api/conversations — danh sách conversation của user hiện tại */
    @GetMapping
    public DataResponse<List<ConversationResponse>> getMyConversations(
            @AuthenticationPrincipal String userId) {
        return DataResponse.success(conversationService.getUserConversations(userId));
    }

    /** POST /api/conversations — tạo conversation mới */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<ConversationResponse> createConversation(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateConversationRequest request) {
        return DataResponse.success(conversationService.createConversation(userId, request), "Conversation created");
    }

    /** GET /api/conversations/{id} — chi tiết conversation */
    @GetMapping("/{id}")
    public DataResponse<ConversationResponse> getConversation(
            @AuthenticationPrincipal String userId,
            @PathVariable String id) {
        return DataResponse.success(conversationService.getConversationById(id, userId));
    }

    /** POST /api/conversations/{id}/members — thêm thành viên */
    @PostMapping("/{id}/members")
    public DataResponse<ConversationResponse> addMember(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @Valid @RequestBody AddMemberRequest request) {
        return DataResponse.success(conversationService.addMemberToGroup(id, userId, request), "Member added");
    }

    /** DELETE /api/conversations/{id}/members/{targetUserId} — xóa/rời nhóm */
    @DeleteMapping("/{id}/members/{targetUserId}")
    public DataResponse<ConversationResponse> removeMember(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @PathVariable String targetUserId) {
        return DataResponse.success(conversationService.removeMemberFromGroup(id, userId, targetUserId), "Member removed");
    }
}
