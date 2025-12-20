package io.ironhawk.zappa.module.notemgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "note", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<NoteTag> noteTags = new HashSet<>();

    // Outgoing links from this note
    @OneToMany(mappedBy = "sourceNote", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<NoteLink> outgoingLinks = new HashSet<>();

    // Incoming links to this note
    @OneToMany(mappedBy = "targetNote", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<NoteLink> incomingLinks = new HashSet<>();

    // File attachments
    @OneToMany(mappedBy = "note", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<NoteAttachment> attachments = new HashSet<>();

    public static Note of(String title, String content) {
        return Note.builder()
            .title(title)
            .content(content)
            .build();
    }

    public static Note createEmpty(String title) {
        return Note.builder()
            .title(title)
            .build();
    }

    public static Note forUpdate(UUID id, String title, String content) {
        return Note.builder()
            .id(id)
            .title(title)
            .content(content)
            .build();
    }

    // Link utility methods
    public Set<NoteLink> getAllLinks() {
        Set<NoteLink> allLinks = new HashSet<>();
        allLinks.addAll(outgoingLinks);
        allLinks.addAll(incomingLinks);
        return allLinks;
    }

    public boolean isLinkedTo(Note other) {
        return outgoingLinks.stream().anyMatch(link -> link.getTargetNote().equals(other)) ||
               incomingLinks.stream().anyMatch(link -> link.getSourceNote().equals(other));
    }

    public boolean hasOutgoingLinkOfType(NoteLinkType linkType) {
        return outgoingLinks.stream().anyMatch(link -> link.getLinkType() == linkType);
    }

    public boolean hasIncomingLinkOfType(NoteLinkType linkType) {
        return incomingLinks.stream().anyMatch(link -> link.getLinkType() == linkType);
    }

    public int getLinkCount() {
        return outgoingLinks.size() + incomingLinks.size();
    }

    public boolean isOrphaned() {
        return outgoingLinks.isEmpty() && incomingLinks.isEmpty();
    }

    // Attachment utility methods
    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    public int getAttachmentCount() {
        return attachments.size();
    }

    public boolean hasWordDocuments() {
        return attachments.stream().anyMatch(NoteAttachment::isWordDocument);
    }

    public boolean hasPdfDocuments() {
        return attachments.stream().anyMatch(NoteAttachment::isPdf);
    }

    public boolean hasImages() {
        return attachments.stream().anyMatch(NoteAttachment::isImage);
    }

    public long getTotalAttachmentSize() {
        return attachments.stream()
            .mapToLong(attachment -> attachment.getFileSize() != null ? attachment.getFileSize() : 0L)
            .sum();
    }
}