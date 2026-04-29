package com.aurachat.module.call.repository;

import com.aurachat.module.call.entity.CallLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface CallLogRepository extends MongoRepository<CallLog, String> {

    /** Lịch sử cuộc gọi của user (gọi đi + gọi đến) */
    @Query("{ $or: [ { 'callerId': ?0 }, { 'receiverId': ?0 } ] }")
    List<CallLog> findByUserId(String userId, Pageable pageable);
}
