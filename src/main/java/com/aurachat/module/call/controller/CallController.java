package com.aurachat.module.call.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calls")
public class CallController {
    // WebSocket signaling: /app/call/offer, /app/call/answer, /app/call/ice-candidate
    // GET /history
}
