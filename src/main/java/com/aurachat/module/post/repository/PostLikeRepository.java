package com.aurachat.module.post.repository;

import com.aurachat.module.post.entity.PostLike;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends MongoRepository<PostLike, String> {

    Optional<PostLike> findByPostIdAndUserId(String postId, String userId);

    long countByPostId(String postId);

    List<PostLike> findByPostIdInAndUserId(Collection<String> postIds, String userId);

    void deleteByPostIdAndUserId(String postId, String userId);
}
