package io.ironhawk.zappa.module.notemgmt.repository;

import io.ironhawk.zappa.module.notemgmt.entity.Tag;
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

    // Find tag by name (case-sensitive)
    Optional<Tag> findByName(String name);

    // Find tag by name (case-insensitive)
    Optional<Tag> findByNameIgnoreCase(String name);

    // Find tags by name pattern
    List<Tag> findByNameContainingIgnoreCase(String namePattern);

    // Find tags by color
    List<Tag> findByColor(String color);

    // Find tags with pagination
    Page<Tag> findByNameContainingIgnoreCase(String namePattern, Pageable pageable);

    // Find tags used by a specific note
    @Query("SELECT DISTINCT t FROM Tag t JOIN t.noteTags nt WHERE nt.note.id = :noteId")
    List<Tag> findByNoteId(@Param("noteId") UUID noteId);

    // Find tags not used by any note
    @Query("SELECT t FROM Tag t WHERE t.noteTags IS EMPTY")
    List<Tag> findUnusedTags();

    // Find most popular tags (by usage count)
    @Query("SELECT t, COUNT(nt) as usage FROM Tag t LEFT JOIN t.noteTags nt " +
           "GROUP BY t ORDER BY usage DESC")
    List<Object[]> findTagsWithUsageCount();

    // Find tags with usage count above threshold
    @Query("SELECT t FROM Tag t WHERE SIZE(t.noteTags) >= :minUsage")
    List<Tag> findTagsWithMinUsage(@Param("minUsage") int minUsage);

    // Check if tag name exists
    boolean existsByName(String name);

    // Check if tag name exists (case-insensitive)
    boolean existsByNameIgnoreCase(String name);
}