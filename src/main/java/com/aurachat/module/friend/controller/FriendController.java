package com.aurachat.module.friend.controller;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.friend.dto.FriendDto;
import com.aurachat.module.friend.dto.FriendRequestDto;
import com.aurachat.module.friend.dto.SendFriendRequestDto;
import com.aurachat.module.friend.dto.UserSearchResultDto;
import com.aurachat.module.friend.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/search")
    public ResponseEntity<DataResponse<List<UserSearchResultDto>>> searchUsers(
        @RequestParam("q") String query,
        @AuthenticationPrincipal String userId
    ) {
        List<UserSearchResultDto> results = friendService.searchUsers(query, userId);
        return ResponseEntity.ok(DataResponse.success(results, "Search completed"));
    }

    @PostMapping("/request")
    public ResponseEntity<DataResponse<FriendRequestDto>> sendFriendRequest(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody SendFriendRequestDto req
    ) {
        FriendRequestDto request = friendService.sendFriendRequest(userId, req);
        return ResponseEntity.ok(DataResponse.success(request, "Friend request sent"));
    }

    @PutMapping("/requests/{requestId}/accept")
    public ResponseEntity<DataResponse<FriendDto>> acceptFriendRequest(
        @AuthenticationPrincipal String userId,
        @PathVariable String requestId
    ) {
        FriendDto friend = friendService.acceptFriendRequest(userId, requestId);
        return ResponseEntity.ok(DataResponse.success(friend, "Friend request accepted"));
    }

    @PutMapping("/requests/{requestId}/decline")
    public ResponseEntity<DataResponse<Void>> declineFriendRequest(
        @AuthenticationPrincipal String userId,
        @PathVariable String requestId
    ) {
        friendService.declineFriendRequest(userId, requestId);
        return ResponseEntity.ok(DataResponse.success("Friend request declined"));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<DataResponse<List<FriendRequestDto>>> getPendingRequests(
        @AuthenticationPrincipal String userId
    ) {
        List<FriendRequestDto> requests = friendService.getPendingRequests(userId);
        return ResponseEntity.ok(DataResponse.success(requests, "Pending requests retrieved"));
    }

    @GetMapping
    public ResponseEntity<DataResponse<List<FriendDto>>> getFriendList(
        @AuthenticationPrincipal String userId
    ) {
        List<FriendDto> friends = friendService.getFriendList(userId);
        return ResponseEntity.ok(DataResponse.success(friends, "Friend list retrieved"));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<DataResponse<Void>> unfriend(
        @AuthenticationPrincipal String userId,
        @PathVariable String friendId
    ) {
        friendService.unfriend(userId, friendId);
        return ResponseEntity.ok(DataResponse.success("Unfriended successfully"));
    }
}
