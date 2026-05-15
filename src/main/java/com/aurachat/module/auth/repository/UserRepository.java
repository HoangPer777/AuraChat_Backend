package com.aurachat.module.auth.repository;

import com.aurachat.module.auth.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    Optional<User> findByProviderId(String providerId);
    
    @Query("{ '_id': { $nin: ?0 } }")
    Page<User> findUsersToDiscover(Collection<String> excludeIds, Pageable pageable);
}
