package io.ironhawk.zappa.module.notemgmt.repository;

import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    // User-specific tag queries
    List<Tag> findByUserOrderByNameAsc(User user);
    Page<Tag> findByUserOrderByNameAsc(User user, Pageable pageable);
    Optional<Tag> findByIdAndUser(UUID id, User user);

    // Find tag by name (case-sensitive) for specific user
    Optional<Tag> findByUserAndName(User user, String name);

    // Find tag by name (case-insensitive) for specific user
    Optional<Tag> findByUserAndNameIgnoreCase(User user, String name);

    // Find tags by name pattern for specific user
    List<Tag> findByUserAndNameContainingIgnoreCase(User user, String namePattern);

    // Find tags by color for specific user
    List<Tag> findByUserAndColor(User user, String color);

    // Find tags with pagination for specific user
    Page<Tag> findByUserAndNameContainingIgnoreCase(User user, String namePattern, Pageable pageable);

    // Find tags used by a specific note
    @Query("SELECT DISTINCT t FROM Tag t JOIN t.noteTags nt WHERE nt.note.id = :noteId")
    List<Tag> findByNoteId(@Param("noteId") UUID noteId);

    // Find tags not used by any note for specific user
    @Query("SELECT t FROM Tag t WHERE t.user = :user AND t.noteTags IS EMPTY")
    List<Tag> findUnusedTagsByUser(@Param("user") User user);

    // Find most popular tags (by usage count) for specific user
    @Query("SELECT t, COUNT(nt) as usage FROM Tag t LEFT JOIN t.noteTags nt WHERE t.user = :user " +
           "GROUP BY t ORDER BY usage DESC")
    List<Object[]> findTagsWithUsageCountByUser(@Param("user") User user);

    // Find tags with usage count above threshold for specific user
    @Query("SELECT t FROM Tag t WHERE t.user = :user AND SIZE(t.noteTags) >= :minUsage")
    List<Tag> findTagsWithMinUsageByUser(@Param("user") User user, @Param("minUsage") int minUsage);

    // Check if tag name exists for specific user
    boolean existsByUserAndName(User user, String name);

    // Check if tag name exists (case-insensitive) for specific user
    boolean existsByUserAndNameIgnoreCase(User user, String name);
}