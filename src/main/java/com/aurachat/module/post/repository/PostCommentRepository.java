package com.aurachat.module.post.repository;

import com.aurachat.module.post.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostCommentRepository extends MongoRepository<PostComment, String> {

    Page<PostComment> findByPostIdAndParentCommentIdIsNullOrderByCreatedAtAsc(String postId, Pageable pageable);

    List<PostComment> findByParentCommentIdInOrderByCreatedAtAsc(Collection<String> parentCommentIds);

    Optional<PostComment> findByIdAndPostId(String id, String postId);

    long countByPostId(String postId);

    void deleteByParentCommentId(String parentCommentId);
}
