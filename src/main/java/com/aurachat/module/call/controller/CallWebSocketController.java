package com.aurachat.module.call.controller;

import com.aurachat.module.call.dto.CallAnswerDto;
import com.aurachat.module.call.dto.CallResponse;
import com.aurachat.module.call.dto.GroupCallJoinRequest;
import com.aurachat.module.call.dto.GroupCallPeerAnswerDto;
import com.aurachat.module.call.dto.GroupCallPeerOfferDto;
import com.aurachat.module.call.dto.IceCandidateDto;
import com.aurachat.module.call.dto.InitiateCallRequest;
import com.aurachat.module.call.dto.InitiateGroupCallRequest;
import com.aurachat.module.call.service.CallService;
import com.aurachat.module.call.service.GroupCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class CallWebSocketController {

    private final CallService callService;
    private final GroupCallService groupCallService;

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
        if (groupCallService.tryRelayIceCandidate(principal.getName(), candidate)) {
            return;
        }
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

    // ─── Group call ───────────────────────────────────────────────────────────

    @MessageMapping("/call/group/offer")
    public void groupOffer(InitiateGroupCallRequest request, Principal principal) {
        if (principal == null || request == null) return;
        groupCallService.initiateGroupCall(principal.getName(), request);
    }

    @MessageMapping("/call/group/join")
    public void groupJoin(GroupCallJoinRequest request, Principal principal) {
        if (principal == null || request == null) return;
        groupCallService.joinGroupCall(request.callId(), principal.getName());
    }

    @MessageMapping("/call/group/peer-offer")
    public void groupPeerOffer(GroupCallPeerOfferDto offer, Principal principal) {
        if (principal == null || offer == null) return;
        groupCallService.relayPeerOffer(principal.getName(), offer);
    }

    @MessageMapping("/call/group/peer-answer")
    public void groupPeerAnswer(GroupCallPeerAnswerDto answer, Principal principal) {
        if (principal == null || answer == null) return;
        groupCallService.relayPeerAnswer(principal.getName(), answer);
    }

    @MessageMapping("/call/group/leave")
    public void groupLeave(GroupCallJoinRequest request, Principal principal) {
        if (principal == null || request == null) return;
        groupCallService.leaveGroupCall(request.callId(), principal.getName());
    }

    @MessageMapping("/call/group/end")
    public void groupEnd(GroupCallJoinRequest request, Principal principal) {
        if (principal == null || request == null) return;
        groupCallService.endGroupCall(request.callId(), principal.getName());
    }

    @MessageMapping("/call/group/decline")
    public void groupDecline(GroupCallJoinRequest request, Principal principal) {
        if (principal == null || request == null) return;
        groupCallService.declineGroupCall(request.callId(), principal.getName());
    }
}
