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
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String color;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<NoteTag> noteTags = new HashSet<>();

    // Static factory methods
    public static Tag of(String name) {
        return Tag.builder()
            .name(name)
            .build();
    }

    public static Tag of(String name, String color) {
        return Tag.builder()
            .name(name)
            .color(color)
            .build();
    }

    public static Tag withColor(String name, String color) {
        return Tag.builder()
            .name(name)
            .color(color)
            .build();
    }

    // Common predefined colors
    public static Tag red(String name) {
        return Tag.of(name, "#FF0000");
    }

    public static Tag green(String name) {
        return Tag.of(name, "#00FF00");
    }

    public static Tag blue(String name) {
        return Tag.of(name, "#0000FF");
    }

    public static Tag yellow(String name) {
        return Tag.of(name, "#FFFF00");
    }

    public static Tag orange(String name) {
        return Tag.of(name, "#FFA500");
    }

    public static Tag purple(String name) {
        return Tag.of(name, "#800080");
    }

    public static Tag forUpdate(UUID id, String name, String color) {
        return Tag.builder()
            .id(id)
            .name(name)
            .color(color)
            .build();
    }
}