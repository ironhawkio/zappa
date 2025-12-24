package io.ironhawk.zappa.security.repository;

import io.ironhawk.zappa.security.entity.User;
import io.ironhawk.zappa.security.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

    Optional<UserPreferences> findByUser(User user);

    Optional<UserPreferences> findByUserId(UUID userId);

    boolean existsByUser(User user);

    boolean existsByUserId(UUID userId);

    @Modifying
    @Query("UPDATE UserPreferences up SET up.graphPositions = :positions WHERE up.user.id = :userId")
    void updateGraphPositions(@Param("userId") UUID userId, @Param("positions") String positions);

    @Query("SELECT up.graphPositions FROM UserPreferences up WHERE up.user.id = :userId")
    Optional<String> findGraphPositionsByUserId(@Param("userId") UUID userId);
}