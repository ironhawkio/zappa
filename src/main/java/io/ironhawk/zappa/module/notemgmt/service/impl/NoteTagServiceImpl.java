package io.ironhawk.zappa.module.notemgmt.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteTag;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.repository.NoteRepository;
import io.ironhawk.zappa.module.notemgmt.repository.NoteTagRepository;
import io.ironhawk.zappa.module.notemgmt.repository.TagRepository;
import io.ironhawk.zappa.module.notemgmt.service.NoteTagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteTagServiceImpl implements NoteTagService {

    private final NoteTagRepository noteTagRepository;
    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public NoteTag createNoteTagRelationship(UUID noteId, UUID tagId) {
        log.info("Creating relationship between note {} and tag {}", noteId, tagId);

        // Validate that note and tag exist
        Note note = noteRepository.findById(noteId)
            .orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + noteId));

        Tag tag = tagRepository.findById(tagId)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found with id: " + tagId));

        // Check if relationship already exists
        if (noteTagRepository.existsByNoteIdAndTagId(noteId, tagId)) {
            throw new IllegalArgumentException("Relationship already exists between note " + noteId + " and tag " + tagId);
        }

        NoteTag noteTag = NoteTag.of(note, tag);
        return noteTagRepository.save(noteTag);
    }

    @Override
    @Transactional
    public void deleteNoteTagRelationship(UUID noteId, UUID tagId) {
        log.info("Deleting relationship between note {} and tag {}", noteId, tagId);

        if (!noteTagRepository.existsByNoteIdAndTagId(noteId, tagId)) {
            throw new IllegalArgumentException("Relationship does not exist between note " + noteId + " and tag " + tagId);
        }

        noteTagRepository.deleteByNoteIdAndTagId(noteId, tagId);
    }

    @Override
    public boolean relationshipExists(UUID noteId, UUID tagId) {
        log.debug("Checking if relationship exists between note {} and tag {}", noteId, tagId);
        return noteTagRepository.existsByNoteIdAndTagId(noteId, tagId);
    }

    @Override
    @Transactional
    public List<NoteTag> addTagsToNote(UUID noteId, List<UUID> tagIds) {
        log.info("Adding {} tags to note {}", tagIds.size(), noteId);

        // Validate that note exists
        Note note = noteRepository.findById(noteId)
            .orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + noteId));

        List<NoteTag> createdRelationships = new ArrayList<>();

        for (UUID tagId : tagIds) {
            // Validate that tag exists
            Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found with id: " + tagId));

            // Skip if relationship already exists
            if (noteTagRepository.existsByNoteIdAndTagId(noteId, tagId)) {
                log.warn("Relationship already exists between note {} and tag {}, skipping", noteId, tagId);
                continue;
            }

            NoteTag noteTag = NoteTag.of(note, tag);
            NoteTag savedNoteTag = noteTagRepository.save(noteTag);
            createdRelationships.add(savedNoteTag);
        }

        log.info("Created {} new relationships for note {}", createdRelationships.size(), noteId);
        return createdRelationships;
    }

    @Override
    @Transactional
    public void removeAllTagsFromNote(UUID noteId) {
        log.info("Removing all tags from note {}", noteId);

        if (!noteRepository.existsById(noteId)) {
            throw new IllegalArgumentException("Note not found with id: " + noteId);
        }

        long deletedCount = noteTagRepository.findByNoteId(noteId).size();
        noteTagRepository.deleteByNoteId(noteId);

        log.info("Removed {} tag relationships from note {}", deletedCount, noteId);
    }

    @Override
    @Transactional
    public void removeTagFromAllNotes(UUID tagId) {
        log.info("Removing tag {} from all notes", tagId);

        if (!tagRepository.existsById(tagId)) {
            throw new IllegalArgumentException("Tag not found with id: " + tagId);
        }

        long deletedCount = noteTagRepository.findByTagId(tagId).size();
        noteTagRepository.deleteByTagId(tagId);

        log.info("Removed tag {} from {} notes", tagId, deletedCount);
    }

    @Override
    public List<NoteTag> findRelationshipsByNoteId(UUID noteId) {
        log.debug("Finding relationships for note {}", noteId);
        return noteTagRepository.findByNoteId(noteId);
    }

    @Override
    public List<NoteTag> findRelationshipsByTagId(UUID tagId) {
        log.debug("Finding relationships for tag {}", tagId);
        return noteTagRepository.findByTagId(tagId);
    }

    @Override
    public List<UUID> findNoteIdsWithAllTags(List<UUID> tagIds) {
        log.debug("Finding notes with all tags: {}", tagIds);

        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }

        return noteTagRepository.findNoteIdsWithAllTags(tagIds, tagIds.size());
    }

    @Override
    public long countNotesForTag(UUID tagId) {
        log.debug("Counting notes for tag {}", tagId);
        return noteTagRepository.countByTagId(tagId);
    }

    @Override
    public long countTagsForNote(UUID noteId) {
        log.debug("Counting tags for note {}", noteId);
        return noteTagRepository.countByNoteId(noteId);
    }

    @Override
    public List<Tag> findCoOccurringTags(UUID tagId) {
        log.debug("Finding co-occurring tags for tag {}", tagId);
        return noteTagRepository.findCoOccurringTags(tagId);
    }

    @Override
    @Transactional
    public void cleanupOrphanedRelationships() {
        log.info("Cleaning up orphaned note-tag relationships");

        // This would typically involve complex queries to find orphaned relationships
        // For now, we'll implement a basic cleanup that relies on cascade operations
        List<NoteTag> allRelationships = noteTagRepository.findAll();
        int orphanedCount = 0;

        for (NoteTag relationship : allRelationships) {
            boolean noteExists = noteRepository.existsById(relationship.getNote().getId());
            boolean tagExists = tagRepository.existsById(relationship.getTag().getId());

            if (!noteExists || !tagExists) {
                noteTagRepository.delete(relationship);
                orphanedCount++;
            }
        }

        log.info("Cleaned up {} orphaned relationships", orphanedCount);
    }
}