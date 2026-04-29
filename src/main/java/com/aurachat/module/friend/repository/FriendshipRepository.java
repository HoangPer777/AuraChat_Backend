package com.aurachat.module.friend.repository;

import com.aurachat.module.friend.entity.Friendship;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends MongoRepository<Friendship, String> {
    List<Friendship> findByUserId(String userId);
    Optional<Friendship> findByUserIdAndFriendId(String userId, String friendId);
    boolean existsByUserIdAndFriendId(String userId, String friendId);
    void deleteByUserIdAndFriendId(String userId, String friendId);
}
