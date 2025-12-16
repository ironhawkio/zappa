package io.ironhawk.zappa.module.notemgmt.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteTag;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.repository.GroupRepository;
import io.ironhawk.zappa.module.notemgmt.repository.NoteRepository;
import io.ironhawk.zappa.module.notemgmt.repository.NoteTagRepository;
import io.ironhawk.zappa.module.notemgmt.repository.TagRepository;
import io.ironhawk.zappa.module.notemgmt.service.NoteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final NoteTagRepository noteTagRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public Note createNote(Note note) {
        log.info("Creating new note with title: {}", note.getTitle());
        return noteRepository.save(note);
    }

    @Override
    public Optional<Note> getNoteById(UUID id) {
        log.debug("Fetching note with id: {}", id);
        return noteRepository.findById(id);
    }

    @Override
    @Transactional
    public Note updateNote(Note note) {
        log.info("Updating note with id: {}", note.getId());
        if (!noteRepository.existsById(note.getId())) {
            throw new IllegalArgumentException("Note not found with id: " + note.getId());
        }
        return noteRepository.save(note);
    }

    @Override
    @Transactional
    public void deleteNote(UUID id) {
        log.info("Deleting note with id: {}", id);
        if (!noteRepository.existsById(id)) {
            throw new IllegalArgumentException("Note not found with id: " + id);
        }
        noteRepository.deleteById(id);
    }

    @Override
    public List<Note> getAllNotes() {
        log.debug("Fetching all notes");
        return noteRepository.findAll();
    }

    @Override
    public List<Note> searchNotes(String searchTerm) {
        log.debug("Searching notes with term: {}", searchTerm);
        return noteRepository.searchNotes(searchTerm);
    }

    @Override
    public List<Note> findNotesByTitle(String title) {
        log.debug("Finding notes by title: {}", title);
        return noteRepository.findByTitleContainingIgnoreCase(title);
    }


    @Override
    public List<Note> findNotesByContent(String keyword) {
        log.debug("Finding notes by content keyword: {}", keyword);
        return noteRepository.findByContentContainingIgnoreCase(keyword);
    }

    @Override
    public List<Note> findNotesByTagName(String tagName) {
        log.debug("Finding notes by tag name: {}", tagName);
        return noteRepository.findByTagName(tagName);
    }

    @Override
    public List<Note> findNotesByTagNames(List<String> tagNames) {
        log.debug("Finding notes by tag names: {}", tagNames);
        return noteRepository.findByTagNames(tagNames);
    }

    @Override
    @Transactional
    public Note addTagToNote(UUID noteId, UUID tagId) {
        log.info("Adding tag {} to note {}", tagId, noteId);

        Note note = noteRepository.findById(noteId)
            .orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + noteId));

        Tag tag = tagRepository.findById(tagId)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found with id: " + tagId));

        // Check if relationship already exists
        if (noteTagRepository.existsByNoteIdAndTagId(noteId, tagId)) {
            log.warn("Note-tag relationship already exists for note {} and tag {}", noteId, tagId);
            return note;
        }

        NoteTag noteTag = NoteTag.of(note, tag);
        noteTagRepository.save(noteTag);

        return noteRepository.findByIdWithTags(noteId).orElse(note);
    }

    @Override
    @Transactional
    public Note removeTagFromNote(UUID noteId, UUID tagId) {
        log.info("Removing tag {} from note {}", tagId, noteId);

        if (!noteRepository.existsById(noteId)) {
            throw new IllegalArgumentException("Note not found with id: " + noteId);
        }

        noteTagRepository.deleteByNoteIdAndTagId(noteId, tagId);

        return noteRepository.findByIdWithTags(noteId)
            .orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + noteId));
    }

    @Override
    public Page<Note> getNotes(Pageable pageable) {
        log.debug("Fetching notes with pagination: {}", pageable);
        return noteRepository.findAll(pageable);
    }

    @Override
    public Page<Note> searchNotes(String searchTerm, Pageable pageable) {
        log.debug("Searching notes with term: {} and pagination: {}", searchTerm, pageable);
        return noteRepository.findByTitleContainingIgnoreCase(searchTerm, pageable);
    }


    @Override
    public Optional<Note> getNoteWithTags(UUID id) {
        log.debug("Fetching note with tags for id: {}", id);
        return noteRepository.findByIdWithTags(id);
    }

    @Override
    public List<Note> findNotesByAllTags(List<String> tagNames) {
        log.debug("Finding notes by ALL tags: {}", tagNames);
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }
        return noteRepository.findByAllTags(tagNames, tagNames.size());
    }

    @Override
    public List<Note> findNotesByAnyTags(List<String> tagNames) {
        log.debug("Finding notes by ANY tags: {}", tagNames);
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }
        return noteRepository.findByTagNames(tagNames);
    }

    @Override
    public Page<Note> findNotesByAllTags(List<String> tagNames, Pageable pageable) {
        log.debug("Finding notes by ALL tags with pagination: {} - {}", tagNames, pageable);
        if (tagNames == null || tagNames.isEmpty()) {
            return Page.empty(pageable);
        }
        return noteRepository.findByAllTags(tagNames, tagNames.size(), pageable);
    }

    @Override
    public Page<Note> findNotesByAnyTags(List<String> tagNames, Pageable pageable) {
        log.debug("Finding notes by ANY tags with pagination: {} - {}", tagNames, pageable);
        if (tagNames == null || tagNames.isEmpty()) {
            return Page.empty(pageable);
        }
        return noteRepository.findByAnyTags(tagNames, pageable);
    }

    @Override
    public List<Note> findNotesByGroup(UUID groupId) {
        log.debug("Finding notes by group: {}", groupId);
        return noteRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    @Override
    public List<Note> findNotesByGroupIncludingSubGroups(UUID groupId) {
        log.debug("Finding notes by group including subgroups: {}", groupId);
        return noteRepository.findByGroupAndSubGroups(groupId);
    }

    @Override
    public List<Note> findUngroupedNotes() {
        log.debug("Finding ungrouped notes");
        return noteRepository.findByGroupIsNullOrderByCreatedAtDesc();
    }

    @Override
    public Page<Note> findNotesByGroup(UUID groupId, Pageable pageable) {
        log.debug("Finding notes by group with pagination: {} - {}", groupId, pageable);
        return noteRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
    }

    @Override
    public Page<Note> findNotesByGroupIncludingSubGroups(UUID groupId, Pageable pageable) {
        log.debug("Finding notes by group including subgroups with pagination: {} - {}", groupId, pageable);
        return noteRepository.findByGroupAndSubGroups(groupId, pageable);
    }

    @Override
    public Page<Note> findUngroupedNotes(Pageable pageable) {
        log.debug("Finding ungrouped notes with pagination: {}", pageable);
        return noteRepository.findByGroupIsNullOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional
    public Note assignNoteToGroup(UUID noteId, UUID groupId) {
        log.info("Assigning note {} to group {}", noteId, groupId);

        Note note = noteRepository.findById(noteId)
            .orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + noteId));

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        note.setGroup(group);
        return noteRepository.save(note);
    }

    @Override
    @Transactional
    public Note removeNoteFromGroup(UUID noteId) {
        log.info("Removing note {} from its group", noteId);

        Note note = noteRepository.findById(noteId)
            .orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + noteId));

        note.setGroup(null);
        return noteRepository.save(note);
    }
}