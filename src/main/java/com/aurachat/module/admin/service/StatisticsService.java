package com.aurachat.module.admin.service;

import com.aurachat.module.admin.dto.StatisticsResponse;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.message.entity.Message;
import com.aurachat.module.presence.service.PresenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private final MongoTemplate mongoTemplate;
    private final PresenceService presenceService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    public StatisticsResponse getStatistics(Instant startDate, Instant endDate) {
        if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
            throw new ValidationException("dateRange", startDate + " - " + endDate,
                "startDate must be before endDate");
        }
        String cacheKey = "admin:statistics:" + startDate.toEpochMilli() + ":" + endDate.toEpochMilli();
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return redisObjectMapper.readValue(cached, StatisticsResponse.class);
            } catch (Exception ignored) {
                redisTemplate.delete(cacheKey);
            }
        }

        long dau = getDailyActiveUsers(startDate, endDate);
        long messages = getMessageVolume(startDate, endDate);
        long newUsers = getNewUsersCount(startDate, endDate);
        StatisticsResponse response = new StatisticsResponse(startDate, endDate, dau, messages,
            newUsers, getOnlineUsersCount(), Instant.now());
        try {
            redisTemplate.opsForValue().set(cacheKey, redisObjectMapper.writeValueAsString(response), CACHE_TTL);
        } catch (Exception ignored) {
            // Statistics remain available even if cache serialization fails.
        }
        return response;
    }

    public long getDailyActiveUsers(Instant startDate, Instant endDate) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("createdAt").gte(startDate).lt(endDate)),
            Aggregation.group("senderId"),
            Aggregation.count().as("count")
        );
        CountResult result = mongoTemplate.aggregate(aggregation, Message.class, CountResult.class)
            .getUniqueMappedResult();
        return result == null ? 0 : result.count();
    }

    public long getMessageVolume(Instant startDate, Instant endDate) {
        return mongoTemplate.count(dateRangeQuery(startDate, endDate), Message.class);
    }

    public long getNewUsersCount(Instant startDate, Instant endDate) {
        return mongoTemplate.count(dateRangeQuery(startDate, endDate), User.class);
    }

    public long getOnlineUsersCount() {
        return presenceService.getOnlineUsersCount();
    }

    private Query dateRangeQuery(Instant startDate, Instant endDate) {
        return Query.query(Criteria.where("createdAt").gte(startDate).lt(endDate));
    }

    private record CountResult(long count) {}
}
