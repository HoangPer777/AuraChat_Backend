package com.aurachat.module.admin.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.module.admin.dto.AdminPostCommentDto;
import com.aurachat.module.admin.dto.AdminPostDto;
import com.aurachat.module.admin.dto.PageResponse;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.post.entity.Post;
import com.aurachat.module.post.entity.PostComment;
import com.aurachat.module.post.repository.PostCommentRepository;
import com.aurachat.module.post.repository.PostLikeRepository;
import com.aurachat.module.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final MongoTemplate mongoTemplate;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final UserRepository userRepository;

    public PageResponse<AdminPostDto> getAllPosts(
        Pageable pageable,
        String queryText,
        String authorId,
        Boolean includeDeleted
    ) {
        Query query = buildPostQuery(queryText, authorId, includeDeleted);
        long total = mongoTemplate.count(query, Post.class);
        query.with(pageable);
        List<Post> posts = mongoTemplate.find(query, Post.class);
        Map<String, User> authors = loadAuthors(posts);
        List<AdminPostDto> content = posts.stream()
            .map(post -> toDto(post, authors.get(post.getAuthorId())))
            .toList();
        return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    public AdminPostDto getPostById(String postId) {
        Post post = requirePost(postId);
        User author = userRepository.findById(post.getAuthorId()).orElse(null);
        return toDto(post, author);
    }

    public PageResponse<AdminPostCommentDto> getPostComments(String postId, Pageable pageable) {
        requirePost(postId);
        Query query = Query.query(Criteria.where("postId").is(postId));
        long total = mongoTemplate.count(query, PostComment.class);
        query.with(pageable);
        List<PostComment> comments = mongoTemplate.find(query, PostComment.class);
        Map<String, User> authors = loadCommentAuthors(comments);
        List<AdminPostCommentDto> content = comments.stream()
            .map(comment -> AdminPostCommentDto.from(comment, authors.get(comment.getAuthorId())))
            .toList();
        return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    public void deletePost(String postId, String adminId) {
        Post post = requirePost(postId);
        if (post.isDeleted()) {
            throw new BusinessLogicException(ErrorCode.POST_NOT_FOUND, "Post already deleted");
        }
        markPostDeleted(post);
        log.info("Admin action=DELETE_POST adminId={} postId={}", adminId, postId);
        softDeleteSharesOf(postId, adminId);
    }

    /** Xóa mềm mọi bài đăng đang dùng URL ảnh (sau khi admin gỡ media nhạy cảm). */
    public void deletePostsReferencingImageUrl(String imageUrl, String adminId) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        Query query = Query.query(
            Criteria.where("deleted").ne(true).and("imageUrls").is(imageUrl.trim())
        );
        List<Post> posts = mongoTemplate.find(query, Post.class);
        for (Post post : posts) {
            if (post.isDeleted()) {
                continue;
            }
            markPostDeleted(post);
            log.info("Admin action=DELETE_POST_IMAGE_CASCADE adminId={} postId={} imageUrl={}",
                adminId, post.getId(), imageUrl);
            softDeleteSharesOf(post.getId(), adminId);
        }
    }

    public void deleteComment(String commentId, String adminId) {
        PostComment comment = postCommentRepository.findById(commentId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.POST_COMMENT_NOT_FOUND, "Comment not found"));
        if (comment.getParentCommentId() == null) {
            postCommentRepository.deleteByParentCommentId(comment.getId());
        }
        postCommentRepository.delete(comment);
        log.info("Admin action=DELETE_COMMENT adminId={} commentId={} postId={}",
            adminId, commentId, comment.getPostId());
    }

    public long countActivePosts() {
        return mongoTemplate.count(Query.query(Criteria.where("deleted").is(false)), Post.class);
    }

    private AdminPostDto toDto(Post post, User author) {
        return AdminPostDto.from(
            post,
            author,
            postLikeRepository.countByPostId(post.getId()),
            postCommentRepository.countByPostId(post.getId()),
            postRepository.countByOriginalPostIdAndDeletedFalse(post.getId())
        );
    }

    private Query buildPostQuery(String queryText, String authorId, Boolean includeDeleted) {
        List<Criteria> filters = new ArrayList<>();
        if (includeDeleted == null || !includeDeleted) {
            filters.add(Criteria.where("deleted").is(false));
        }
        if (authorId != null && !authorId.isBlank()) {
            filters.add(Criteria.where("authorId").is(authorId.trim()));
        }
        if (queryText != null && !queryText.isBlank()) {
            String trimmed = queryText.trim();
            String regex = Pattern.quote(trimmed);
            List<String> authorIds = findUserIdsBySearch(trimmed);
            List<Criteria> searchCriteria = new ArrayList<>(List.of(
                Criteria.where("content").regex(regex, "i")
            ));
            authorIds.forEach(id -> searchCriteria.add(Criteria.where("authorId").is(id)));
            filters.add(new Criteria().orOperator(searchCriteria.toArray(Criteria[]::new)));
        }
        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(Criteria[]::new)));
        }
        return query;
    }

    private Map<String, User> loadAuthors(List<Post> posts) {
        List<String> authorIds = posts.stream().map(Post::getAuthorId).distinct().toList();
        if (authorIds.isEmpty()) return Map.of();
        return userRepository.findAllById(authorIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));
    }

    private Map<String, User> loadCommentAuthors(List<PostComment> comments) {
        List<String> authorIds = comments.stream().map(PostComment::getAuthorId).distinct().toList();
        if (authorIds.isEmpty()) return Map.of();
        return userRepository.findAllById(authorIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));
    }

    private void markPostDeleted(Post post) {
        post.setDeleted(true);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
    }

    private void softDeleteSharesOf(String originalPostId, String adminId) {
        Query query = Query.query(
            Criteria.where("originalPostId").is(originalPostId).and("deleted").ne(true)
        );
        for (Post share : mongoTemplate.find(query, Post.class)) {
            markPostDeleted(share);
            log.info("Admin action=DELETE_SHARE_CASCADE adminId={} sharePostId={} originalPostId={}",
                adminId, share.getId(), originalPostId);
        }
    }

    private Post requirePost(String postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.POST_NOT_FOUND, "Post not found"));
        return post;
    }

    private List<String> findUserIdsBySearch(String queryText) {
        String regex = Pattern.quote(queryText.trim());
        Query userQuery = Query.query(new Criteria().orOperator(
            Criteria.where("displayName").regex(regex, "i"),
            Criteria.where("email").regex(regex, "i")
        ));
        return mongoTemplate.find(userQuery, User.class).stream().map(User::getId).toList();
    }
}
