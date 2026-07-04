package com.travelease.backend.auth.repository;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.shared.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String email) {
        User user = new User();
        user.setName("Asha");
        user.setEmail(email);
        user.setPhone("9999999999");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.ROLE_TRAVELER);
        return user;
    }

    @Test
    void findByEmailReturnsSavedUser() {
        userRepository.save(newUser("asha@example.com"));

        assertThat(userRepository.findByEmail("asha@example.com")).isPresent();
        assertThat(userRepository.findByEmail("asha@example.com").get().getName()).isEqualTo("Asha");
    }

    @Test
    void existsByEmailIsFalseForUnknownEmail() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void existsByEmailIsTrueAfterSaving() {
        userRepository.save(newUser("known@example.com"));

        assertThat(userRepository.existsByEmail("known@example.com")).isTrue();
    }
}
