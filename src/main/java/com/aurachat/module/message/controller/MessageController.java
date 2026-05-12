package com.aurachat.module.message.controller;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.message.dto.MessageResponse;
import com.aurachat.module.message.dto.SendMessageRequest;
import com.aurachat.module.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** GET /api/conversations/{id}/messages — lịch sử tin nhắn có phân trang */
    @GetMapping("/{id}/messages")
    public DataResponse<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return DataResponse.success(
            messageService.getMessageHistory(id, userId, PageRequest.of(page, size))
        );
    }

    /** POST /api/conversations/{id}/messages — gửi tin nhắn qua REST */
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<MessageResponse> sendMessage(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request) {
        // Đảm bảo conversationId trong body khớp với path variable
        SendMessageRequest req = new SendMessageRequest(
            id, request.content(), request.type(),
            request.fileUrl(), request.fileName(), request.fileSize()
        );
        return DataResponse.success(messageService.sendMessage(userId, req), "Message sent");
    }

    /** DELETE /api/conversations/{id}/messages/{messageId} — soft delete */
    @DeleteMapping("/{id}/messages/{messageId}")
    public DataResponse<MessageResponse> deleteMessage(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @PathVariable String messageId) {
        return DataResponse.success(messageService.deleteMessage(messageId, userId), "Message deleted");
    }
}
