package com.aurachat.module.message.repository;

import com.aurachat.module.message.entity.Conversation;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    /** Lấy tất cả conversation của một user, sắp xếp theo tin nhắn mới nhất */
    @Query("{ 'members.userId': ?0 }")
    List<Conversation> findByMembersUserId(String userId, Sort sort);

    /** Tìm conversation PRIVATE giữa 2 user */
    @Query("{ 'type': 'PRIVATE', 'members.userId': { $all: [?0, ?1] } }")
    Optional<Conversation> findPrivateConversation(String userId1, String userId2);
}
