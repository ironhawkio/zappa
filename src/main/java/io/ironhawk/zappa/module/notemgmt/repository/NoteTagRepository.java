package io.ironhawk.zappa.module.notemgmt.repository;

import io.ironhawk.zappa.module.notemgmt.entity.NoteTag;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteTagRepository extends JpaRepository<NoteTag, NoteTag.NoteTagId> {

    // Find all note-tag relationships for a note
    List<NoteTag> findByNoteId(UUID noteId);

    // Find all note-tag relationships for a tag
    List<NoteTag> findByTagId(UUID tagId);

    // Find specific note-tag relationship
    Optional<NoteTag> findByNoteIdAndTagId(UUID noteId, UUID tagId);

    // Check if note-tag relationship exists
    boolean existsByNoteIdAndTagId(UUID noteId, UUID tagId);

    // Delete by note ID (when note is deleted)
    @Modifying
    @Query("DELETE FROM NoteTag nt WHERE nt.note.id = :noteId")
    void deleteByNoteId(@Param("noteId") UUID noteId);

    // Delete by tag ID (when tag is deleted)
    @Modifying
    @Query("DELETE FROM NoteTag nt WHERE nt.tag.id = :tagId")
    void deleteByTagId(@Param("tagId") UUID tagId);

    // Delete specific note-tag relationship
    @Modifying
    @Query("DELETE FROM NoteTag nt WHERE nt.note.id = :noteId AND nt.tag.id = :tagId")
    void deleteByNoteIdAndTagId(@Param("noteId") UUID noteId, @Param("tagId") UUID tagId);

    // Count notes for a tag
    long countByTagId(UUID tagId);

    // Count tags for a note
    long countByNoteId(UUID noteId);

    // Find notes that have all specified tags
    @Query("SELECT nt.note.id FROM NoteTag nt WHERE nt.tag.id IN :tagIds " +
           "GROUP BY nt.note.id HAVING COUNT(DISTINCT nt.tag.id) = :tagCount")
    List<UUID> findNoteIdsWithAllTags(@Param("tagIds") List<UUID> tagIds, @Param("tagCount") long tagCount);

    // Find tags that are used with a specific tag (co-occurrence)
    @Query("SELECT DISTINCT nt2.tag FROM NoteTag nt1 JOIN NoteTag nt2 ON nt1.note.id = nt2.note.id " +
           "WHERE nt1.tag.id = :tagId AND nt2.tag.id != :tagId")
    List<Tag> findCoOccurringTags(@Param("tagId") UUID tagId);
}