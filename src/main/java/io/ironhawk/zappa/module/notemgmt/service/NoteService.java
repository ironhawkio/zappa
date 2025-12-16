package io.ironhawk.zappa.module.notemgmt.service;

import io.ironhawk.zappa.module.notemgmt.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteService {

    // Basic CRUD operations
    Note createNote(Note note);
    Optional<Note> getNoteById(UUID id);
    Note updateNote(Note note);
    void deleteNote(UUID id);
    List<Note> getAllNotes();

    // Search operations
    List<Note> searchNotes(String searchTerm);
    List<Note> findNotesByTitle(String title);
    List<Note> findNotesByContent(String keyword);

    // Tag-related operations
    List<Note> findNotesByTagName(String tagName);
    List<Note> findNotesByTagNames(List<String> tagNames);
    Note addTagToNote(UUID noteId, UUID tagId);
    Note removeTagFromNote(UUID noteId, UUID tagId);

    // Advanced tag filtering operations
    List<Note> findNotesByAllTags(List<String> tagNames);
    List<Note> findNotesByAnyTags(List<String> tagNames);
    Page<Note> findNotesByAllTags(List<String> tagNames, Pageable pageable);
    Page<Note> findNotesByAnyTags(List<String> tagNames, Pageable pageable);

    // Pagination
    Page<Note> getNotes(Pageable pageable);
    Page<Note> searchNotes(String searchTerm, Pageable pageable);

    // Group-related operations
    List<Note> findNotesByGroup(UUID groupId);
    List<Note> findNotesByGroupIncludingSubGroups(UUID groupId);
    List<Note> findUngroupedNotes();
    Page<Note> findNotesByGroup(UUID groupId, Pageable pageable);
    Page<Note> findNotesByGroupIncludingSubGroups(UUID groupId, Pageable pageable);
    Page<Note> findUngroupedNotes(Pageable pageable);
    Note assignNoteToGroup(UUID noteId, UUID groupId);
    Note removeNoteFromGroup(UUID noteId);

    // Analytics
    Optional<Note> getNoteWithTags(UUID id);
}