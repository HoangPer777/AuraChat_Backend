package com.aurachat.module.presence.dto;

/** Payload heartbeat từ client — userId lấy từ Principal, field này optional */
public record HeartbeatRequest(String userId) {}
