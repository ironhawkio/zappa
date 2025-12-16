package io.ironhawk.zappa.module.notemgmt.entity;

public enum NoteLinkType {
    RELATES_TO,
    REFERENCES,
    FOLLOWS_FROM,
    CONTRADICTS,
    EXTENDS,
    SUMMARIZES,
    IMPLEMENTS,
    INSPIRED_BY,
    PARENT_OF,
    CHILD_OF,
    SIMILAR_TO,
    PREREQUISITES,
    DERIVED_FROM,
    UPDATES,
    OBSOLETES,
    CITES,
    MENTIONS;

    public static NoteLinkType fromString(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown link type: " + name);
        }
    }

    // Semantic helpers for common relationships
    public boolean isHierarchical() {
        return this == PARENT_OF || this == CHILD_OF;
    }

    public boolean isSequential() {
        return this == FOLLOWS_FROM || this == PREREQUISITES || this == UPDATES;
    }

    public boolean isContentBased() {
        return this == SIMILAR_TO || this == EXTENDS || this == SUMMARIZES || this == DERIVED_FROM;
    }

    public boolean isConflicting() {
        return this == CONTRADICTS || this == OBSOLETES;
    }

    // Get the inverse relationship
    public NoteLinkType getInverse() {
        return switch (this) {
            case PARENT_OF -> CHILD_OF;
            case CHILD_OF -> PARENT_OF;
            case FOLLOWS_FROM -> PREREQUISITES;
            case PREREQUISITES -> FOLLOWS_FROM;
            case UPDATES -> OBSOLETES;
            case OBSOLETES -> UPDATES;
            case CITES -> MENTIONS; // Cited by
            default -> RELATES_TO; // Most relationships are symmetric or don't have clear inverses
        };
    }
}