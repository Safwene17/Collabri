package org.example.userservice.repository;

import org.example.userservice.entities.User;
import org.example.userservice.enums.Role;
import org.example.userservice.integration.AbstractIntegrationTest;
import org.example.userservice.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRepository Tests")
class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String email, Role role, boolean verified) {
        return User.builder()
                .firstname("Test")
                .lastname("User")
                .email(email)
                .password("encoded-password")
                .role(role)
                .verified(verified)
                .build();
    }

    private User buildUser(String email) {
        return buildUser(email, Role.USER, false);
    }

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }


    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("should return user when email exists")
        void shouldReturnUserWhenEmailExists() {
            userRepository.save(buildUser("john@example.com", Role.USER, true));

            assertThat(userRepository.findByEmail("john@example.com"))
                    .isPresent()
                    .get()
                    .extracting(User::getEmail, User::getRole)
                    .containsExactly("john@example.com", Role.USER);
        }

        @Test
        @DisplayName("should return empty when email does not exist")
        void shouldReturnEmptyWhenEmailNotFound() {
            assertThat(userRepository.findByEmail("ghost@example.com")).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("should return true for existing email")
        void shouldReturnTrueForExistingEmail() {
            userRepository.save(buildUser("exists@example.com"));

            assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existing email")
        void shouldReturnFalseForNonExistingEmail() {
            assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("Unique email constraint")
    class UniqueEmailConstraint {

        @Test
        @DisplayName("should throw DataIntegrityViolationException on duplicate email")
        void shouldEnforceUniqueEmailConstraint() {
            userRepository.save(buildUser("john@example.com"));

            User duplicate = buildUser("john@example.com");

            assertThatThrownBy(() -> {
                userRepository.save(duplicate);
                userRepository.flush(); // must flush to trigger the DB constraint check
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }


    @Nested
    @DisplayName("countByVerifiedTrue and countByVerifiedFalse")
    class CountByVerified {

        @Test
        @DisplayName("should count only verified users")
        void shouldCountOnlyVerifiedUsers() {
            userRepository.save(buildUser("v1@example.com", Role.USER, true));
            userRepository.save(buildUser("v2@example.com", Role.ADMIN, true));
            userRepository.save(buildUser("u1@example.com", Role.USER, false));

            assertThat(userRepository.countByVerifiedTrue()).isEqualTo(2);
        }

        @Test
        @DisplayName("should count only unverified users")
        void shouldCountOnlyUnverifiedUsers() {
            userRepository.save(buildUser("v@example.com", Role.USER, true));
            userRepository.save(buildUser("u1@example.com", Role.USER, false));
            userRepository.save(buildUser("u2@example.com", Role.USER, false));

            assertThat(userRepository.countByVerifiedFalse()).isEqualTo(2);
        }

        @Test
        @DisplayName("verified and unverified counts should always sum to total")
        void verifiedAndUnverifiedShouldSumToTotal() {
            userRepository.save(buildUser("v@example.com", Role.USER, true));
            userRepository.save(buildUser("u1@example.com", Role.USER, false));
            userRepository.save(buildUser("u2@example.com", Role.USER, false));

            assertThat(userRepository.countByVerifiedTrue() + userRepository.countByVerifiedFalse())
                    .isEqualTo(userRepository.count());
        }
    }

    @Nested
    @DisplayName("countByCreatedAtAfter")
    class CountByCreatedAtAfter {

        @Test
        @DisplayName("should count users created after the given timestamp")
        void shouldCountUsersCreatedAfterTimestamp() {
            LocalDateTime boundary = LocalDateTime.now().minusDays(1);
            userRepository.save(buildUser("r1@example.com"));
            userRepository.save(buildUser("r2@example.com"));

            assertThat(userRepository.countByCreatedAtAfter(boundary)).isEqualTo(2);
        }

        @Test
        @DisplayName("should return 0 when all users were created before the timestamp")
        void shouldReturnZeroWhenAllUsersOlderThanTimestamp() {
            userRepository.save(buildUser("old@example.com"));

            assertThat(userRepository.countByCreatedAtAfter(LocalDateTime.now().plusDays(1)))
                    .isZero();
        }

        @Test
        @DisplayName("should NOT count users created exactly at the boundary — strictly after")
        void shouldNotCountUsersAtExactBoundary() {
            User saved = userRepository.saveAndFlush(buildUser("exact@example.com"));

            // "After" is exclusive — a user saved at exactly this instant must not be counted
            assertThat(userRepository.countByCreatedAtAfter(saved.getCreatedAt())).isZero();
        }
    }

    @Nested
    @DisplayName("countUsersGroupedByRole")
    class CountUsersGroupedByRole {

        @Test
        @DisplayName("should return empty list when no users exist")
        void shouldReturnEmptyWhenNoUsers() {
            assertThat(userRepository.countUsersGroupedByRole()).isEmpty();
        }

        @Test
        @DisplayName("should return one entry per distinct role")
        void shouldReturnOneEntryPerRole() {
            userRepository.save(buildUser("admin@example.com", Role.ADMIN, true));
            userRepository.save(buildUser("u1@example.com", Role.USER, false));
            userRepository.save(buildUser("u2@example.com", Role.USER, false));

            assertThat(userRepository.countUsersGroupedByRole()).hasSize(2);
        }

        @Test
        @DisplayName("row[0]=Role and row[1]=count — documents and verifies Object[] contract")
        void shouldReturnCorrectCountsWithCorrectStructure() {
            userRepository.save(buildUser("admin@example.com", Role.ADMIN, true));
            userRepository.save(buildUser("u1@example.com", Role.USER, false));
            userRepository.save(buildUser("u2@example.com", Role.USER, false));

            Map<Role, Long> countByRole = userRepository.countUsersGroupedByRole()
                    .stream()
                    .collect(Collectors.toMap(
                            row -> (Role) row[0],
                            row -> (Long) row[1]
                    ));

            assertThat(countByRole)
                    .containsEntry(Role.USER, 2L)
                    .containsEntry(Role.ADMIN, 1L);
        }

        @Test
        @DisplayName("should reflect single role when all users share the same role")
        void shouldReflectSingleRoleWhenAllUsersShareIt() {
            userRepository.save(buildUser("u1@example.com", Role.USER, false));
            userRepository.save(buildUser("u2@example.com", Role.USER, false));
            userRepository.save(buildUser("u3@example.com", Role.USER, false));

            List<Object[]> results = userRepository.countUsersGroupedByRole();

            assertThat(results).hasSize(1);
            assertThat((Role) results.get(0)[0]).isEqualTo(Role.USER);
            assertThat((Long) results.get(0)[1]).isEqualTo(3L);
        }
    }
}