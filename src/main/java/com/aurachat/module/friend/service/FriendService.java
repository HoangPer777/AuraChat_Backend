package com.aurachat.module.friend.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.friend.controller.FriendWebSocketController;
import com.aurachat.module.friend.dto.FriendDto;
import com.aurachat.module.friend.dto.FriendRequestDto;
import com.aurachat.module.friend.dto.SendFriendRequestDto;
import com.aurachat.module.friend.dto.UserSearchResultDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aurachat.module.friend.entity.FriendRequest;
import com.aurachat.module.friend.entity.Friendship;
import com.aurachat.module.friend.repository.FriendRequestRepository;
import com.aurachat.module.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private static final int SEARCH_LIMIT = 50;
    private static final Duration FRIEND_LIST_TTL = Duration.ofMinutes(10);
    private static final Duration PENDING_COUNT_TTL = Duration.ofMinutes(10);
    private static final Duration SEARCH_TTL = Duration.ofMinutes(5);
    private static final Duration FRIEND_IDS_TTL = Duration.ofHours(12);
    private static final int SEND_REQUEST_LIMIT_PER_MIN = 5;
    private static final Duration SEND_REQUEST_WINDOW = Duration.ofMinutes(1);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_DECLINED = "DECLINED";

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final FriendWebSocketController friendWebSocketController;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public FriendRequestDto sendFriendRequest(String senderId, SendFriendRequestDto req) {
        enforceSendRequestRateLimit(senderId);
        String receiverId = req.receiverId();

        if (senderId.equals(receiverId)) {
            throw new BusinessLogicException(
                ErrorCode.FRIEND_SELF_REQUEST,
                "Cannot send friend request to yourself",
                "senderId != receiverId"
            );
        }

        ensureUserExists(receiverId);

        if (friendshipRepository.existsByUserIdAndFriendId(senderId, receiverId)) {
            throw new BusinessLogicException(
                ErrorCode.FRIEND_ALREADY_EXISTS,
                "Users are already friends",
                "friendship must not already exist"
            );
        }

        if (friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, STATUS_PENDING)
            || friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(receiverId, senderId, STATUS_PENDING)) {
            throw new BusinessLogicException(
                ErrorCode.FRIEND_REQUEST_EXISTS,
                "Friend request already exists",
                "no duplicate pending request"
            );
        }

        FriendRequest friendRequest = FriendRequest.builder()
            .senderId(senderId)
            .receiverId(receiverId)
            .status(STATUS_PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        friendRequestRepository.save(friendRequest);
        FriendRequestDto response = toFriendRequestDto(friendRequest);
        friendWebSocketController.notifyFriendRequestCreated(receiverId, response);
        incrementPendingCount(receiverId);
        invalidateSearchCache(senderId);
        invalidateSearchCache(receiverId);
        return response;
    }

    public FriendDto acceptFriendRequest(String userId, String requestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.FRIEND_REQUEST_NOT_FOUND,
                "Friend request not found",
                "request exists"
            ));

        if (!Objects.equals(friendRequest.getReceiverId(), userId)) {
            throw new AuthorizationException("friend_request", "accept", userId);
        }

        if (!STATUS_PENDING.equals(friendRequest.getStatus())) {
            throw new BusinessLogicException(
                ErrorCode.FRIEND_REQUEST_NOT_FOUND,
                "Friend request already processed",
                "request status is PENDING"
            );
        }

        String senderId = friendRequest.getSenderId();

        if (friendshipRepository.existsByUserIdAndFriendId(userId, senderId)) {
            throw new BusinessLogicException(
                ErrorCode.FRIEND_ALREADY_EXISTS,
                "Users are already friends",
                "friendship must not already exist"
            );
        }

        Instant now = Instant.now();
        friendshipRepository.save(Friendship.builder()
            .userId(userId)
            .friendId(senderId)
            .createdAt(now)
            .build());

        friendshipRepository.save(Friendship.builder()
            .userId(senderId)
            .friendId(userId)
            .createdAt(now)
            .build());

        friendRequest.setStatus(STATUS_ACCEPTED);
        friendRequest.setUpdatedAt(now);
        friendRequestRepository.save(friendRequest);
        decrementPendingCount(userId);
        invalidateFriendListCache(userId, senderId);
        invalidateSearchCache(userId);
        invalidateSearchCache(senderId);
        addFriendIds(userId, senderId);

        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found",
                "sender exists"
            ));

        FriendDto friend = toFriendDto(sender, now);
        FriendRequestDto requestDto = toFriendRequestDto(friendRequest);
        friendWebSocketController.notifyFriendRequestAccepted(senderId, requestDto, friend);
        return friend;
    }

    public void declineFriendRequest(String userId, String requestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.FRIEND_REQUEST_NOT_FOUND,
                "Friend request not found",
                "request exists"
            ));

        if (!Objects.equals(friendRequest.getReceiverId(), userId)) {
            throw new AuthorizationException("friend_request", "decline", userId);
        }

        if (!STATUS_PENDING.equals(friendRequest.getStatus())) {
            throw new BusinessLogicException(
                ErrorCode.FRIEND_REQUEST_NOT_FOUND,
                "Friend request already processed",
                "request status is PENDING"
            );
        }

        friendRequest.setStatus(STATUS_DECLINED);
        friendRequest.setUpdatedAt(Instant.now());
        friendRequestRepository.save(friendRequest);
        decrementPendingCount(userId);
        FriendRequestDto requestDto = toFriendRequestDto(friendRequest);
        friendWebSocketController.notifyFriendRequestDeclined(friendRequest.getSenderId(), requestDto);
    }

    public List<FriendRequestDto> getPendingRequests(String userId) {
        List<FriendRequest> requests = friendRequestRepository
            .findByReceiverIdAndStatus(userId, STATUS_PENDING);
        cachePendingCount(userId, requests.size());
        return toFriendRequestDtos(requests);
    }

    public List<FriendDto> getFriendList(String userId) {
        List<FriendDto> cached = getCachedFriendList(userId);
        if (cached != null) {
            return cached;
        }

        List<Friendship> friendships = friendshipRepository.findByUserId(userId);
        if (friendships.isEmpty()) {
            cacheFriendList(userId, List.of());
            return List.of();
        }

        Set<String> friendIds = friendships.stream()
            .map(Friendship::getFriendId)
            .collect(Collectors.toSet());

        Map<String, User> usersById = userRepository.findAllById(friendIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        List<FriendDto> friends = new ArrayList<>();
        for (Friendship friendship : friendships) {
            User user = usersById.get(friendship.getFriendId());
            if (user != null) {
                friends.add(toFriendDto(user, friendship.getCreatedAt()));
            }
        }

        friends.sort(Comparator.comparing(FriendDto::displayName, String.CASE_INSENSITIVE_ORDER));
        cacheFriendList(userId, friends);
        cacheFriendIds(userId, friendIds);
        return friends;
    }

    public void unfriend(String userId, String friendId) {
        friendshipRepository.deleteByUserIdAndFriendId(userId, friendId);
        friendshipRepository.deleteByUserIdAndFriendId(friendId, userId);
        invalidateFriendListCache(userId, friendId);
        removeFriendIds(userId, friendId);
    }

    public List<UserSearchResultDto> searchUsers(String query, String currentUserId) {
        if (query == null || query.trim().isEmpty()) {
            throw new ValidationException(
                ErrorCode.VALIDATION_REQUIRED_FIELD,
                "query",
                query,
                "Search query is required"
            );
        }

        String normalizedQuery = query.trim().toLowerCase();
        List<UserSearchResultDto> cached = getCachedSearchResults(currentUserId, normalizedQuery);
        if (cached != null) {
            return cached;
        }

        Set<String> friendIds = getCachedFriendIds(currentUserId);
        if (friendIds == null) {
            List<Friendship> friendships = friendshipRepository.findByUserId(currentUserId);
            friendIds = friendships.stream()
                .map(Friendship::getFriendId)
                .collect(Collectors.toSet());
            cacheFriendIds(currentUserId, friendIds);
        }

        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(normalizedQuery);
        TextQuery textQuery = TextQuery.queryText(textCriteria)
            .sortByScore()
            .limit(SEARCH_LIMIT);

        textQuery.addCriteria(Criteria.where("id").ne(currentUserId));
        if (!friendIds.isEmpty()) {
            textQuery.addCriteria(Criteria.where("id").nin(friendIds));
        }

        List<User> users = mongoTemplate.find(textQuery, User.class);

        List<UserSearchResultDto> results = users.stream()
            .map(this::toUserSearchResultDto)
            .collect(Collectors.toList());
        cacheSearchResults(currentUserId, normalizedQuery, results);
        return results;
    }

    private void enforceSendRequestRateLimit(String userId) {
        String key = sendRequestRateKey(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, SEND_REQUEST_WINDOW);
        }
        if (count != null && count > SEND_REQUEST_LIMIT_PER_MIN) {
            throw new ValidationException(
                ErrorCode.VALIDATION_FAILED,
                "rateLimit",
                count,
                "Too many friend requests. Please try again later."
            );
        }
    }

    private void ensureUserExists(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found",
                "user exists"
            );
        }
    }

    private FriendRequestDto toFriendRequestDto(FriendRequest request) {
        User sender = userRepository.findById(request.getSenderId()).orElse(null);
        User receiver = userRepository.findById(request.getReceiverId()).orElse(null);

        return new FriendRequestDto(
            request.getId(),
            sender == null ? null : toUserSummaryDto(sender),
            receiver == null ? null : toUserSummaryDto(receiver),
            request.getStatus(),
            request.getCreatedAt()
        );
    }

    private List<FriendRequestDto> toFriendRequestDtos(List<FriendRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        Set<String> userIds = requests.stream()
            .flatMap(request -> java.util.stream.Stream.of(request.getSenderId(), request.getReceiverId()))
            .collect(Collectors.toSet());

        Map<String, User> usersById = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        return requests.stream()
            .map(request -> new FriendRequestDto(
                request.getId(),
                toUserSummaryDto(usersById.get(request.getSenderId())),
                toUserSummaryDto(usersById.get(request.getReceiverId())),
                request.getStatus(),
                request.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    private FriendRequestDto.UserSummaryDto toUserSummaryDto(User user) {
        if (user == null) {
            return null;
        }
        return new FriendRequestDto.UserSummaryDto(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            user.getAvatarUrl()
        );
    }

    private FriendDto toFriendDto(User user, Instant since) {
        return new FriendDto(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            user.getAvatarUrl(),
            since
        );
    }

    private UserSearchResultDto toUserSearchResultDto(User user) {
        return new UserSearchResultDto(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            user.getAvatarUrl()
        );
    }

    private void cacheFriendList(String userId, List<FriendDto> friends) {
        try {
            String key = friendListKey(userId);
            String payload = objectMapper.writeValueAsString(friends);
            redisTemplate.opsForValue().set(key, payload, FRIEND_LIST_TTL);
        } catch (JsonProcessingException ignored) {
        }
    }

    private List<FriendDto> getCachedFriendList(String userId) {
        String payload = redisTemplate.opsForValue().get(friendListKey(userId));
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<List<FriendDto>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void cachePendingCount(String userId, int count) {
        String key = pendingCountKey(userId);
        redisTemplate.opsForValue().set(key, String.valueOf(count), PENDING_COUNT_TTL);
    }

    private void incrementPendingCount(String userId) {
        String key = pendingCountKey(userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, PENDING_COUNT_TTL);
        }
    }

    private void decrementPendingCount(String userId) {
        String key = pendingCountKey(userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Long updated = redisTemplate.opsForValue().decrement(key);
            if (updated != null && updated < 0) {
                redisTemplate.opsForValue().set(key, "0", PENDING_COUNT_TTL);
            }
        }
    }

    private void cacheSearchResults(String userId, String query, List<UserSearchResultDto> results) {
        try {
            String payload = objectMapper.writeValueAsString(results);
            redisTemplate.opsForValue().set(searchKey(userId, query), payload, SEARCH_TTL);
        } catch (JsonProcessingException ignored) {
        }
    }

    private List<UserSearchResultDto> getCachedSearchResults(String userId, String query) {
        String payload = redisTemplate.opsForValue().get(searchKey(userId, query));
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<List<UserSearchResultDto>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void invalidateFriendListCache(String... userIds) {
        List<String> keys = new ArrayList<>();
        for (String userId : userIds) {
            keys.add(friendListKey(userId));
        }
        redisTemplate.delete(keys);
    }

    private void invalidateSearchCache(String userId) {
        Set<String> keys = redisTemplate.keys(searchKey(userId, "*"));
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private Set<String> getCachedFriendIds(String userId) {
        String key = friendIdsKey(userId);
        Set<String> friendIds = redisTemplate.opsForSet().members(key);
        if (friendIds == null || friendIds.isEmpty()) {
            return null;
        }
        return friendIds;
    }

    private void cacheFriendIds(String userId, Set<String> friendIds) {
        if (friendIds == null || friendIds.isEmpty()) {
            return;
        }
        String key = friendIdsKey(userId);
        redisTemplate.delete(key);
        redisTemplate.opsForSet().add(key, friendIds.toArray(new String[0]));
        redisTemplate.expire(key, FRIEND_IDS_TTL);
    }

    private void addFriendIds(String userId, String friendId) {
        String keyUser = friendIdsKey(userId);
        String keyFriend = friendIdsKey(friendId);
        redisTemplate.opsForSet().add(keyUser, friendId);
        redisTemplate.opsForSet().add(keyFriend, userId);
        redisTemplate.expire(keyUser, FRIEND_IDS_TTL);
        redisTemplate.expire(keyFriend, FRIEND_IDS_TTL);
    }

    private void removeFriendIds(String userId, String friendId) {
        redisTemplate.opsForSet().remove(friendIdsKey(userId), friendId);
        redisTemplate.opsForSet().remove(friendIdsKey(friendId), userId);
    }

    private String friendListKey(String userId) {
        return "friend:list:" + userId;
    }

    private String pendingCountKey(String userId) {
        return "friend:pending:count:" + userId;
    }

    private String searchKey(String userId, String query) {
        return "friend:search:" + userId + ":" + query;
    }

    private String friendIdsKey(String userId) {
        return "friend:ids:" + userId;
    }

    private String sendRequestRateKey(String userId) {
        return "friend:rate:send:" + userId;
    }
}
