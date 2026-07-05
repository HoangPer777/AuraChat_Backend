package com.aurachat.module.post.repository;

import com.aurachat.module.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {

    @Query("{ 'authorId': { $in: ?0 }, 'deleted': { $ne: true } }")
    Page<Post> findByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(
        Collection<String> authorIds, Pageable pageable
    );

    @Query("{ 'authorId': ?0, 'deleted': { $ne: true } }")
    Page<Post> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(String authorId, Pageable pageable);

    @Query(value = "{ 'originalPostId': ?0, 'deleted': { $ne: true } }", count = true)
    long countByOriginalPostIdAndDeletedFalse(String originalPostId);

    @Query("{ '_id': { $in: ?0 }, 'deleted': { $ne: true } }")
    List<Post> findByIdInAndDeletedFalse(Collection<String> ids);
}
