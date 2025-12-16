package io.ironhawk.zappa.module.notemgmt.service;

import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagService {

    // Basic CRUD operations
    Tag createTag(Tag tag);
    Optional<Tag> getTagById(UUID id);
    Tag updateTag(Tag tag);
    void deleteTag(UUID id);
    List<Tag> getAllTags();

    // Search operations
    Optional<Tag> findTagByName(String name);
    List<Tag> findTagsByNamePattern(String namePattern);
    List<Tag> findTagsByColor(String color);

    // Pagination
    Page<Tag> getTags(Pageable pageable);
    Page<Tag> searchTags(String namePattern, Pageable pageable);

    // Tag analytics
    List<Tag> findUnusedTags();
    List<Tag> findPopularTags();
    List<Tag> findTagsWithMinUsage(int minUsage);
    List<Tag> findTagsByNoteId(UUID noteId);
    List<Tag> findCoOccurringTags(UUID tagId);

    // Utility methods
    boolean tagExists(String name);
    Tag getOrCreateTag(String name, String color);
    void deleteUnusedTags();
}