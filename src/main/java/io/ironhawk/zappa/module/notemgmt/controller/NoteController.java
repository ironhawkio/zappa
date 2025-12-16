package io.ironhawk.zappa.module.notemgmt.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.dto.NoteCreateRequest;
import io.ironhawk.zappa.module.notemgmt.dto.NoteResponse;
import io.ironhawk.zappa.module.notemgmt.dto.NoteUpdateRequest;
import io.ironhawk.zappa.module.notemgmt.dto.TagResponse;
import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.service.NoteService;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Validated
public class NoteController {

    private final NoteService noteService;
    private final TagService tagService;

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(@Valid @RequestBody NoteCreateRequest request) {
        log.info("Creating note with title: {}", request.getTitle());

        Note note = Note.of(request.getTitle(), request.getContent());

        Note createdNote = noteService.createNote(note);

        // Add tags if provided
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            for (String tagName : request.getTagNames()) {
                Tag tag = tagService.getOrCreateTag(tagName, null);
                noteService.addTagToNote(createdNote.getId(), tag.getId());
            }
            // Refresh note with tags
            createdNote = noteService.getNoteWithTags(createdNote.getId()).orElse(createdNote);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toNoteResponse(createdNote));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNote(@PathVariable UUID id) {
        log.info("Fetching note with id: {}", id);

        return noteService.getNoteWithTags(id)
            .map(note -> ResponseEntity.ok(toNoteResponse(note)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<NoteResponse>> getAllNotes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Fetching notes with pagination: page={}, size={}, sortBy={}, direction={}",
                 page, size, sortBy, sortDirection);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Note> notes = noteService.getNotes(pageable);
        Page<NoteResponse> responsePages = notes.map(this::toNoteResponse);

        return ResponseEntity.ok(responsePages);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(@PathVariable UUID id, @Valid @RequestBody NoteUpdateRequest request) {
        log.info("Updating note with id: {}", id);

        if (!id.equals(request.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Note note = Note.forUpdate(request.getId(), request.getTitle(), request.getContent());

        try {
            Note updatedNote = noteService.updateNote(note);

            // Update tags if provided
            if (request.getTagNames() != null) {
                // Remove all existing tags
                List<Tag> existingTags = tagService.findTagsByNoteId(id);
                for (Tag tag : existingTags) {
                    noteService.removeTagFromNote(id, tag.getId());
                }

                // Add new tags
                for (String tagName : request.getTagNames()) {
                    Tag tag = tagService.getOrCreateTag(tagName, null);
                    noteService.addTagToNote(id, tag.getId());
                }

                // Refresh note with tags
                updatedNote = noteService.getNoteWithTags(id).orElse(updatedNote);
            }

            return ResponseEntity.ok(toNoteResponse(updatedNote));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable UUID id) {
        log.info("Deleting note with id: {}", id);

        try {
            noteService.deleteNote(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<NoteResponse>> searchNotes(@RequestParam String query) {
        log.info("Searching notes with query: {}", query);

        List<Note> notes = noteService.searchNotes(query);
        List<NoteResponse> responses = notes.stream()
            .map(this::toNoteResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-tag")
    public ResponseEntity<List<NoteResponse>> getNotesByTag(@RequestParam String tagName) {
        log.info("Fetching notes by tag: {}", tagName);

        List<Note> notes = noteService.findNotesByTagName(tagName);
        List<NoteResponse> responses = notes.stream()
            .map(this::toNoteResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-tags")
    public ResponseEntity<List<NoteResponse>> getNotesByTags(@RequestParam List<String> tagNames) {
        log.info("Fetching notes by tags: {}", tagNames);

        List<Note> notes = noteService.findNotesByTagNames(tagNames);
        List<NoteResponse> responses = notes.stream()
            .map(this::toNoteResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }


    @PostMapping("/{id}/tags/{tagId}")
    public ResponseEntity<NoteResponse> addTagToNote(@PathVariable UUID id, @PathVariable UUID tagId) {
        log.info("Adding tag {} to note {}", tagId, id);

        try {
            Note note = noteService.addTagToNote(id, tagId);
            return ResponseEntity.ok(toNoteResponse(note));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ResponseEntity<NoteResponse> removeTagFromNote(@PathVariable UUID id, @PathVariable UUID tagId) {
        log.info("Removing tag {} from note {}", tagId, id);

        try {
            Note note = noteService.removeTagFromNote(id, tagId);
            return ResponseEntity.ok(toNoteResponse(note));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }


    private NoteResponse toNoteResponse(Note note) {
        List<TagResponse> tagResponses = note.getNoteTags().stream()
            .map(noteTag -> TagResponse.builder()
                .id(noteTag.getTag().getId())
                .name(noteTag.getTag().getName())
                .color(noteTag.getTag().getColor())
                .createdAt(noteTag.getTag().getCreatedAt())
                .updatedAt(noteTag.getTag().getUpdatedAt())
                .build())
            .collect(Collectors.toList());

        return NoteResponse.builder()
            .id(note.getId())
            .title(note.getTitle())
            .content(note.getContent())
            .createdAt(note.getCreatedAt())
            .updatedAt(note.getUpdatedAt())
            .tags(tagResponses)
            .build();
    }
}