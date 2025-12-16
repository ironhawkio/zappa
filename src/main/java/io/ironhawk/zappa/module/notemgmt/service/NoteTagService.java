package io.ironhawk.zappa.module.notemgmt.service;

import io.ironhawk.zappa.module.notemgmt.entity.NoteTag;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;

import java.util.List;
import java.util.UUID;

public interface NoteTagService {

    // Relationship management
    NoteTag createNoteTagRelationship(UUID noteId, UUID tagId);
    void deleteNoteTagRelationship(UUID noteId, UUID tagId);
    boolean relationshipExists(UUID noteId, UUID tagId);

    // Bulk operations
    List<NoteTag> addTagsToNote(UUID noteId, List<UUID> tagIds);
    void removeAllTagsFromNote(UUID noteId);
    void removeTagFromAllNotes(UUID tagId);

    // Query operations
    List<NoteTag> findRelationshipsByNoteId(UUID noteId);
    List<NoteTag> findRelationshipsByTagId(UUID tagId);
    List<UUID> findNoteIdsWithAllTags(List<UUID> tagIds);

    // Analytics
    long countNotesForTag(UUID tagId);
    long countTagsForNote(UUID noteId);
    List<Tag> findCoOccurringTags(UUID tagId);

    // Utility methods
    void cleanupOrphanedRelationships();
}