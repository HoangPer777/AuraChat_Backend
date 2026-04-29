package com.aurachat.module.friend.repository;

import com.aurachat.module.friend.entity.FriendRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends MongoRepository<FriendRequest, String> {
    Optional<FriendRequest> findBySenderIdAndReceiverId(String senderId, String receiverId);
    List<FriendRequest> findByReceiverIdAndStatus(String receiverId, String status);
    boolean existsBySenderIdAndReceiverIdAndStatus(String senderId, String receiverId, String status);
}
