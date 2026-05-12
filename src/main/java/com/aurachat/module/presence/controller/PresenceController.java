package com.aurachat.module.presence.controller;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.presence.dto.PresenceStatusDto;
import com.aurachat.module.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    /** GET /api/presence/status/{userId} — trạng thái online/offline của user */
    @GetMapping("/status/{userId}")
    public DataResponse<PresenceStatusDto> getStatus(
            @AuthenticationPrincipal String requesterId,
            @PathVariable String userId) {
        return DataResponse.success(presenceService.getPresenceStatus(userId));
    }
}
