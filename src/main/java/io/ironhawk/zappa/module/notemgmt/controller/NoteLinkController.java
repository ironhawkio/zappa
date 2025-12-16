package io.ironhawk.zappa.module.notemgmt.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLink;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLinkType;
import io.ironhawk.zappa.module.notemgmt.service.NoteLinkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/note-links")
@RequiredArgsConstructor
public class
NoteLinkController {

    private final NoteLinkService noteLinkService;

    @PostMapping
    public ResponseEntity<NoteLink> createLink(
        @RequestParam UUID sourceNoteId,
        @RequestParam UUID targetNoteId,
        @RequestParam NoteLinkType linkType,
        @RequestParam(defaultValue = "1") Integer weight) {

        try {
            NoteLink link = noteLinkService.createLink(sourceNoteId, targetNoteId, linkType, weight);
            return ResponseEntity.status(HttpStatus.CREATED).body(link);
        } catch (Exception e) {
            log.error("Error creating note link", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteLink> getLink(@PathVariable UUID id) {
        return noteLinkService.getLinkById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable UUID id) {
        try {
            noteLinkService.deleteLink(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/note/{noteId}")
    public ResponseEntity<List<NoteLink>> getLinksForNote(@PathVariable UUID noteId) {
        List<NoteLink> links = noteLinkService.findLinksForNote(noteId);
        return ResponseEntity.ok(links);
    }

    @GetMapping("/note/{noteId}/outgoing")
    public ResponseEntity<List<NoteLink>> getOutgoingLinks(@PathVariable UUID noteId) {
        List<NoteLink> links = noteLinkService.findOutgoingLinks(noteId);
        return ResponseEntity.ok(links);
    }

    @GetMapping("/note/{noteId}/incoming")
    public ResponseEntity<List<NoteLink>> getIncomingLinks(@PathVariable UUID noteId) {
        List<NoteLink> links = noteLinkService.findIncomingLinks(noteId);
        return ResponseEntity.ok(links);
    }

    @GetMapping("/graph/connected")
    public ResponseEntity<List<UUID>> getConnectedNotes(
        @RequestParam UUID startNoteId,
        @RequestParam(defaultValue = "3") Integer maxDepth) {

        List<UUID> connectedNotes = noteLinkService.findConnectedNotes(startNoteId, maxDepth);
        return ResponseEntity.ok(connectedNotes);
    }

    @GetMapping("/graph/path")
    public ResponseEntity<List<UUID>> getShortestPath(
        @RequestParam UUID startNoteId,
        @RequestParam UUID targetNoteId) {

        return noteLinkService.findShortestPath(startNoteId, targetNoteId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/graph/hubs")
    public ResponseEntity<List<UUID>> getMostConnectedNotes(
        @RequestParam(defaultValue = "10") Integer limit) {

        List<UUID> hubs = noteLinkService.findMostConnectedNotes(limit);
        return ResponseEntity.ok(hubs);
    }

    @GetMapping("/graph/orphans")
    public ResponseEntity<List<UUID>> getOrphanedNotes() {
        List<UUID> orphans = noteLinkService.findOrphanedNotes();
        return ResponseEntity.ok(orphans);
    }

    @GetMapping("/statistics")
    public ResponseEntity<List<Object[]>> getLinkStatistics() {
        List<Object[]> stats = noteLinkService.getLinkTypeStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/note/{noteId}/stats")
    public ResponseEntity<Object> getNoteGraphStats(@PathVariable UUID noteId) {
        Long linkCount = noteLinkService.countLinksForNote(noteId);
        Double avgWeight = noteLinkService.getAverageWeightForNote(noteId);

        return ResponseEntity.ok(new Object() {
            public final Long totalLinks = linkCount;
            public final Double averageWeight = avgWeight;
        });
    }

    @PostMapping("/bidirectional")
    public ResponseEntity<String> createBidirectionalLink(
        @RequestParam UUID noteId1,
        @RequestParam UUID noteId2,
        @RequestParam NoteLinkType linkType,
        @RequestParam(defaultValue = "1") Integer weight) {

        try {
            noteLinkService.createBidirectionalLink(noteId1, noteId2, linkType, weight);
            return ResponseEntity.ok("Bidirectional link created successfully");
        } catch (Exception e) {
            log.error("Error creating bidirectional link", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}