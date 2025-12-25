package io.ironhawk.zappa.module.notemgmt.repository;

import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.entity.Note;
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
public interface NoteRepository extends JpaRepository<Note, UUID> {

    // User-specific note queries
    List<Note> findByUserOrderByCreatedAtDesc(User user);
    Page<Note> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Optional<Note> findByIdAndUser(UUID id, User user);

    // Find notes by title (case-insensitive) for specific user
    List<Note> findByUserAndTitleContainingIgnoreCase(User user, String title);

    // Find notes by content containing keyword for specific user
    List<Note> findByUserAndContentContainingIgnoreCase(User user, String keyword);

    // Find notes with pagination for specific user
    Page<Note> findByUserAndTitleContainingIgnoreCase(User user, String title, Pageable pageable);

    // Find notes by tag name for specific user
    @Query("SELECT DISTINCT n FROM Note n JOIN n.noteTags nt JOIN nt.tag t WHERE n.user = :user AND t.name = :tagName")
    List<Note> findByUserAndTagName(@Param("user") User user, @Param("tagName") String tagName);

    // Find notes by multiple tag names for specific user
    @Query("SELECT DISTINCT n FROM Note n JOIN n.noteTags nt JOIN nt.tag t WHERE n.user = :user AND t.name IN :tagNames")
    List<Note> findByUserAndTagNames(@Param("user") User user, @Param("tagNames") List<String> tagNames);

    // Find notes with eager loading of tags for specific user
    @Query("SELECT DISTINCT n FROM Note n LEFT JOIN FETCH n.noteTags nt LEFT JOIN FETCH nt.tag WHERE n.user = :user AND n.id = :id")
    Optional<Note> findByIdAndUserWithTags(@Param("id") UUID id, @Param("user") User user);

    // Search notes by title and content for specific user
    @Query("SELECT n FROM Note n WHERE n.user = :user AND (" +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Note> searchNotesByUser(@Param("user") User user, @Param("searchTerm") String searchTerm);

    @Query("SELECT n FROM Note n WHERE n.user = :user AND (" +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Note> searchNotesByUser(@Param("user") User user, @Param("searchTerm") String searchTerm, Pageable pageable);

    // Advanced tag filtering - AND operation (notes that have ALL specified tags)
    @Query("SELECT DISTINCT n FROM Note n WHERE " +
           "(SELECT COUNT(DISTINCT t.name) FROM Note n2 JOIN n2.noteTags nt JOIN nt.tag t " +
           "WHERE n2.id = n.id AND t.name IN :tagNames) = :tagCount")
    List<Note> findByAllTags(@Param("tagNames") List<String> tagNames, @Param("tagCount") long tagCount);

    // Advanced tag filtering with pagination - AND operation
    @Query("SELECT DISTINCT n FROM Note n WHERE " +
           "(SELECT COUNT(DISTINCT t.name) FROM Note n2 JOIN n2.noteTags nt JOIN nt.tag t " +
           "WHERE n2.id = n.id AND t.name IN :tagNames) = :tagCount")
    Page<Note> findByAllTags(@Param("tagNames") List<String> tagNames, @Param("tagCount") long tagCount, Pageable pageable);

    // Advanced tag filtering with pagination - OR operation (existing method enhanced)
    @Query("SELECT DISTINCT n FROM Note n JOIN n.noteTags nt JOIN nt.tag t WHERE t.name IN :tagNames")
    Page<Note> findByAnyTags(@Param("tagNames") List<String> tagNames, Pageable pageable);

    // Group-based filtering for specific user
    List<Note> findByUserAndGroupOrderByCreatedAtDesc(User user, Group group);
    List<Note> findByUserAndGroupIdOrderByCreatedAtDesc(User user, UUID groupId);
    Page<Note> findByUserAndGroupOrderByCreatedAtDesc(User user, Group group, Pageable pageable);
    Page<Note> findByUserAndGroupIdOrderByCreatedAtDesc(User user, UUID groupId, Pageable pageable);

    // Find notes without a group (ungrouped) for specific user
    List<Note> findByUserAndGroupIsNullOrderByCreatedAtDesc(User user);
    Page<Note> findByUserAndGroupIsNullOrderByCreatedAtDesc(User user, Pageable pageable);

    // Group hierarchy filtering - find notes in group and all its subgroups
    @Query("SELECT DISTINCT n FROM Note n WHERE n.group.id = :groupId OR " +
           "n.group.id IN (SELECT sg.id FROM Group sg WHERE sg.parentGroup.id = :groupId)")
    List<Note> findByGroupAndSubGroups(@Param("groupId") UUID groupId);

    @Query("SELECT DISTINCT n FROM Note n WHERE n.group.id = :groupId OR " +
           "n.group.id IN (SELECT sg.id FROM Group sg WHERE sg.parentGroup.id = :groupId) " +
           "ORDER BY n.createdAt DESC")
    Page<Note> findByGroupAndSubGroups(@Param("groupId") UUID groupId, Pageable pageable);
}