package com.banking.core.auth.repository;

import com.banking.core.auth.module.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByKeycloakId(String keycloakId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByKeycloakId(String keycloakId);

    boolean existsByUsernameOrEmail(String username, String email);
}
