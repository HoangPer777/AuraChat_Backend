package com.aurachat.module.moderation.repository;

import com.aurachat.module.moderation.entity.ModerationFlag;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ModerationFlagRepository extends MongoRepository<ModerationFlag, String> {

    boolean existsByContentTypeAndContentIdAndStatus(String contentType, String contentId, String status);

    long countByStatus(String status);

    long countByStatusAndContentType(String status, String contentType);

    Optional<ModerationFlag> findFirstByContentTypeAndContentIdAndStatusOrderByCreatedAtDesc(
        String contentType, String contentId, String status
    );
}
