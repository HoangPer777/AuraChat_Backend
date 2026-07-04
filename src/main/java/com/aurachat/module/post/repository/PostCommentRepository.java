package com.aurachat.module.post.repository;

import com.aurachat.module.post.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostCommentRepository extends MongoRepository<PostComment, String> {

    Page<PostComment> findByPostIdOrderByCreatedAtAsc(String postId, Pageable pageable);

    long countByPostId(String postId);
}
