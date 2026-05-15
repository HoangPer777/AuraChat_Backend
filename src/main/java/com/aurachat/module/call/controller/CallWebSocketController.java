package com.aurachat.module.call.controller;

import com.aurachat.module.call.dto.CallAnswerDto;
import com.aurachat.module.call.dto.CallResponse;
import com.aurachat.module.call.dto.IceCandidateDto;
import com.aurachat.module.call.dto.InitiateCallRequest;
import com.aurachat.module.call.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class CallWebSocketController {

    private final CallService callService;

    @MessageMapping("/call/offer")
    public void offer(InitiateCallRequest request, Principal principal) {
        if (principal == null) return;
        callService.initiateCall(principal.getName(), request);
    }

    @MessageMapping("/call/answer")
    public void answer(CallAnswerDto answer, Principal principal) {
        if (principal == null || answer == null) return;
        callService.acceptCall(answer.callId(), principal.getName(), answer.sdp());
    }

    @MessageMapping("/call/ice-candidate")
    public void iceCandidate(IceCandidateDto candidate, Principal principal) {
        if (principal == null) return;
        callService.relayIceCandidate(principal.getName(), candidate);
    }

    @MessageMapping("/call/end")
    public void end(CallResponse request, Principal principal) {
        if (principal == null || request == null) return;
        if ("DECLINED".equalsIgnoreCase(request.status())) {
            callService.declineCall(request.callId(), principal.getName());
            return;
        }
        callService.endCall(request.callId(), principal.getName());
    }
}
