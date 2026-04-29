package com.aurachat.module.auth.repository;

import com.aurachat.module.auth.entity.BannedIp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BannedIpRepository extends MongoRepository<BannedIp, String> {
    Optional<BannedIp> findByIpAddress(String ipAddress);
    boolean existsByIpAddress(String ipAddress);
}
