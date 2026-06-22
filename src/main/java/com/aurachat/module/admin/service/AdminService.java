package com.aurachat.module.admin.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.admin.dto.*;
import com.aurachat.module.auth.entity.BannedIp;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.BannedIpRepository;
import com.aurachat.module.auth.repository.RefreshTokenRepository;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final BannedIpRepository bannedIpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PresenceService presenceService;
    private final MongoTemplate mongoTemplate;

    public PageResponse<AdminUserDto> getAllUsers(Pageable pageable) {
        return getAllUsers(pageable, null, null, null);
    }

    public PageResponse<AdminUserDto> getAllUsers(Pageable pageable, String queryText,
                                                   String status, String role) {
        List<Criteria> filters = new ArrayList<>();
        if (queryText != null && !queryText.isBlank()) {
            String regex = Pattern.quote(queryText.trim());
            filters.add(new Criteria().orOperator(
                Criteria.where("displayName").regex(regex, "i"),
                Criteria.where("email").regex(regex, "i")
            ));
        }
        if (status != null && !status.isBlank()) filters.add(Criteria.where("status").is(status.toUpperCase()));
        if (role != null && !role.isBlank()) filters.add(Criteria.where("role").is(role.toUpperCase()));

        Query query = new Query();
        if (!filters.isEmpty()) query.addCriteria(new Criteria().andOperator(filters.toArray(Criteria[]::new)));
        long total = mongoTemplate.count(query, User.class);
        query.with(pageable);
        List<AdminUserDto> users = mongoTemplate.find(query, User.class).stream()
            .map(this::toDto)
            .toList();
        return PageResponse.of(users, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    public AdminUserDto getUserById(String userId) {
        return toDto(requireUser(userId));
    }

    public AdminUserDto updateUser(String userId, String adminId, UpdateUserRequest request) {
        User user = requireUser(userId);
        if (request.role() != null && userId.equals(adminId) && !"ADMIN".equals(request.role())) {
            throw new BusinessLogicException(ErrorCode.ADMIN_SELF_ACTION_FORBIDDEN, "Admin cannot remove own role");
        }
        if (request.displayName() != null) user.setDisplayName(request.displayName().trim());
        if (request.role() != null) user.setRole(request.role());
        if (request.bio() != null) user.setBio(request.bio().trim());
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        log.info("Admin action=UPDATE_USER adminId={} targetUserId={}", adminId, userId);
        return toDto(saved);
    }

    public AdminUserDto deactivateUser(String userId, String adminId) {
        rejectSelfAction(userId, adminId);
        User user = requireUser(userId);
        requireStatus(user, "ACTIVE");
        return changeStatus(user, "DEACTIVATED", adminId);
    }

    public AdminUserDto activateUser(String userId, String adminId) {
        User user = requireUser(userId);
        requireStatus(user, "DEACTIVATED");
        return changeStatus(user, "ACTIVE", adminId);
    }

    public AdminUserDto terminateUser(String userId, String adminId) {
        rejectSelfAction(userId, adminId);
        User user = requireUser(userId);
        if ("TERMINATED".equals(user.getStatus())) {
            throw new BusinessLogicException(ErrorCode.ADMIN_INVALID_STATUS_TRANSITION,
                "User is already terminated");
        }
        return changeStatus(user, "TERMINATED", adminId);
    }

    public BannedIpDto banIp(String ipAddress, String reason, String adminId) {
        String normalizedIp = normalizeAndValidateIp(ipAddress);
        if (bannedIpRepository.existsByIpAddress(normalizedIp)) {
            throw new BusinessLogicException(ErrorCode.ADMIN_IP_ALREADY_BANNED, "IP uniqueness");
        }
        BannedIp saved = bannedIpRepository.save(BannedIp.builder()
            .ipAddress(normalizedIp).reason(reason.trim()).bannedBy(adminId).createdAt(Instant.now()).build());
        log.info("Admin action=BAN_IP adminId={} ip={}", adminId, normalizedIp);
        return BannedIpDto.from(saved);
    }

    public void unbanIp(String ipAddress, String adminId) {
        String normalizedIp = normalizeAndValidateIp(ipAddress);
        BannedIp bannedIp = bannedIpRepository.findByIpAddress(normalizedIp)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.ADMIN_IP_NOT_BANNED, "IP must be banned"));
        bannedIpRepository.delete(bannedIp);
        log.info("Admin action=UNBAN_IP adminId={} ip={}", adminId, normalizedIp);
    }

    public PageResponse<BannedIpDto> getBannedIps(Pageable pageable) {
        var page = bannedIpRepository.findAll(pageable);
        return PageResponse.of(page.getContent().stream().map(BannedIpDto::from).toList(),
            page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private AdminUserDto changeStatus(User user, String status, String adminId) {
        user.setStatus(status);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        if (!"ACTIVE".equals(status)) {
            refreshTokenRepository.deleteByUserId(user.getId());
            presenceService.removePresence(user.getId());
        }
        log.info("Admin action=CHANGE_STATUS adminId={} targetUserId={} status={}", adminId, user.getId(), status);
        return toDto(saved);
    }

    private void requireStatus(User user, String expected) {
        if (!expected.equals(user.getStatus())) {
            throw new BusinessLogicException(ErrorCode.ADMIN_INVALID_STATUS_TRANSITION,
                "Expected " + expected + " but was " + user.getStatus());
        }
    }

    private void rejectSelfAction(String userId, String adminId) {
        if (userId.equals(adminId)) {
            throw new BusinessLogicException(ErrorCode.ADMIN_SELF_ACTION_FORBIDDEN, "Self admin action");
        }
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND, "User ID must exist"));
    }

    private AdminUserDto toDto(User user) {
        return AdminUserDto.from(user, presenceService.isOnline(user.getId()));
    }

    private String normalizeAndValidateIp(String value) {
        String ip = value == null ? "" : value.trim().toLowerCase();
        if (!ip.matches("[0-9a-f:.]+")) {
            throw new ValidationException(ErrorCode.VALIDATION_INVALID_FORMAT, "ipAddress", value,
                "Invalid IP address");
        }
        try {
            String normalized = InetAddress.getByName(ip).getHostAddress();
            int zoneIndex = normalized.indexOf('%');
            return zoneIndex >= 0 ? normalized.substring(0, zoneIndex) : normalized;
        } catch (Exception exception) {
            throw new ValidationException(ErrorCode.VALIDATION_INVALID_FORMAT, "ipAddress", value,
                "Invalid IP address");
        }
    }
}
