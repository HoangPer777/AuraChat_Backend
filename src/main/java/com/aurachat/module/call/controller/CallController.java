package com.aurachat.module.call.controller;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.call.dto.CallLogDto;
import com.aurachat.module.call.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    /** GET /api/calls/history — lịch sử cuộc gọi có phân trang */
    @GetMapping("/history")
    public DataResponse<List<CallLogDto>> getCallHistory(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return DataResponse.success(
            callService.getCallHistory(userId, PageRequest.of(page, size))
        );
    }
}
