package com.aurachat.module.presence.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {
    // GET /status/{userId}
    // WebSocket: connect/disconnect events handled via listener
}
