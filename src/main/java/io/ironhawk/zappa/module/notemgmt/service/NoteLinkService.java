package io.ironhawk.zappa.module.notemgmt.service;

import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLink;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLinkType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteLinkService {

    // Basic CRUD operations
    NoteLink createLink(NoteLink noteLink);
    Optional<NoteLink> getLinkById(UUID id);
    NoteLink updateLink(NoteLink noteLink);
    void deleteLink(UUID id);
    List<NoteLink> getAllLinks();

    // Link management
    NoteLink createLink(UUID sourceNoteId, UUID targetNoteId, NoteLinkType linkType);
    NoteLink createLink(UUID sourceNoteId, UUID targetNoteId, NoteLinkType linkType, Integer weight);
    void deleteLink(UUID sourceNoteId, UUID targetNoteId, NoteLinkType linkType);
    boolean linkExists(UUID sourceNoteId, UUID targetNoteId, NoteLinkType linkType);

    // Query operations
    List<NoteLink> findLinksForNote(UUID noteId);
    List<NoteLink> findOutgoingLinks(UUID noteId);
    List<NoteLink> findIncomingLinks(UUID noteId);
    List<NoteLink> findLinksByType(NoteLinkType linkType);
    List<NoteLink> findLinksByWeight(Integer minWeight, Integer maxWeight);

    // Graph traversal
    List<UUID> findConnectedNotes(UUID startNoteId, Integer maxDepth);
    Optional<List<UUID>> findShortestPath(UUID startNoteId, UUID targetNoteId);
    List<UUID> findMostConnectedNotes(Integer limit);
    List<UUID> findOrphanedNotes();

    // Analytics
    Long countLinksForNote(UUID noteId);
    Double getAverageWeightForNote(UUID noteId);
    List<Object[]> getLinkTypeStatistics();

    // Group-specific operations
    List<NoteLink> findLinksInGroup(UUID groupId);
    List<NoteLink> findLinksInGroupIncludingSubGroups(UUID groupId);
    List<Note> findNotesInGroupGraph(UUID groupId);
    List<Note> findNotesInGroupGraphIncludingSubGroups(UUID groupId);

    // Utility methods
    void deleteAllLinksForNote(UUID noteId);
    void createBidirectionalLink(UUID noteId1, UUID noteId2, NoteLinkType linkType, Integer weight);
}