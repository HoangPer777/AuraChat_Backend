package com.aurachat.module.admin.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.module.admin.dto.UpdateUserRequest;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.BannedIpRepository;
import com.aurachat.module.auth.repository.RefreshTokenRepository;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.presence.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock BannedIpRepository bannedIpRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PresenceService presenceService;
    @Mock MongoTemplate mongoTemplate;
    @InjectMocks AdminService adminService;

    @Test
    void deactivateUser_revokesSessionsAndPresence() {
        User target = user("target", "ACTIVE");
        when(userRepository.findById("target")).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        var result = adminService.deactivateUser("target", "admin");

        assertThat(result.status()).isEqualTo("DEACTIVATED");
        verify(refreshTokenRepository).deleteByUserId("target");
        verify(presenceService).removePresence("target");
    }

    @Test
    void activateUser_rejectsTerminatedAccount() {
        when(userRepository.findById("target")).thenReturn(Optional.of(user("target", "TERMINATED")));

        assertThatThrownBy(() -> adminService.activateUser("target", "admin"))
            .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void terminateUser_rejectsSelfAction() {
        assertThatThrownBy(() -> adminService.terminateUser("admin", "admin"))
            .isInstanceOf(BusinessLogicException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateUser_doesNotAllowAdminToRemoveOwnRole() {
        when(userRepository.findById("admin")).thenReturn(Optional.of(user("admin", "ACTIVE")));

        assertThatThrownBy(() -> adminService.updateUser("admin", "admin",
            new UpdateUserRequest(null, "USER", null)))
            .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void banIp_normalizesAndPersistsIpv4() {
        when(bannedIpRepository.existsByIpAddress("127.0.0.1")).thenReturn(false);
        when(bannedIpRepository.save(any())).thenAnswer(invocation -> {
            var value = invocation.getArgument(0, com.aurachat.module.auth.entity.BannedIp.class);
            value.setId("ban-1");
            return value;
        });

        var result = adminService.banIp("127.0.0.1", "abuse", "admin");

        assertThat(result.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(result.bannedBy()).isEqualTo("admin");
    }

    private User user(String id, String status) {
        return User.builder().id(id).email(id + "@example.com").displayName(id)
            .role("ADMIN").status(status).build();
    }
}
