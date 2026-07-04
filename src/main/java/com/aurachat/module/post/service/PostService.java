package com.aurachat.module.post.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.friend.repository.FriendshipRepository;
import com.aurachat.module.post.dto.*;
import com.aurachat.module.post.entity.Post;
import com.aurachat.module.post.entity.PostComment;
import com.aurachat.module.post.entity.PostLike;
import com.aurachat.module.post.repository.PostCommentRepository;
import com.aurachat.module.post.repository.PostLikeRepository;
import com.aurachat.module.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_IMAGES = 10;

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public PostResponse createPost(String userId, CreatePostRequest req) {
        validatePostContent(req.content(), req.imageUrls());

        Post post = Post.builder()
            .authorId(userId)
            .content(normalizeContent(req.content()))
            .imageUrls(sanitizeImageUrls(req.imageUrls()))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        postRepository.save(post);
        return toPostResponse(post, userId, Map.of(), Map.of());
    }

    public PostPageResponse getFeed(String userId, int page, int size) {
        Set<String> authorIds = getFeedAuthorIds(userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(authorIds, pageable);
        List<PostResponse> content = enrichPosts(postPage.getContent(), userId);
        return PostPageResponse.of(content, page, size, postPage.getTotalElements());
    }

    public PostPageResponse getUserPosts(String viewerId, String authorId, int page, int size) {
        ensureCanViewUserPosts(viewerId, authorId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(authorId, pageable);
        List<PostResponse> content = enrichPosts(postPage.getContent(), viewerId);
        return PostPageResponse.of(content, page, size, postPage.getTotalElements());
    }

    public void deletePost(String userId, String postId) {
        Post post = findActivePost(postId);
        if (!post.getAuthorId().equals(userId)) {
            throw new AuthorizationException(
                ErrorCode.POST_ACCESS_DENIED,
                "Only author can delete post",
                "delete post"
            );
        }
        post.setDeleted(true);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
    }

    public PostResponse toggleLike(String userId, String postId) {
        Post post = findActivePost(postId);
        ensureCanViewPost(userId, post);

        Optional<PostLike> existing = postLikeRepository.findByPostIdAndUserId(postId, userId);
        boolean likedAfter;
        if (existing.isPresent()) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            likedAfter = false;
        } else {
            postLikeRepository.save(PostLike.builder()
                .postId(postId)
                .userId(userId)
                .createdAt(Instant.now())
                .build());
            likedAfter = true;
        }

        Map<String, Boolean> likedMap = Map.of(postId, likedAfter);
        return toPostResponse(post, userId, loadOriginalPosts(List.of(post)), likedMap);
    }

    public CommentResponse addComment(String userId, String postId, CreateCommentRequest req) {
        Post post = findActivePost(postId);
        ensureCanViewPost(userId, post);

        PostComment comment = PostComment.builder()
            .postId(postId)
            .authorId(userId)
            .content(req.content().trim())
            .createdAt(Instant.now())
            .build();
        postCommentRepository.save(comment);

        User author = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND, "User not found"));
        return toCommentResponse(comment, author);
    }

    public CommentPageResponse getComments(String userId, String postId, int page, int size) {
        Post post = findActivePost(postId);
        ensureCanViewPost(userId, post);

        Pageable pageable = PageRequest.of(page, size);
        Page<PostComment> commentPage = postCommentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
        Map<String, User> users = loadUsers(commentPage.getContent().stream()
            .map(PostComment::getAuthorId)
            .collect(Collectors.toSet()));

        List<CommentResponse> content = commentPage.getContent().stream()
            .map(comment -> toCommentResponse(comment, users.get(comment.getAuthorId())))
            .toList();

        return CommentPageResponse.of(content, page, size, commentPage.getTotalElements());
    }

    public PostResponse sharePost(String userId, String postId, SharePostRequest req) {
        Post original = findActivePost(postId);
        ensureCanViewPost(userId, original);

        Post share = Post.builder()
            .authorId(userId)
            .content(normalizeContent(req.content()))
            .originalPostId(original.getId())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        postRepository.save(share);
        Map<String, Post> originals = Map.of(original.getId(), original);
        return toPostResponse(share, userId, originals, Map.of());
    }

    private Set<String> getFeedAuthorIds(String userId) {
        Set<String> authorIds = friendshipRepository.findByUserId(userId).stream()
            .map(f -> f.getFriendId())
            .collect(Collectors.toCollection(HashSet::new));
        authorIds.add(userId);
        return authorIds;
    }

    private void ensureCanViewUserPosts(String viewerId, String authorId) {
        if (viewerId.equals(authorId)) return;
        if (!friendshipRepository.existsByUserIdAndFriendId(viewerId, authorId)) {
            throw new AuthorizationException(
                ErrorCode.POST_ACCESS_DENIED,
                "Cannot view posts from non-friend user",
                "view user posts"
            );
        }
    }

    private void ensureCanViewPost(String viewerId, Post post) {
        if (viewerId.equals(post.getAuthorId())) return;

        String originalAuthorId = post.getOriginalPostId() != null
            ? findActivePost(post.getOriginalPostId()).getAuthorId()
            : post.getAuthorId();

        if (!viewerId.equals(originalAuthorId)
            && !friendshipRepository.existsByUserIdAndFriendId(viewerId, post.getAuthorId())
            && !friendshipRepository.existsByUserIdAndFriendId(viewerId, originalAuthorId)) {
            throw new AuthorizationException(
                ErrorCode.POST_ACCESS_DENIED,
                "Cannot view this post",
                "view post"
            );
        }
    }

    private List<PostResponse> enrichPosts(List<Post> posts, String viewerId) {
        if (posts.isEmpty()) return List.of();

        Map<String, Post> originals = loadOriginalPosts(posts);
        Set<String> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Map<String, Boolean> likedMap = loadLikedPostIds(postIds, viewerId);

        Set<String> userIds = new HashSet<>();
        posts.forEach(p -> {
            userIds.add(p.getAuthorId());
            if (p.getOriginalPostId() != null) {
                Post original = originals.get(p.getOriginalPostId());
                if (original != null) userIds.add(original.getAuthorId());
            }
        });
        Map<String, User> users = loadUsers(userIds);

        return posts.stream()
            .map(post -> toPostResponse(post, viewerId, originals, likedMap, users))
            .toList();
    }

    private PostResponse toPostResponse(
        Post post,
        String viewerId,
        Map<String, Post> originals,
        Map<String, Boolean> likedMap
    ) {
        Set<String> userIds = new HashSet<>();
        userIds.add(post.getAuthorId());
        if (post.getOriginalPostId() != null) {
            Post original = originals.get(post.getOriginalPostId());
            if (original != null) userIds.add(original.getAuthorId());
        }
        return toPostResponse(post, viewerId, originals, likedMap, loadUsers(userIds));
    }

    private PostResponse toPostResponse(
        Post post,
        String viewerId,
        Map<String, Post> originals,
        Map<String, Boolean> likedMap,
        Map<String, User> users
    ) {
        User author = users.get(post.getAuthorId());
        PostSummary originalSummary = null;
        if (post.getOriginalPostId() != null) {
            Post original = originals.get(post.getOriginalPostId());
            if (original != null) {
                User originalAuthor = users.get(original.getAuthorId());
                originalSummary = new PostSummary(
                    original.getId(),
                    toAuthorSummary(originalAuthor, original.getAuthorId()),
                    original.getContent(),
                    original.getImageUrls(),
                    original.getCreatedAt()
                );
            }
        }

        return new PostResponse(
            post.getId(),
            toAuthorSummary(author, post.getAuthorId()),
            post.getContent(),
            post.getImageUrls(),
            post.getOriginalPostId(),
            originalSummary,
            postLikeRepository.countByPostId(post.getId()),
            postCommentRepository.countByPostId(post.getId()),
            postRepository.countByOriginalPostIdAndDeletedFalse(post.getId()),
            Boolean.TRUE.equals(likedMap.get(post.getId())),
            post.getCreatedAt()
        );
    }

    private Map<String, Post> loadOriginalPosts(List<Post> posts) {
        Set<String> originalIds = posts.stream()
            .map(Post::getOriginalPostId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (originalIds.isEmpty()) return Map.of();
        return postRepository.findByIdInAndDeletedFalse(originalIds).stream()
            .collect(Collectors.toMap(Post::getId, Function.identity()));
    }

    private Map<String, Boolean> loadLikedPostIds(Collection<String> postIds, String userId) {
        if (postIds.isEmpty()) return Map.of();
        return postLikeRepository.findByPostIdInAndUserId(postIds, userId).stream()
            .collect(Collectors.toMap(PostLike::getPostId, like -> true));
    }

    private Map<String, User> loadUsers(Collection<String> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private CommentResponse toCommentResponse(PostComment comment, User author) {
        return new CommentResponse(
            comment.getId(),
            toAuthorSummary(author, comment.getAuthorId()),
            comment.getContent(),
            comment.getCreatedAt()
        );
    }

    private AuthorSummary toAuthorSummary(User user, String userId) {
        if (user == null) {
            return new AuthorSummary(userId, "Người dùng", null);
        }
        return new AuthorSummary(user.getId(), user.getDisplayName(), user.getAvatarUrl());
    }

    private Post findActivePost(String postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.POST_NOT_FOUND, "Post not found"));
        if (post.isDeleted()) {
            throw new BusinessLogicException(ErrorCode.POST_NOT_FOUND, "Post not found");
        }
        return post;
    }

    private void validatePostContent(String content, List<String> imageUrls) {
        String normalized = normalizeContent(content);
        List<String> images = sanitizeImageUrls(imageUrls);
        if ((normalized == null || normalized.isBlank()) && images.isEmpty()) {
            throw new ValidationException(
                ErrorCode.POST_INVALID_CONTENT,
                "content",
                content,
                "Post must have text or at least one image"
            );
        }
    }

    private String normalizeContent(String content) {
        return content == null ? null : content.trim();
    }

    private List<String> sanitizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return List.of();
        return imageUrls.stream()
            .filter(url -> url != null && !url.isBlank())
            .limit(MAX_IMAGES)
            .toList();
    }
}
