// file: src/main/java/org/example/userservice/repositories/RefreshTokenRepository.java
package org.example.userservice.repositories;

import org.example.userservice.entities.Admin;  // ADDED
import org.example.userservice.entities.RefreshToken;
import org.example.userservice.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;  // ADDED
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user = :user AND t.token <> :exclude")
    int revokeAllExcept(@Param("user") User user, @Param("exclude") String exclude);

    void deleteAllByUser(User user);

    // ADDED: Methods for Admin
    List<RefreshToken> findAllByAdmin(Admin admin);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.admin = :admin AND t.token <> :exclude")
    int revokeAllExcept(@Param("admin") Admin admin, @Param("exclude") String exclude);

    void deleteAllByAdmin(Admin admin);
}