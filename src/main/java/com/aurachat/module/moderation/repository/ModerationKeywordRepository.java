package com.aurachat.module.moderation.repository;

import com.aurachat.module.moderation.entity.ModerationKeyword;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ModerationKeywordRepository extends MongoRepository<ModerationKeyword, String> {

    List<ModerationKeyword> findByEnabledTrue();

    Optional<ModerationKeyword> findByWordIgnoreCase(String word);

    boolean existsByWordIgnoreCase(String word);
}
