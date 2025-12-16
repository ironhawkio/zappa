package io.ironhawk.zappa.module.notemgmt.repository;

import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.entity.Note;
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

    // Find notes by title (case-insensitive)
    List<Note> findByTitleContainingIgnoreCase(String title);

    // Find notes by content containing keyword
    List<Note> findByContentContainingIgnoreCase(String keyword);

    // Find notes with pagination
    Page<Note> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Find notes by tag name
    @Query("SELECT DISTINCT n FROM Note n JOIN n.noteTags nt JOIN nt.tag t WHERE t.name = :tagName")
    List<Note> findByTagName(@Param("tagName") String tagName);

    // Find notes by multiple tag names
    @Query("SELECT DISTINCT n FROM Note n JOIN n.noteTags nt JOIN nt.tag t WHERE t.name IN :tagNames")
    List<Note> findByTagNames(@Param("tagNames") List<String> tagNames);

    // Find notes with eager loading of tags
    @Query("SELECT DISTINCT n FROM Note n LEFT JOIN FETCH n.noteTags nt LEFT JOIN FETCH nt.tag WHERE n.id = :id")
    Optional<Note> findByIdWithTags(@Param("id") UUID id);

    // Search notes by title and content
    @Query("SELECT n FROM Note n WHERE " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Note> searchNotes(@Param("searchTerm") String searchTerm);

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

    // Group-based filtering
    List<Note> findByGroupOrderByCreatedAtDesc(Group group);
    List<Note> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
    Page<Note> findByGroupOrderByCreatedAtDesc(Group group, Pageable pageable);
    Page<Note> findByGroupIdOrderByCreatedAtDesc(UUID groupId, Pageable pageable);

    // Find notes without a group (ungrouped)
    List<Note> findByGroupIsNullOrderByCreatedAtDesc();
    Page<Note> findByGroupIsNullOrderByCreatedAtDesc(Pageable pageable);

    // Group hierarchy filtering - find notes in group and all its subgroups
    @Query("SELECT DISTINCT n FROM Note n WHERE n.group.id = :groupId OR " +
           "n.group.id IN (SELECT sg.id FROM Group sg WHERE sg.parentGroup.id = :groupId)")
    List<Note> findByGroupAndSubGroups(@Param("groupId") UUID groupId);

    @Query("SELECT DISTINCT n FROM Note n WHERE n.group.id = :groupId OR " +
           "n.group.id IN (SELECT sg.id FROM Group sg WHERE sg.parentGroup.id = :groupId) " +
           "ORDER BY n.createdAt DESC")
    Page<Note> findByGroupAndSubGroups(@Param("groupId") UUID groupId, Pageable pageable);
}