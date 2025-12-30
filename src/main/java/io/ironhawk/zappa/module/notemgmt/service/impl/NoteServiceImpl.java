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
import io.ironhawk.zappa.module.notemgmt.service.GroupService;
import io.ironhawk.zappa.security.entity.User;
import io.ironhawk.zappa.security.service.CurrentUserService;
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
    private final CurrentUserService currentUserService;
    private final GroupService groupService;

    @Override
    @Transactional
    public Note createNote(Note note) {
        User currentUser = currentUserService.getCurrentUser();
        note.setUser(currentUser);

        // If note has no group assigned, assign to default group
        if (note.getGroup() == null) {
            Group defaultGroup;
            if (!groupService.hasAnyGroups()) {
                // Create default group for new users
                defaultGroup = groupService.createDefaultGroup();
                log.info("Created default group '{}' for new user: {}", defaultGroup.getName(), currentUser.getUsername());
            } else {
                // Use existing default group or create one if none exists
                defaultGroup = groupService.findByName("Default")
                    .orElseGet(() -> {
                        Group newDefault = groupService.createDefaultGroup();
                        log.info("Created default group '{}' for user: {}", newDefault.getName(), currentUser.getUsername());
                        return newDefault;
                    });
            }
            note.setGroup(defaultGroup);
            log.debug("Assigned note to default group for user: {}", currentUser.getUsername());
        }

        log.info("Creating new note with title: {} for user: {}", note.getTitle(), currentUser.getUsername());
        return noteRepository.save(note);
    }

    @Override
    public Optional<Note> getNoteById(UUID id) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching note with id: {} for user: {}", id, currentUser.getUsername());
        return noteRepository.findByIdAndUser(id, currentUser);
    }

    @Override
    @Transactional
    public Note updateNote(Note note) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("Updating note with id: {} for user: {}", note.getId(), currentUser.getUsername());

        Optional<Note> existingNoteOpt = noteRepository.findByIdAndUser(note.getId(), currentUser);
        if (existingNoteOpt.isEmpty()) {
            throw new IllegalArgumentException("Note not found or access denied with id: " + note.getId());
        }

        Note existingNote = existingNoteOpt.get();

        // Preserve existing relationships that should not be modified by basic note update
        note.setUser(currentUser);
        note.setCreatedAt(existingNote.getCreatedAt()); // Preserve original creation time
        note.setAttachments(existingNote.getAttachments()); // Preserve existing attachments
        note.setOutgoingLinks(existingNote.getOutgoingLinks()); // Preserve existing links
        note.setIncomingLinks(existingNote.getIncomingLinks()); // Preserve existing links
        note.setNoteTags(existingNote.getNoteTags()); // Preserve existing tags (will be updated separately)

        return noteRepository.save(note);
    }

    @Override
    @Transactional
    public void deleteNote(UUID id) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("Deleting note with id: {} for user: {}", id, currentUser.getUsername());

        Optional<Note> note = noteRepository.findByIdAndUser(id, currentUser);
        if (note.isEmpty()) {
            throw new IllegalArgumentException("Note not found or access denied with id: " + id);
        }

        noteRepository.deleteById(id);
    }

    @Override
    public List<Note> getAllNotes() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching all notes for user: {}", currentUser.getUsername());
        return noteRepository.findByUserOrderByCreatedAtDesc(currentUser);
    }

    @Override
    public List<Note> searchNotes(String searchTerm) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Searching notes with term: {} for user: {}", searchTerm, currentUser.getUsername());
        return noteRepository.searchNotesByUser(currentUser, searchTerm);
    }

    @Override
    public List<Note> findNotesByTitle(String title) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding notes by title: {} for user: {}", title, currentUser.getUsername());
        return noteRepository.findByUserAndTitleContainingIgnoreCase(currentUser, title);
    }

    @Override
    public List<Note> findNotesByContent(String keyword) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding notes by content keyword: {} for user: {}", keyword, currentUser.getUsername());
        return noteRepository.findByUserAndContentContainingIgnoreCase(currentUser, keyword);
    }

    @Override
    public List<Note> findNotesByTagName(String tagName) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding notes by tag name: {} for user: {}", tagName, currentUser.getUsername());
        return noteRepository.findByUserAndTagName(currentUser, tagName);
    }

    @Override
    public List<Note> findNotesByTagNames(List<String> tagNames) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding notes by tag names: {} for user: {}", tagNames, currentUser.getUsername());
        return noteRepository.findByUserAndTagNames(currentUser, tagNames);
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

        return noteRepository.findByIdAndUserWithTags(noteId, currentUserService.getCurrentUser()).orElse(note);
    }

    @Override
    @Transactional
    public Note removeTagFromNote(UUID noteId, UUID tagId) {
        log.info("Removing tag {} from note {}", tagId, noteId);

        if (!noteRepository.existsById(noteId)) {
            throw new IllegalArgumentException("Note not found with id: " + noteId);
        }

        noteTagRepository.deleteByNoteIdAndTagId(noteId, tagId);

        return noteRepository.findByIdAndUserWithTags(noteId, currentUserService.getCurrentUser())
            .orElseThrow(() -> new IllegalArgumentException("Note not found with id: " + noteId));
    }

    @Override
    public Page<Note> getNotes(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching notes with pagination: {} for user: {}", pageable, currentUser.getUsername());
        return noteRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);
    }

    @Override
    public Page<Note> searchNotes(String searchTerm, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Searching notes with term: {} and pagination: {} for user: {}", searchTerm, pageable, currentUser.getUsername());
        return noteRepository.searchNotesByUser(currentUser, searchTerm, pageable);
    }

    @Override
    public Optional<Note> getNoteWithTags(UUID id) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching note with tags for id: {} for user: {}", id, currentUser.getUsername());
        return noteRepository.findByIdAndUserWithTags(id, currentUser);
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
        User currentUser = currentUserService.getCurrentUser();
        return noteRepository.findByUserAndTagNames(currentUser, tagNames);
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
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding notes by group: {} for user: {}", groupId, currentUser.getUsername());
        return noteRepository.findByUserAndGroupIdOrderByCreatedAtDesc(currentUser, groupId);
    }

    @Override
    public List<Note> findNotesByGroupIncludingSubGroups(UUID groupId) {
        log.debug("Finding notes by group including subgroups: {}", groupId);
        return noteRepository.findByGroupAndSubGroups(groupId);
    }

    @Override
    public List<Note> findUngroupedNotes() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding ungrouped notes for user: {}", currentUser.getUsername());
        return noteRepository.findByUserAndGroupIsNullOrderByCreatedAtDesc(currentUser);
    }

    @Override
    public Page<Note> findNotesByGroup(UUID groupId, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding notes by group with pagination: {} - {} for user: {}", groupId, pageable, currentUser.getUsername());
        return noteRepository.findByUserAndGroupIdOrderByCreatedAtDesc(currentUser, groupId, pageable);
    }

    @Override
    public Page<Note> findNotesByGroupIncludingSubGroups(UUID groupId, Pageable pageable) {
        log.debug("Finding notes by group including subgroups with pagination: {} - {}", groupId, pageable);
        return noteRepository.findByGroupAndSubGroups(groupId, pageable);
    }

    @Override
    public Page<Note> findUngroupedNotes(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding ungrouped notes with pagination: {} for user: {}", pageable, currentUser.getUsername());
        return noteRepository.findByUserAndGroupIsNullOrderByCreatedAtDesc(currentUser, pageable);
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