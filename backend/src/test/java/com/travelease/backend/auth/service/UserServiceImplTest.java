package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.RegisterRequest;
import com.travelease.backend.auth.dto.UserResponse;
import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.shared.exception.DuplicateResourceException;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerSavesNewUserWithHashedPasswordAndTravelerRole() {
        RegisterRequest request = new RegisterRequest("Asha", "asha@example.com", "9999999999", "Passw0rd1");
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd1")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return saved;
        });

        UserResponse response = userService.register(request);

        assertThat(response.id()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(response.name()).isEqualTo("Asha");
        assertThat(response.email()).isEqualTo("asha@example.com");
        assertThat(response.role()).isEqualTo(Role.ROLE_TRAVELER.name());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Asha", "asha@example.com", "9999999999", "Passw0rd1");
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getByEmailReturnsUserWhenFound() {
        User user = new User();
        user.setEmail("asha@example.com");
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));

        assertThat(userService.getByEmail("asha@example.com")).isSameAs(user);
    }

    @Test
    void getByEmailThrowsWhenNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmail("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchTravelersReturnsMatchingTravelersMappedToResponses() {
        User user = new User();
        user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setName("Asha Rao");
        user.setEmail("asha@example.com");
        user.setRole(Role.ROLE_TRAVELER);
        when(userRepository.searchByRoleAndNameOrEmail(eq(Role.ROLE_TRAVELER), eq("asha"), any(Pageable.class)))
                .thenReturn(List.of(user));

        List<UserResponse> responses = userService.searchTravelers("asha");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Asha Rao");
        assertThat(responses.get(0).email()).isEqualTo("asha@example.com");
    }

    @Test
    void searchTravelersTrimsQueryBeforeDelegatingToRepository() {
        when(userRepository.searchByRoleAndNameOrEmail(eq(Role.ROLE_TRAVELER), eq("asha"), any(Pageable.class)))
                .thenReturn(List.of());

        userService.searchTravelers("  asha  ");

        verify(userRepository).searchByRoleAndNameOrEmail(eq(Role.ROLE_TRAVELER), eq("asha"), any(Pageable.class));
    }

    @Test
    void searchTravelersReturnsEmptyListForBlankQueryWithoutQueryingRepository() {
        assertThat(userService.searchTravelers("   ")).isEmpty();
        assertThat(userService.searchTravelers(null)).isEmpty();
        verifyNoInteractions(userRepository);
    }
}
