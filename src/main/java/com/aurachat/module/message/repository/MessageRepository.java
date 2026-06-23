package com.aurachat.module.message.repository;

import com.aurachat.module.message.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    /** Lấy tin nhắn theo conversationId, phân trang cursor-based (cũ → mới) */
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId, Pageable pageable);
}
