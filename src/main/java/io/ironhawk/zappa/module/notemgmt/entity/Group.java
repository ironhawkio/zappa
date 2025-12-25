package io.ironhawk.zappa.module.notemgmt.entity;

import io.ironhawk.zappa.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 7) // For hex color codes like #FF5722
    private String color;

    @Column(length = 50) // For Font Awesome icon classes like 'fas fa-seedling'
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Self-referencing for hierarchical structure
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private Group parentGroup;

    @OneToMany(mappedBy = "parentGroup", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Group> subGroups = new HashSet<>();

    // Notes in this group
    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Note> notes = new HashSet<>();

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Factory methods
    public static Group of(String name, String description) {
        return Group.builder()
            .name(name)
            .description(description)
            .color("#6c757d") // Default gray color
            .build();
    }

    public static Group of(String name, String description, String color, String icon) {
        return Group.builder()
            .name(name)
            .description(description)
            .color(color)
            .icon(icon)
            .build();
    }

    public static Group withParent(String name, String description, Group parentGroup) {
        return Group.builder()
            .name(name)
            .description(description)
            .parentGroup(parentGroup)
            .color("#6c757d")
            .build();
    }

    // Utility methods
    public boolean isRootGroup() {
        return parentGroup == null;
    }

    public boolean hasSubGroups() {
        return !subGroups.isEmpty();
    }

    public int getNoteCount() {
        return notes.size();
    }

    public int getTotalNoteCount() {
        int count = getNoteCount();
        for (Group subGroup : subGroups) {
            count += subGroup.getTotalNoteCount();
        }
        return count;
    }

    public String getFullName() {
        if (parentGroup != null) {
            return parentGroup.getFullName() + " > " + name;
        }
        return name;
    }

    public String getDisplayColor() {
        if (color != null && !color.isEmpty()) {
            return color;
        }
        if (parentGroup != null) {
            return parentGroup.getDisplayColor();
        }
        return "#6c757d"; // Default color
    }

    public String getDisplayIcon() {
        if (icon != null && !icon.isEmpty()) {
            return icon;
        }
        if (parentGroup != null) {
            return parentGroup.getDisplayIcon();
        }
        return "fas fa-folder"; // Default icon
    }
}