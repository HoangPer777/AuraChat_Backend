package com.aurachat.module.admin.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.post.entity.Post;
import com.aurachat.module.post.entity.PostComment;
import com.aurachat.module.post.repository.PostCommentRepository;
import com.aurachat.module.post.repository.PostLikeRepository;
import com.aurachat.module.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPostServiceTest {

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock PostCommentRepository postCommentRepository;
    @Mock UserRepository userRepository;
    @InjectMocks AdminPostService adminPostService;

    @Test
    void deletePost_softDeletesPost() {
        Post post = post("post-1", "user-1", false);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        adminPostService.deletePost("post-1", "admin-1");

        assertThat(post.isDeleted()).isTrue();
        verify(postRepository).save(post);
    }

    @Test
    void deletePost_throwsWhenAlreadyDeleted() {
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post("post-1", "user-1", true)));

        assertThatThrownBy(() -> adminPostService.deletePost("post-1", "admin-1"))
            .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void deleteComment_removesRepliesForTopLevelComment() {
        PostComment parent = comment("c1", "post-1", null);
        when(postCommentRepository.findById("c1")).thenReturn(Optional.of(parent));

        adminPostService.deleteComment("c1", "admin-1");

        verify(postCommentRepository).deleteByParentCommentId("c1");
        verify(postCommentRepository).delete(parent);
    }

    @Test
    void getPostById_includesCounts() {
        Post post = post("post-1", "user-1", false);
        User author = user("user-1", "Bob");
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(author));
        when(postLikeRepository.countByPostId("post-1")).thenReturn(3L);
        when(postCommentRepository.countByPostId("post-1")).thenReturn(5L);
        when(postRepository.countByOriginalPostIdAndDeletedFalse("post-1")).thenReturn(1L);

        var result = adminPostService.getPostById("post-1");

        assertThat(result.likeCount()).isEqualTo(3);
        assertThat(result.commentCount()).isEqualTo(5);
        assertThat(result.authorDisplayName()).isEqualTo("Bob");
    }

    private Post post(String id, String authorId, boolean deleted) {
        return Post.builder()
            .id(id)
            .authorId(authorId)
            .content("Hello")
            .createdAt(Instant.now())
            .deleted(deleted)
            .build();
    }

    private PostComment comment(String id, String postId, String parentId) {
        return PostComment.builder()
            .id(id)
            .postId(postId)
            .authorId("user-2")
            .content("Nice")
            .parentCommentId(parentId)
            .createdAt(Instant.now())
            .build();
    }

    private User user(String id, String name) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(name);
        user.setEmail("bob@test.com");
        return user;
    }
}
