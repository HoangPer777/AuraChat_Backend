package com.aurachat.module.message.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class CallLogContent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CallLogContent() {
    }

    public static String toJson(String callType, String status, long durationSeconds) {
        return toJson(callType, status, durationSeconds, false, 0);
    }

    public static String toJson(String callType, String status, long durationSeconds, boolean groupCall, int participantCount) {
        try {
            return MAPPER.writeValueAsString(new Payload(
                callType == null ? "VIDEO" : callType,
                status == null ? "DECLINED" : status,
                Math.max(durationSeconds, 0L),
                groupCall,
                Math.max(participantCount, 0)
            ));
        } catch (JsonProcessingException ex) {
            return "{\"callType\":\"VIDEO\",\"status\":\"DECLINED\",\"durationSeconds\":0,\"groupCall\":false,\"participantCount\":0}";
        }
    }

    public static String toPreview(String json) {
        Payload payload = parse(json);
        if (payload == null) {
            return "Cuộc gọi";
        }

        String typeLabel = "AUDIO".equalsIgnoreCase(payload.callType()) ? "Cuộc gọi thoại" : "Cuộc gọi video";
        if (payload.groupCall()) {
            typeLabel += " nhóm";
        }
        String participantSuffix = payload.groupCall() && payload.participantCount() > 0
            ? " · " + payload.participantCount() + " người tham gia"
            : "";
        return switch (payload.status()) {
            case "COMPLETED" -> {
                String duration = formatDuration(payload.durationSeconds());
                yield duration == null
                    ? typeLabel + participantSuffix
                    : typeLabel + participantSuffix + " · " + duration;
            }
            case "MISSED" -> typeLabel + participantSuffix + " · Không trả lời";
            case "DECLINED" -> typeLabel + participantSuffix + " · Không kết nối được";
            default -> typeLabel + participantSuffix;
        };
    }

    private static Payload parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return new Payload(
                node.path("callType").asText("VIDEO"),
                node.path("status").asText("DECLINED"),
                node.path("durationSeconds").asLong(0L),
                node.path("groupCall").asBoolean(false),
                node.path("participantCount").asInt(0)
            );
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private static String formatDuration(long durationSeconds) {
        if (durationSeconds <= 0) {
            return null;
        }
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        if (minutes > 0 && seconds > 0) {
            return minutes + " phút " + seconds + " giây";
        }
        if (minutes > 0) {
            return minutes + " phút";
        }
        return seconds + " giây";
    }

    private record Payload(String callType, String status, long durationSeconds, boolean groupCall, int participantCount) {
    }
}
