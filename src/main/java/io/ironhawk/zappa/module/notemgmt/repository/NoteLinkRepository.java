package io.ironhawk.zappa.module.notemgmt.repository;

import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLink;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteLinkRepository extends JpaRepository<NoteLink, UUID> {

    // Basic link queries
    List<NoteLink> findBySourceNoteId(UUID sourceNoteId);

    List<NoteLink> findByTargetNoteId(UUID targetNoteId);

    List<NoteLink> findByLinkType(NoteLinkType linkType);

    Optional<NoteLink> findBySourceNoteIdAndTargetNoteIdAndLinkType(
        UUID sourceNoteId, UUID targetNoteId, NoteLinkType linkType);

    // Bidirectional and weight-based queries
    List<NoteLink> findByIsBidirectionalTrue();

    List<NoteLink> findByWeightGreaterThanEqual(Integer minWeight);

    @Query("SELECT nl FROM NoteLink nl WHERE nl.weight BETWEEN :minWeight AND :maxWeight")
    List<NoteLink> findByWeightRange(@Param("minWeight") Integer minWeight, @Param("maxWeight") Integer maxWeight);

    // Find all links for a note (both incoming and outgoing)
    @Query("SELECT nl FROM NoteLink nl WHERE nl.sourceNote.id = :noteId OR nl.targetNote.id = :noteId")
    List<NoteLink> findAllLinksForNote(@Param("noteId") UUID noteId);

    // Find all outgoing links from a note
    @Query("SELECT nl FROM NoteLink nl WHERE nl.sourceNote.id = :noteId")
    List<NoteLink> findOutgoingLinks(@Param("noteId") UUID noteId);

    // Find all incoming links to a note
    @Query("SELECT nl FROM NoteLink nl WHERE nl.targetNote.id = :noteId")
    List<NoteLink> findIncomingLinks(@Param("noteId") UUID noteId);

    // Find links by type for a specific note
    @Query("SELECT nl FROM NoteLink nl WHERE (nl.sourceNote.id = :noteId OR nl.targetNote.id = :noteId) AND nl.linkType = :linkType")
    List<NoteLink> findLinksForNoteByType(@Param("noteId") UUID noteId, @Param("linkType") NoteLinkType linkType);

    // Graph traversal queries
    @Query(value = """
        WITH RECURSIVE note_graph(note_id, depth, path) AS (
            SELECT :startNoteId::uuid, 0, ARRAY[:startNoteId::uuid]
            UNION ALL
            SELECT CASE
                WHEN nl.source_note_id = ng.note_id THEN nl.target_note_id
                WHEN nl.target_note_id = ng.note_id AND nl.is_bidirectional = true THEN nl.source_note_id
                ELSE NULL
            END, ng.depth + 1, ng.path || CASE
                WHEN nl.source_note_id = ng.note_id THEN nl.target_note_id
                WHEN nl.target_note_id = ng.note_id AND nl.is_bidirectional = true THEN nl.source_note_id
            END
            FROM note_links nl
            JOIN note_graph ng ON (
                (nl.source_note_id = ng.note_id OR (nl.target_note_id = ng.note_id AND nl.is_bidirectional = true))
                AND NOT (CASE
                    WHEN nl.source_note_id = ng.note_id THEN nl.target_note_id
                    WHEN nl.target_note_id = ng.note_id AND nl.is_bidirectional = true THEN nl.source_note_id
                END = ANY(ng.path))
            )
            WHERE ng.depth < :maxDepth
        )
        SELECT DISTINCT note_id FROM note_graph WHERE depth > 0 AND depth <= :maxDepth
        """, nativeQuery = true)
    List<UUID> findConnectedNotes(@Param("startNoteId") UUID startNoteId, @Param("maxDepth") Integer maxDepth);

    // Find shortest path between two notes
    @Query(value = """
        WITH RECURSIVE shortest_path(note_id, target_id, depth, path) AS (
            SELECT :startNoteId::uuid, :targetNoteId::uuid, 0, ARRAY[:startNoteId::uuid]
            WHERE :startNoteId != :targetNoteId
            UNION ALL
            SELECT CASE
                WHEN nl.source_note_id = sp.note_id THEN nl.target_note_id
                WHEN nl.target_note_id = sp.note_id AND nl.is_bidirectional = true THEN nl.source_note_id
            END, sp.target_id, sp.depth + 1, sp.path || CASE
                WHEN nl.source_note_id = sp.note_id THEN nl.target_note_id
                WHEN nl.target_note_id = sp.note_id AND nl.is_bidirectional = true THEN nl.source_note_id
            END
            FROM note_links nl
            JOIN shortest_path sp ON (
                (nl.source_note_id = sp.note_id OR (nl.target_note_id = sp.note_id AND nl.is_bidirectional = true))
                AND NOT (CASE
                    WHEN nl.source_note_id = sp.note_id THEN nl.target_note_id
                    WHEN nl.target_note_id = sp.note_id AND nl.is_bidirectional = true THEN nl.source_note_id
                END = ANY(sp.path))
            )
            WHERE sp.depth < 10 AND sp.note_id != sp.target_id
        )
        SELECT path FROM shortest_path WHERE note_id = target_id ORDER BY depth LIMIT 1
        """, nativeQuery = true)
    Optional<Object[]> findShortestPath(@Param("startNoteId") UUID startNoteId, @Param("targetNoteId") UUID targetNoteId);

    // Analytics and statistics
    @Query("SELECT COUNT(nl) FROM NoteLink nl WHERE nl.sourceNote.id = :noteId OR nl.targetNote.id = :noteId")
    Long countLinksForNote(@Param("noteId") UUID noteId);

    @Query("SELECT nl.linkType, COUNT(nl) FROM NoteLink nl GROUP BY nl.linkType")
    List<Object[]> getLinkTypeStatistics();

    @Query("SELECT AVG(nl.weight) FROM NoteLink nl WHERE nl.sourceNote.id = :noteId OR nl.targetNote.id = :noteId")
    Double getAverageWeightForNote(@Param("noteId") UUID noteId);

    // Find most connected notes
    @Query(value = """
        SELECT note_id, link_count FROM (
            SELECT source_note_id as note_id, COUNT(*) as link_count FROM note_links GROUP BY source_note_id
            UNION ALL
            SELECT target_note_id as note_id, COUNT(*) as link_count FROM note_links GROUP BY target_note_id
        ) combined
        GROUP BY note_id
        ORDER BY SUM(link_count) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findMostConnectedNotes(@Param("limit") Integer limit);

    // Find orphaned notes (notes with no links)
    @Query(value = """
        SELECT n.id FROM notes n
        WHERE n.id NOT IN (
            SELECT DISTINCT source_note_id FROM note_links
            UNION
            SELECT DISTINCT target_note_id FROM note_links
        )
        """, nativeQuery = true)
    List<UUID> findOrphanedNotes();

    // Cleanup and maintenance
    @Modifying
    @Query("DELETE FROM NoteLink nl WHERE nl.sourceNote.id = :noteId OR nl.targetNote.id = :noteId")
    void deleteAllLinksForNote(@Param("noteId") UUID noteId);

    @Modifying
    @Query("DELETE FROM NoteLink nl WHERE nl.sourceNote.id = :noteId AND nl.targetNote.id = :targetNoteId")
    void deleteAllLinksBetweenNotes(@Param("noteId") UUID noteId, @Param("targetNoteId") UUID targetNoteId);

    // Check for existing links
    boolean existsBySourceNoteIdAndTargetNoteIdAndLinkType(UUID sourceId, UUID targetId, NoteLinkType linkType);

    boolean existsBySourceNoteIdAndTargetNoteId(UUID sourceId, UUID targetId);

    // Find similar links (same source and target with different types)
    @Query("SELECT nl FROM NoteLink nl WHERE nl.sourceNote.id = :sourceId AND nl.targetNote.id = :targetId AND nl.linkType != :excludeType")
    List<NoteLink> findSimilarLinks(@Param("sourceId") UUID sourceId, @Param("targetId") UUID targetId, @Param("excludeType") NoteLinkType excludeType);

    // Find notes with specific relationship patterns
    @Query("SELECT DISTINCT nl.targetNote.id FROM NoteLink nl WHERE nl.sourceNote.id = :noteId AND nl.linkType IN :linkTypes")
    List<UUID> findRelatedNotesByTypes(@Param("noteId") UUID noteId, @Param("linkTypes") List<NoteLinkType> linkTypes);

    // Group-specific graph queries
    @Query("SELECT DISTINCT nl FROM NoteLink nl WHERE " +
           "(nl.sourceNote.group.id = :groupId AND nl.targetNote.group.id = :groupId)")
    List<NoteLink> findLinksWithinGroup(@Param("groupId") UUID groupId);

    @Query("SELECT DISTINCT nl FROM NoteLink nl WHERE " +
           "((nl.sourceNote.group.id = :groupId OR nl.sourceNote.group.id IN " +
           "(SELECT sg.id FROM Group sg WHERE sg.parentGroup.id = :groupId)) AND " +
           "(nl.targetNote.group.id = :groupId OR nl.targetNote.group.id IN " +
           "(SELECT sg.id FROM Group sg WHERE sg.parentGroup.id = :groupId)))")
    List<NoteLink> findLinksWithinGroupIncludingSubGroups(@Param("groupId") UUID groupId);

    // Find notes that have links within a group
    @Query("SELECT DISTINCT n FROM Note n WHERE n.group.id = :groupId AND " +
           "(EXISTS (SELECT 1 FROM NoteLink nl WHERE nl.sourceNote = n AND nl.targetNote.group.id = :groupId) OR " +
           "EXISTS (SELECT 1 FROM NoteLink nl WHERE nl.targetNote = n AND nl.sourceNote.group.id = :groupId))")
    List<Note> findConnectedNotesInGroup(@Param("groupId") UUID groupId);

    @Query("SELECT DISTINCT n FROM Note n WHERE " +
           "(n.group.id = :groupId OR n.group.id IN (SELECT sg.id FROM Group sg WHERE sg.parentGroup.id = :groupId)) AND " +
           "(EXISTS (SELECT 1 FROM NoteLink nl WHERE nl.sourceNote = n AND " +
           "(nl.targetNote.group.id = :groupId OR nl.targetNote.group.id IN " +
           "(SELECT sg2.id FROM Group sg2 WHERE sg2.parentGroup.id = :groupId))) OR " +
           "EXISTS (SELECT 1 FROM NoteLink nl WHERE nl.targetNote = n AND " +
           "(nl.sourceNote.group.id = :groupId OR nl.sourceNote.group.id IN " +
           "(SELECT sg3.id FROM Group sg3 WHERE sg3.parentGroup.id = :groupId))))")
    List<Note> findConnectedNotesInGroupIncludingSubGroups(@Param("groupId") UUID groupId);
}