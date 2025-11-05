package org.example.userservice.repositories;

import org.example.userservice.entities.PasswordResetToken;
import org.example.userservice.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);

    void deleteAllByUser(User user);
}