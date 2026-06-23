package com.aurachat.module.media.repository;

import com.aurachat.module.media.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MediaRepository extends MongoRepository<Media, String> {

    Page<Media> findByOwnerIdAndDeletedFalseOrderByCreatedAtDesc(String ownerId, Pageable pageable);

    Optional<Media> findByIdAndDeletedFalse(String id);
}
