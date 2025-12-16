package io.ironhawk.zappa.module.notemgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "note_tags")
@IdClass(NoteTag.NoteTagId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NoteTag {

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "note_id", referencedColumnName = "id")
    private Note note;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tag_id", referencedColumnName = "id")
    private Tag tag;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public NoteTag(Note note, Tag tag) {
        this.note = note;
        this.tag = tag;
    }

    public static NoteTag of(Note note, Tag tag) {
        return new NoteTag(note, tag);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteTagId implements Serializable {
        private UUID note;
        private UUID tag;

        public static NoteTagId of(UUID noteId, UUID tagId) {
            return new NoteTagId(noteId, tagId);
        }
    }
}