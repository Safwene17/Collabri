package org.example.userservice.repositories;

import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    long countByRole(Role role);
    long countByVerifiedTrue();
    long countByVerifiedFalse();
    long countByCreatedAtAfter(LocalDateTime createdAt);

    @Query("select u.role, count(u) from User u group by u.role")
    List<Object[]> countUsersGroupedByRole();
}
