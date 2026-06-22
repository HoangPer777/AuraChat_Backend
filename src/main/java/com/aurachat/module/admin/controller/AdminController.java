package com.aurachat.module.admin.controller;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.admin.dto.*;
import com.aurachat.module.admin.service.AdminService;
import com.aurachat.module.admin.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AdminService adminService;
    private final StatisticsService statisticsService;

    @GetMapping("/users")
    public DataResponse<PageResponse<AdminUserDto>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return DataResponse.success(adminService.getAllUsers(pageable, q, status, role));
    }

    @GetMapping("/users/{id}")
    public DataResponse<AdminUserDto> getUser(@PathVariable String id) {
        return DataResponse.success(adminService.getUserById(id));
    }

    @PatchMapping("/users/{id}")
    public DataResponse<AdminUserDto> updateUser(@AuthenticationPrincipal String adminId,
                                                  @PathVariable String id,
                                                  @Valid @RequestBody UpdateUserRequest request) {
        return DataResponse.success(adminService.updateUser(id, adminId, request), "User updated");
    }

    @PostMapping("/users/{id}/deactivate")
    public DataResponse<AdminUserDto> deactivate(@AuthenticationPrincipal String adminId, @PathVariable String id) {
        return DataResponse.success(adminService.deactivateUser(id, adminId), "User deactivated");
    }

    @PostMapping("/users/{id}/activate")
    public DataResponse<AdminUserDto> activate(@AuthenticationPrincipal String adminId, @PathVariable String id) {
        return DataResponse.success(adminService.activateUser(id, adminId), "User activated");
    }

    @PostMapping("/users/{id}/terminate")
    public DataResponse<AdminUserDto> terminate(@AuthenticationPrincipal String adminId, @PathVariable String id) {
        return DataResponse.success(adminService.terminateUser(id, adminId), "User terminated");
    }

    @PostMapping("/ban-ip")
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse<BannedIpDto> banIp(@AuthenticationPrincipal String adminId,
                                           @Valid @RequestBody BanIpRequest request) {
        return DataResponse.success(adminService.banIp(request.ipAddress(), request.reason(), adminId), "IP banned");
    }

    @DeleteMapping("/ban-ip/{ipAddress}")
    public DataResponse<Void> unbanIp(@AuthenticationPrincipal String adminId, @PathVariable String ipAddress) {
        adminService.unbanIp(ipAddress, adminId);
        return DataResponse.success("IP unbanned");
    }

    @GetMapping("/banned-ips")
    public DataResponse<PageResponse<BannedIpDto>> getBannedIps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        return DataResponse.success(adminService.getBannedIps(pageable));
    }

    @GetMapping("/statistics")
    public DataResponse<StatisticsResponse> statistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate endExclusive = endDate == null ? LocalDate.now(BUSINESS_ZONE).plusDays(1) : endDate.plusDays(1);
        LocalDate start = startDate == null ? endExclusive.minusDays(1) : startDate;
        Instant startInstant = start.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant endInstant = endExclusive.atStartOfDay(BUSINESS_ZONE).toInstant();
        return DataResponse.success(statisticsService.getStatistics(startInstant, endInstant));
    }
}
