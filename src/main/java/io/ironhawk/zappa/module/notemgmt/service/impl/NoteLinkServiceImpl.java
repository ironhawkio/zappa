package io.ironhawk.zappa.module.notemgmt.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLink;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLinkType;
import io.ironhawk.zappa.module.notemgmt.repository.NoteLinkRepository;
import io.ironhawk.zappa.module.notemgmt.repository.NoteRepository;
import io.ironhawk.zappa.module.notemgmt.service.NoteLinkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteLinkServiceImpl implements NoteLinkService {

    private final NoteLinkRepository noteLinkRepository;
    private final NoteRepository noteRepository;

    @Override
    @Transactional
    public NoteLink createLink(NoteLink noteLink) {
        log.info("Creating note link from {} to {} with type {}",
            noteLink.getSourceNote().getId(),
            noteLink.getTargetNote().getId(),
            noteLink.getLinkType());

        return noteLinkRepository.save(noteLink);
    }

    @Override
    public Optional<NoteLink> getLinkById(UUID id) {
        log.debug("Fetching note link with id: {}", id);
        return noteLinkRepository.findById(id);
    }

    @Override
    @Transactional
    public NoteLink updateLink(NoteLink noteLink) {
        log.info("Updating note link with id: {}", noteLink.getId());
        if (!noteLinkRepository.existsById(noteLink.getId())) {
            throw new IllegalArgumentException("NoteLink not found with id: " + noteLink.getId());
        }
        return noteLinkRepository.save(noteLink);
    }

    @Override
    @Transactional
    public void deleteLink(UUID id) {
        log.info("Deleting note link with id: {}", id);
        if (!noteLinkRepository.existsById(id)) {
            throw new IllegalArgumentException("NoteLink not found with id: " + id);
        }
        noteLinkRepository.deleteById(id);
    }

    @Override
    public List<NoteLink> getAllLinks() {
        log.debug("Fetching all note links");
        return noteLinkRepository.findAll();
    }

    @Override
    @Transactional
    public NoteLink createLink(UUID sourceNoteId, UUID targetNoteId, NoteLinkType linkType) {
        return createLink(sourceNoteId, targetNoteId, linkType, 1);
    }

    @Override
    @Transactional
    public NoteLink createLink(UUID sourceNoteId, UUID targetNoteId, NoteLinkType linkType, Integer weight) {
        log.info("Creating link from note {} to note {} with type {} and weight {}",
            sourceNoteId, targetNoteId, linkType, weight);

        // Validate notes exist
        Note sourceNote = noteRepository.findById(sourceNoteId)
            .orElseThrow(() -> new IllegalArgumentException("Source note not found with id: " + sourceNoteId));

        Note targetNote = noteRepository.findById(targetNoteId)
            .orElseThrow(() -> new IllegalArgumentException("Target note not found with id: " + targetNoteId));

        // Check if link already exists
        if (noteLinkRepository.existsBySourceNoteIdAndTargetNoteIdAndLinkType(sourceNoteId, targetNoteId, linkType)) {
            throw new IllegalArgumentException("Link already exists between notes with type: " + linkType);
        }

        // Prevent self-referencing links
        if (sourceNoteId.equals(targetNoteId)) {
            throw new IllegalArgumentException("Cannot create self-referencing link");
        }

        NoteLink noteLink = NoteLink.builder()
            .sourceNote(sourceNote)
            .targetNote(targetNote)
            .linkType(linkType)
            .weight(weight)
            .build();

        return noteLinkRepository.save(noteLink);
    }

    @Override
    @Transactional
    public void deleteLink(UUID sourceNoteId, UUID targetNoteId, NoteLinkType linkType) {
        log.info("Deleting link from note {} to note {} with type {}",
            sourceNoteId, targetNoteId, linkType);

        NoteLink link = noteLinkRepository.findBySourceNoteIdAndTargetNoteIdAndLinkType(
            sourceNoteId, targetNoteId, linkType)
            .orElseThrow(() -> new IllegalArgumentException("Link not found"));

        noteLinkRepository.delete(link);
    }

    @Override
    public boolean linkExists(UUID sourceNoteId, UUID targetNoteId, NoteLinkType linkType) {
        return noteLinkRepository.existsBySourceNoteIdAndTargetNoteIdAndLinkType(
            sourceNoteId, targetNoteId, linkType);
    }

    @Override
    public List<NoteLink> findLinksForNote(UUID noteId) {
        log.debug("Finding all links for note: {}", noteId);
        return noteLinkRepository.findAllLinksForNote(noteId);
    }

    @Override
    public List<NoteLink> findOutgoingLinks(UUID noteId) {
        log.debug("Finding outgoing links for note: {}", noteId);
        return noteLinkRepository.findBySourceNoteId(noteId);
    }

    @Override
    public List<NoteLink> findIncomingLinks(UUID noteId) {
        log.debug("Finding incoming links for note: {}", noteId);
        return noteLinkRepository.findByTargetNoteId(noteId);
    }

    @Override
    public List<NoteLink> findLinksByType(NoteLinkType linkType) {
        log.debug("Finding links by type: {}", linkType);
        return noteLinkRepository.findByLinkType(linkType);
    }

    @Override
    public List<NoteLink> findLinksByWeight(Integer minWeight, Integer maxWeight) {
        log.debug("Finding links with weight between {} and {}", minWeight, maxWeight);
        return noteLinkRepository.findByWeightRange(minWeight, maxWeight);
    }

    @Override
    public List<UUID> findConnectedNotes(UUID startNoteId, Integer maxDepth) {
        log.debug("Finding connected notes from {} within {} degrees", startNoteId, maxDepth);
        return noteLinkRepository.findConnectedNotes(startNoteId, maxDepth);
    }

    @Override
    public Optional<List<UUID>> findShortestPath(UUID startNoteId, UUID targetNoteId) {
        log.debug("Finding shortest path from {} to {}", startNoteId, targetNoteId);
        return noteLinkRepository.findShortestPath(startNoteId, targetNoteId)
            .map(result -> (List<UUID>) result[0]);
    }

    @Override
    public List<UUID> findMostConnectedNotes(Integer limit) {
        log.debug("Finding {} most connected notes", limit);
        return noteLinkRepository.findMostConnectedNotes(limit).stream()
            .map(result -> (UUID) result[0])
            .toList();
    }

    @Override
    public List<UUID> findOrphanedNotes() {
        log.debug("Finding orphaned notes");
        return noteLinkRepository.findOrphanedNotes();
    }

    @Override
    public Long countLinksForNote(UUID noteId) {
        log.debug("Counting links for note: {}", noteId);
        return noteLinkRepository.countLinksForNote(noteId);
    }

    @Override
    public Double getAverageWeightForNote(UUID noteId) {
        log.debug("Calculating average weight for note: {}", noteId);
        return noteLinkRepository.getAverageWeightForNote(noteId);
    }

    @Override
    public List<Object[]> getLinkTypeStatistics() {
        log.debug("Getting link type statistics");
        return noteLinkRepository.getLinkTypeStatistics();
    }

    @Override
    @Transactional
    public void deleteAllLinksForNote(UUID noteId) {
        log.info("Deleting all links for note: {}", noteId);
        noteLinkRepository.deleteAllLinksForNote(noteId);
    }

    @Override
    @Transactional
    public void createBidirectionalLink(UUID noteId1, UUID noteId2, NoteLinkType linkType, Integer weight) {
        log.info("Creating bidirectional link between {} and {} with type {}",
            noteId1, noteId2, linkType);

        // Create primary link
        createLink(noteId1, noteId2, linkType, weight);

        // Create reverse link with inverse type if different
        NoteLinkType inverseType = linkType.getInverse();
        if (inverseType != linkType) {
            createLink(noteId2, noteId1, inverseType, weight);
        } else {
            // For symmetric relationships, create with bidirectional flag
            NoteLink link = noteLinkRepository.findBySourceNoteIdAndTargetNoteIdAndLinkType(
                noteId1, noteId2, linkType).orElse(null);
            if (link != null) {
                link.setIsBidirectional(true);
                noteLinkRepository.save(link);
            }
        }
    }

    @Override
    public List<NoteLink> findLinksInGroup(UUID groupId) {
        log.debug("Finding links within group: {}", groupId);
        return noteLinkRepository.findLinksWithinGroup(groupId);
    }

    @Override
    public List<NoteLink> findLinksInGroupIncludingSubGroups(UUID groupId) {
        log.debug("Finding links within group including subgroups: {}", groupId);
        return noteLinkRepository.findLinksWithinGroupIncludingSubGroups(groupId);
    }

    @Override
    public List<Note> findNotesInGroupGraph(UUID groupId) {
        log.debug("Finding notes in group graph: {}", groupId);
        return noteLinkRepository.findConnectedNotesInGroup(groupId);
    }

    @Override
    public List<Note> findNotesInGroupGraphIncludingSubGroups(UUID groupId) {
        log.debug("Finding notes in group graph including subgroups: {}", groupId);
        return noteLinkRepository.findConnectedNotesInGroupIncludingSubGroups(groupId);
    }
}