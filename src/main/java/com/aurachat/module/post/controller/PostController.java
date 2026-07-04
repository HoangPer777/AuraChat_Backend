package com.aurachat.module.post.controller;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.post.dto.*;
import com.aurachat.module.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<DataResponse<PostResponse>> createPost(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody CreatePostRequest req
    ) {
        PostResponse response = postService.createPost(userId, req);
        return ResponseEntity.ok(DataResponse.success(response, "Post created successfully"));
    }

    @GetMapping("/feed")
    public ResponseEntity<DataResponse<PostPageResponse>> getFeed(
        @AuthenticationPrincipal String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        PostPageResponse response = postService.getFeed(userId, page, size);
        return ResponseEntity.ok(DataResponse.success(response));
    }

    @GetMapping("/user/{authorId}")
    public ResponseEntity<DataResponse<PostPageResponse>> getUserPosts(
        @AuthenticationPrincipal String userId,
        @PathVariable String authorId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        PostPageResponse response = postService.getUserPosts(userId, authorId, page, size);
        return ResponseEntity.ok(DataResponse.success(response));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<DataResponse<Void>> deletePost(
        @AuthenticationPrincipal String userId,
        @PathVariable String postId
    ) {
        postService.deletePost(userId, postId);
        return ResponseEntity.ok(DataResponse.success("Post deleted successfully"));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<DataResponse<PostResponse>> toggleLike(
        @AuthenticationPrincipal String userId,
        @PathVariable String postId
    ) {
        PostResponse response = postService.toggleLike(userId, postId);
        return ResponseEntity.ok(DataResponse.success(response, "Like updated"));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<DataResponse<CommentPageResponse>> getComments(
        @AuthenticationPrincipal String userId,
        @PathVariable String postId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        CommentPageResponse response = postService.getComments(userId, postId, page, size);
        return ResponseEntity.ok(DataResponse.success(response));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<DataResponse<CommentResponse>> addComment(
        @AuthenticationPrincipal String userId,
        @PathVariable String postId,
        @Valid @RequestBody CreateCommentRequest req
    ) {
        CommentResponse response = postService.addComment(userId, postId, req);
        return ResponseEntity.ok(DataResponse.success(response, "Comment added"));
    }

    @PostMapping("/{postId}/share")
    public ResponseEntity<DataResponse<PostResponse>> sharePost(
        @AuthenticationPrincipal String userId,
        @PathVariable String postId,
        @Valid @RequestBody(required = false) SharePostRequest req
    ) {
        SharePostRequest body = req != null ? req : new SharePostRequest(null);
        PostResponse response = postService.sharePost(userId, postId, body);
        return ResponseEntity.ok(DataResponse.success(response, "Post shared successfully"));
    }
}
