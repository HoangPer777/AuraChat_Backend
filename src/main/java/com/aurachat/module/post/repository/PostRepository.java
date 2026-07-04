package com.aurachat.module.post.repository;

import com.aurachat.module.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {

    Page<Post> findByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(
        Collection<String> authorIds, Pageable pageable
    );

    Page<Post> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(String authorId, Pageable pageable);

    long countByOriginalPostIdAndDeletedFalse(String originalPostId);

    List<Post> findByIdInAndDeletedFalse(Collection<String> ids);
}
