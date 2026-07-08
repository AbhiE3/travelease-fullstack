package com.travelease.backend.auth.repository;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role = :role "
            + "AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) "
            + "ORDER BY u.name")
    List<User> searchByRoleAndNameOrEmail(@Param("role") Role role, @Param("query") String query, Pageable pageable);
}
