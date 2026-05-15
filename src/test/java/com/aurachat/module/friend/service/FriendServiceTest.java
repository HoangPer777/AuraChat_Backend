package com.aurachat.module.friend.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.friend.controller.FriendWebSocketController;
import com.aurachat.module.friend.dto.FriendDto;
import com.aurachat.module.friend.dto.FriendRequestDto;
import com.aurachat.module.friend.dto.SendFriendRequestDto;
import com.aurachat.module.friend.entity.FriendRequest;
import com.aurachat.module.friend.entity.Friendship;
import com.aurachat.module.friend.repository.FriendRequestRepository;
import com.aurachat.module.friend.repository.FriendshipRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private FriendWebSocketController friendWebSocketController;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private FriendService friendService;

    private static final String SENDER = "userA";
    private static final String RECEIVER = "userB";

    private User buildUser(String id, String displayName) {
        return User.builder()
            .id(id)
            .displayName(displayName)
            .email(id + "@example.com")
            .avatarUrl(null)
            .build();
    }

    private FriendRequest buildRequest(String id, String senderId, String receiverId, String status) {
        return FriendRequest.builder()
            .id(id)
            .senderId(senderId)
            .receiverId(receiverId)
            .status(status)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    // ─── sendFriendRequest ─────────────────────────────────────────────────

    @Test
    void sendFriendRequest_savesAndNotifies() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(userRepository.existsById(RECEIVER)).thenReturn(true);
        when(friendshipRepository.existsByUserIdAndFriendId(SENDER, RECEIVER)).thenReturn(false);
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(anyString(), anyString(), anyString()))
            .thenReturn(false);
        when(friendRequestRepository.save(any())).thenAnswer(inv -> {
            FriendRequest saved = inv.getArgument(0);
            saved.setId("req-1");
            return saved;
        });
        when(userRepository.findById(SENDER)).thenReturn(Optional.of(buildUser(SENDER, "Alice")));
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(buildUser(RECEIVER, "Bob")));
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        FriendRequestDto result = friendService.sendFriendRequest(SENDER, new SendFriendRequestDto(RECEIVER));

        assertThat(result.id()).isEqualTo("req-1");
        assertThat(result.sender()).isNotNull();
        verify(friendRequestRepository).save(any());
        verify(friendWebSocketController).notifyFriendRequestCreated(eq(RECEIVER), argThat(requestHasId("req-1")));
    }

    @Test
    void sendFriendRequest_throwsWhenSelfRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> friendService.sendFriendRequest(SENDER, new SendFriendRequestDto(SENDER)))
            .isInstanceOf(BusinessLogicException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FRIEND_SELF_REQUEST.getCode());
    }

    @Test
    void sendFriendRequest_throwsWhenRequestAlreadyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(userRepository.existsById(RECEIVER)).thenReturn(true);
        when(friendshipRepository.existsByUserIdAndFriendId(SENDER, RECEIVER)).thenReturn(false);
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(SENDER, RECEIVER, "PENDING"))
            .thenReturn(true);

        assertThatThrownBy(() -> friendService.sendFriendRequest(SENDER, new SendFriendRequestDto(RECEIVER)))
            .isInstanceOf(BusinessLogicException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FRIEND_REQUEST_EXISTS.getCode());
    }

    // ─── acceptFriendRequest ───────────────────────────────────────────────

    @Test
    void acceptFriendRequest_createsFriendshipAndNotifies() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        FriendRequest request = buildRequest("req-2", SENDER, RECEIVER, "PENDING");
        when(friendRequestRepository.findById("req-2")).thenReturn(Optional.of(request));
        when(friendshipRepository.existsByUserIdAndFriendId(RECEIVER, SENDER)).thenReturn(false);
        when(friendRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(SENDER)).thenReturn(Optional.of(buildUser(SENDER, "Alice")));
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(buildUser(RECEIVER, "Bob")));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        FriendDto friend = friendService.acceptFriendRequest(RECEIVER, "req-2");

        assertThat(friend.id()).isEqualTo(SENDER);
        verify(friendshipRepository, times(2)).save(any(Friendship.class));
        verify(friendWebSocketController).notifyFriendRequestAccepted(eq(SENDER), any(), any());
    }

    // ─── declineFriendRequest ──────────────────────────────────────────────

    @Test
    void declineFriendRequest_updatesStatusAndNotifies() {
        FriendRequest request = buildRequest("req-3", SENDER, RECEIVER, "PENDING");
        when(friendRequestRepository.findById("req-3")).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(SENDER)).thenReturn(Optional.of(buildUser(SENDER, "Alice")));
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(buildUser(RECEIVER, "Bob")));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        friendService.declineFriendRequest(RECEIVER, "req-3");

        verify(friendRequestRepository).save(argThat(req -> "DECLINED".equals(req.getStatus())));
        verify(friendWebSocketController).notifyFriendRequestDeclined(eq(SENDER), any());
    }

    // ─── searchUsers ───────────────────────────────────────────────────────

    @Test
    void searchUsers_throwsWhenQueryBlank() {
        assertThatThrownBy(() -> friendService.searchUsers("  ", SENDER))
            .isInstanceOf(ValidationException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VALIDATION_REQUIRED_FIELD.getCode());
    }

    // ─── getFriendList ─────────────────────────────────────────────────────

    @Test
    void getFriendList_returnsSortedByDisplayName() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(friendshipRepository.findByUserId(SENDER)).thenReturn(List.of(
            Friendship.builder().userId(SENDER).friendId("u1").createdAt(Instant.now()).build(),
            Friendship.builder().userId(SENDER).friendId("u2").createdAt(Instant.now()).build()
        ));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(
            buildUser("u2", "bob"),
            buildUser("u1", "Alice")
        ));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        List<FriendDto> friends = friendService.getFriendList(SENDER);

        assertThat(friends).hasSize(2);
        assertThat(friends.get(0).displayName()).isEqualTo("Alice");
        assertThat(friends.get(1).displayName()).isEqualTo("bob");
    }

    private ArgumentMatcher<FriendRequestDto> requestHasId(String id) {
        return request -> request != null && id.equals(request.id());
    }
}
