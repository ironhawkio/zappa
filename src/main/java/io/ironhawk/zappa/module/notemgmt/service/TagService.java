package io.ironhawk.zappa.module.notemgmt.service;

import io.ironhawk.zappa.module.notemgmt.entity.Group;
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

    // ===============================
    // Group-scoped tag operations
    // ===============================

    // Get tags available for a specific group (includes inherited and global tags)
    List<Tag> getTagsForGroup(UUID groupId);

    // Get only tags specific to a group (not including global/inherited)
    List<Tag> getGroupSpecificTags(UUID groupId);

    // Get global tags only
    List<Tag> getGlobalTags();

    // Create tag in specific group
    Tag createTagInGroup(Tag tag, UUID groupId);

    // Create global tag
    Tag createGlobalTag(Tag tag);

    // Get or create tag in specific group context
    Tag getOrCreateTagInGroup(String name, String color, UUID groupId);

    // Get or create global tag
    Tag getOrCreateGlobalTag(String name, String color);

    // Check if tag exists in group context (including global and inherited)
    boolean tagExistsInGroup(String name, UUID groupId);

    // Find tag by name in group context
    Optional<Tag> findTagByNameInGroup(String name, UUID groupId);

    // Get tag usage statistics for group
    List<Tag> findPopularTagsInGroup(UUID groupId);

    // Find unused tags in group
    List<Tag> findUnusedTagsInGroup(UUID groupId);

    // Delete unused tags in specific group
    void deleteUnusedTagsInGroup(UUID groupId);

    // Move tag between groups
    Tag moveTagToGroup(UUID tagId, UUID newGroupId);

    // Make tag global
    Tag makeTagGlobal(UUID tagId);
}