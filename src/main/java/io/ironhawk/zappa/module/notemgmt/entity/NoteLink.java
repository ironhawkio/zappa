package io.ironhawk.zappa.module.notemgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "note_links", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"source_note_id", "target_note_id", "link_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_note_id", nullable = false)
    private Note sourceNote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_note_id", nullable = false)
    private Note targetNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false)
    private NoteLinkType linkType;

    @Column(name = "weight")
    @Builder.Default
    private Integer weight = 1; // 1-10 scale

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "is_bidirectional")
    @Builder.Default
    private Boolean isBidirectional = false;

    @Column(name = "created_by")
    private String createdBy; // Future user support

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Custom constructor for basic linking
    public NoteLink(Note sourceNote, Note targetNote, NoteLinkType linkType) {
        this.sourceNote = sourceNote;
        this.targetNote = targetNote;
        this.linkType = linkType;
        this.weight = 1;
        this.isBidirectional = false;
    }

    // Static factory methods for common link types
    public static NoteLink relates(Note source, Note target) {
        return NoteLink.builder()
            .sourceNote(source)
            .targetNote(target)
            .linkType(NoteLinkType.RELATES_TO)
            .isBidirectional(true)
            .build();
    }

    public static NoteLink references(Note source, Note target) {
        return NoteLink.builder()
            .sourceNote(source)
            .targetNote(target)
            .linkType(NoteLinkType.REFERENCES)
            .build();
    }

    public static NoteLink followsFrom(Note source, Note target) {
        return NoteLink.builder()
            .sourceNote(source)
            .targetNote(target)
            .linkType(NoteLinkType.FOLLOWS_FROM)
            .build();
    }

    public static NoteLink extends_(Note source, Note target) {
        return NoteLink.builder()
            .sourceNote(source)
            .targetNote(target)
            .linkType(NoteLinkType.EXTENDS)
            .weight(3)
            .build();
    }

    public static NoteLink contradicts(Note source, Note target) {
        return NoteLink.builder()
            .sourceNote(source)
            .targetNote(target)
            .linkType(NoteLinkType.CONTRADICTS)
            .weight(2)
            .build();
    }

    public static NoteLink summarizes(Note summary, Note source) {
        return NoteLink.builder()
            .sourceNote(summary)
            .targetNote(source)
            .linkType(NoteLinkType.SUMMARIZES)
            .weight(4)
            .build();
    }

    public static NoteLink implements_(Note implementation, Note concept, Integer weight) {
        return NoteLink.builder()
            .sourceNote(implementation)
            .targetNote(concept)
            .linkType(NoteLinkType.IMPLEMENTS)
            .weight(weight != null ? weight : 3)
            .build();
    }

    public static NoteLink parentChild(Note parent, Note child) {
        return NoteLink.builder()
            .sourceNote(parent)
            .targetNote(child)
            .linkType(NoteLinkType.PARENT_OF)
            .weight(5)
            .build();
    }

    // Utility methods
    public boolean isStrongLink() {
        return weight != null && weight >= 7;
    }

    public boolean isWeakLink() {
        return weight != null && weight <= 3;
    }

    public boolean involves(Note note) {
        return sourceNote.equals(note) || targetNote.equals(note);
    }

    public Note getOtherNote(Note note) {
        if (sourceNote.equals(note)) {
            return targetNote;
        } else if (targetNote.equals(note)) {
            return sourceNote;
        } else {
            throw new IllegalArgumentException("Note is not part of this link");
        }
    }

    // Add metadata helper methods
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    public String getMetadataAsString(String key) {
        Object value = getMetadata(key);
        return value != null ? value.toString() : null;
    }
}