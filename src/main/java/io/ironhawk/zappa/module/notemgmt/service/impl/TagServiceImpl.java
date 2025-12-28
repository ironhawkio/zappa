package io.ironhawk.zappa.module.notemgmt.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.repository.NoteTagRepository;
import io.ironhawk.zappa.module.notemgmt.repository.TagRepository;
import io.ironhawk.zappa.module.notemgmt.service.GroupService;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
import io.ironhawk.zappa.security.entity.User;
import io.ironhawk.zappa.security.service.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final NoteTagRepository noteTagRepository;
    private final CurrentUserService currentUserService;
    private final GroupService groupService;

    @Override
    @Transactional
    public Tag createTag(Tag tag) {
        User currentUser = currentUserService.getCurrentUser();
        tag.setUser(currentUser);
        log.info("Creating new tag with name: {} for user: {}", tag.getName(), currentUser.getUsername());

        // Check if tag with same name already exists for this user
        if (tagRepository.existsByUserAndNameIgnoreCase(currentUser, tag.getName())) {
            throw new IllegalArgumentException("Tag with name '" + tag.getName() + "' already exists");
        }

        return tagRepository.save(tag);
    }

    @Override
    public Optional<Tag> getTagById(UUID id) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching tag with id: {} for user: {}", id, currentUser.getUsername());
        return tagRepository.findByIdAndUser(id, currentUser);
    }

    @Override
    @Transactional
    public Tag updateTag(Tag tag) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("Updating tag with id: {} for user: {}", tag.getId(), currentUser.getUsername());

        if (!tagRepository.existsById(tag.getId())) {
            throw new IllegalArgumentException("Tag not found with id: " + tag.getId());
        }

        // Check if name is being changed to an existing name
        Optional<Tag> existingTag = tagRepository.findByUserAndNameIgnoreCase(currentUser, tag.getName());
        if (existingTag.isPresent() && !existingTag.get().getId().equals(tag.getId())) {
            throw new IllegalArgumentException("Tag with name '" + tag.getName() + "' already exists");
        }

        // Set the current user to ensure user_id is not null
        tag.setUser(currentUser);

        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public void deleteTag(UUID id) {
        log.info("Deleting tag with id: {}", id);

        if (!tagRepository.existsById(id)) {
            throw new IllegalArgumentException("Tag not found with id: " + id);
        }

        // Delete all note-tag relationships first
        noteTagRepository.deleteByTagId(id);

        // Then delete the tag
        tagRepository.deleteById(id);
    }

    @Override
    public List<Tag> getAllTags() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching all tags for user: {}", currentUser.getUsername());
        return tagRepository.findByUserOrderByNameAsc(currentUser);
    }

    @Override
    public Optional<Tag> findTagByName(String name) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding tag by name: {} for user: {}", name, currentUser.getUsername());
        return tagRepository.findByUserAndNameIgnoreCase(currentUser, name);
    }

    @Override
    public List<Tag> findTagsByNamePattern(String namePattern) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding tags by name pattern: {} for user: {}", namePattern, currentUser.getUsername());
        return tagRepository.findByUserAndNameContainingIgnoreCase(currentUser, namePattern);
    }

    @Override
    public List<Tag> findTagsByColor(String color) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding tags by color: {} for user: {}", color, currentUser.getUsername());
        return tagRepository.findByUserAndColor(currentUser, color);
    }

    @Override
    public Page<Tag> getTags(Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching tags with pagination: {} for user: {}", pageable, currentUser.getUsername());
        return tagRepository.findByUserOrderByNameAsc(currentUser, pageable);
    }

    @Override
    public Page<Tag> searchTags(String namePattern, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Searching tags with pattern: {} and pagination: {} for user: {}", namePattern, pageable, currentUser.getUsername());
        return tagRepository.findByUserAndNameContainingIgnoreCase(currentUser, namePattern, pageable);
    }

    @Override
    public List<Tag> findUnusedTags() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding unused tags for user: {}", currentUser.getUsername());
        return tagRepository.findUnusedTagsByUser(currentUser);
    }

    @Override
    public List<Tag> findPopularTags() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding popular tags for user: {}", currentUser.getUsername());
        List<Object[]> results = tagRepository.findTagsWithUsageCountByUser(currentUser);
        return results.stream()
            .map(result -> (Tag) result[0])
            .toList();
    }

    @Override
    public List<Tag> findTagsWithMinUsage(int minUsage) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding tags with minimum usage: {} for user: {}", minUsage, currentUser.getUsername());
        return tagRepository.findTagsWithMinUsageByUser(currentUser, minUsage);
    }

    @Override
    public List<Tag> findTagsByNoteId(UUID noteId) {
        log.debug("Finding tags by note id: {}", noteId);
        return tagRepository.findByNoteId(noteId);
    }

    @Override
    public List<Tag> findCoOccurringTags(UUID tagId) {
        log.debug("Finding co-occurring tags for tag id: {}", tagId);
        return noteTagRepository.findCoOccurringTags(tagId);
    }

    @Override
    public boolean tagExists(String name) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Checking if tag exists with name: {} for user: {}", name, currentUser.getUsername());
        return tagRepository.existsByUserAndNameIgnoreCase(currentUser, name);
    }

    @Override
    @Transactional
    public Tag getOrCreateTag(String name, String color) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Getting or creating tag with name: {} for user: {}", name, currentUser.getUsername());

        Optional<Tag> existingTag = tagRepository.findByUserAndNameIgnoreCase(currentUser, name);
        if (existingTag.isPresent()) {
            return existingTag.get();
        }

        Tag newTag = Tag.of(name, color);
        newTag.setUser(currentUser);

        return tagRepository.save(newTag);
    }

    @Override
    @Transactional
    public void deleteUnusedTags() {
        log.info("Deleting unused tags");
        List<Tag> unusedTags = findUnusedTags();

        if (!unusedTags.isEmpty()) {
            log.info("Found {} unused tags to delete", unusedTags.size());
            tagRepository.deleteAll(unusedTags);
        }
    }

    // ===============================
    // Group-scoped tag operations
    // ===============================

    @Override
    public List<Tag> getTagsForGroup(UUID groupId) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Getting tags for group: {} and user: {}", groupId, currentUser.getUsername());

        if (groupId == null) {
            // Return all tags if no group specified
            return getAllTags();
        }

        Optional<Group> groupOpt = groupService.getGroupById(groupId);
        if (groupOpt.isEmpty()) {
            return getGlobalTags();
        }

        Group group = groupOpt.get();
        List<Group> groupHierarchy = groupService.getGroupHierarchy(groupId);
        groupHierarchy.add(group); // Include the group itself

        return tagRepository.findTagsAvailableForGroupHierarchy(currentUser, groupHierarchy);
    }

    @Override
    public List<Tag> getGroupSpecificTags(UUID groupId) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Getting group-specific tags for group: {} and user: {}", groupId, currentUser.getUsername());

        if (groupId == null) {
            return getGlobalTags();
        }

        return groupService.getGroupById(groupId)
            .map(group -> tagRepository.findByUserAndGroupOrderByNameAsc(currentUser, group))
            .orElse(List.of());
    }

    @Override
    public List<Tag> getGlobalTags() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Getting global tags for user: {}", currentUser.getUsername());
        return tagRepository.findByUserAndGroupIsNullOrderByNameAsc(currentUser);
    }

    @Override
    @Transactional
    public Tag createTagInGroup(Tag tag, UUID groupId) {
        User currentUser = currentUserService.getCurrentUser();
        tag.setUser(currentUser);

        if (groupId != null) {
            Group group = groupService.getGroupById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));
            tag.setGroup(group);
            log.info("Creating tag '{}' in group '{}' for user: {}", tag.getName(), group.getName(), currentUser.getUsername());

            // Check if tag already exists in this group context
            if (tagRepository.existsByUserAndNameIgnoreCaseInGroupOrGlobal(currentUser, tag.getName(), group)) {
                throw new IllegalArgumentException("Tag with name '" + tag.getName() + "' already exists in this group or globally");
            }
        } else {
            tag.setGroup(null);
            log.info("Creating global tag '{}' for user: {}", tag.getName(), currentUser.getUsername());

            // Check if global tag already exists
            if (tagRepository.existsByUserAndNameIgnoreCase(currentUser, tag.getName())) {
                throw new IllegalArgumentException("Global tag with name '" + tag.getName() + "' already exists");
            }
        }

        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public Tag createGlobalTag(Tag tag) {
        // Create in Default group instead of null
        Group defaultGroup = groupService.getDefaultGroup();
        return createTagInGroup(tag, defaultGroup.getId());
    }

    @Override
    @Transactional
    public Tag getOrCreateTagInGroup(String name, String color, UUID groupId) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Getting or creating tag '{}' in group: {} for user: {}", name, groupId, currentUser.getUsername());

        if (groupId != null) {
            Group group = groupService.getGroupById(groupId).orElse(null);
            if (group != null) {
                // Check if tag exists in group context (group-specific or global)
                Optional<Tag> existingTag = tagRepository.findByUserAndNameIgnoreCaseInGroupOrGlobal(currentUser, name, group);
                if (existingTag.isPresent()) {
                    return existingTag.get();
                }

                // Create new tag in group
                Tag newTag = Tag.ofGroup(name, color, group);
                newTag.setUser(currentUser);
                return tagRepository.save(newTag);
            }
        }

        // Fallback to default group tag
        return getOrCreateGlobalTag(name, color);
    }

    @Override
    @Transactional
    public Tag getOrCreateGlobalTag(String name, String color) {
        User currentUser = currentUserService.getCurrentUser();
        Group defaultGroup = groupService.getDefaultGroup();
        log.debug("Getting or creating tag '{}' in Default group for user: {}", name, currentUser.getUsername());

        // Look for existing tag in Default group
        Optional<Tag> existingTag = tagRepository.findByUserAndNameIgnoreCaseInGroupOrGlobal(currentUser, name, defaultGroup);
        if (existingTag.isPresent()) {
            return existingTag.get();
        }

        // Create new tag in Default group
        Tag newTag = Tag.ofGroup(name, color, defaultGroup);
        newTag.setUser(currentUser);
        return tagRepository.save(newTag);
    }

    @Override
    public boolean tagExistsInGroup(String name, UUID groupId) {
        User currentUser = currentUserService.getCurrentUser();

        if (groupId == null) {
            return tagRepository.existsByUserAndNameIgnoreCase(currentUser, name);
        }

        return groupService.getGroupById(groupId)
            .map(group -> tagRepository.existsByUserAndNameIgnoreCaseInGroupOrGlobal(currentUser, name, group))
            .orElse(false);
    }

    @Override
    public Optional<Tag> findTagByNameInGroup(String name, UUID groupId) {
        User currentUser = currentUserService.getCurrentUser();

        if (groupId == null) {
            return tagRepository.findByUserAndNameIgnoreCase(currentUser, name);
        }

        return groupService.getGroupById(groupId)
            .flatMap(group -> tagRepository.findByUserAndNameIgnoreCaseInGroupOrGlobal(currentUser, name, group));
    }

    @Override
    public List<Tag> findPopularTagsInGroup(UUID groupId) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding popular tags in group: {} for user: {}", groupId, currentUser.getUsername());

        if (groupId == null) {
            return findPopularTags();
        }

        return groupService.getGroupById(groupId)
            .map(group -> {
                List<Object[]> results = tagRepository.findTagsWithUsageCountByUserAndGroup(currentUser, group);
                return results.stream()
                    .map(result -> (Tag) result[0])
                    .toList();
            })
            .orElse(List.of());
    }

    @Override
    public List<Tag> findUnusedTagsInGroup(UUID groupId) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding unused tags in group: {} for user: {}", groupId, currentUser.getUsername());

        if (groupId == null) {
            return tagRepository.findUnusedGlobalTagsByUser(currentUser);
        }

        return groupService.getGroupById(groupId)
            .map(group -> tagRepository.findUnusedTagsByUserAndGroup(currentUser, group))
            .orElse(List.of());
    }

    @Override
    @Transactional
    public void deleteUnusedTagsInGroup(UUID groupId) {
        List<Tag> unusedTags = findUnusedTagsInGroup(groupId);
        if (!unusedTags.isEmpty()) {
            log.info("Deleting {} unused tags in group: {}", unusedTags.size(), groupId);
            tagRepository.deleteAll(unusedTags);
        }
    }

    @Override
    @Transactional
    public Tag moveTagToGroup(UUID tagId, UUID newGroupId) {
        User currentUser = currentUserService.getCurrentUser();
        Tag tag = tagRepository.findByIdAndUser(tagId, currentUser)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found with id: " + tagId));

        Group newGroup = null;
        if (newGroupId != null) {
            newGroup = groupService.getGroupById(newGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + newGroupId));
        }

        tag.setGroup(newGroup);
        log.info("Moving tag '{}' to group: {} for user: {}", tag.getName(), newGroupId, currentUser.getUsername());

        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public Tag makeTagGlobal(UUID tagId) {
        // Move to Default group instead of null
        Group defaultGroup = groupService.getDefaultGroup();
        return moveTagToGroup(tagId, defaultGroup.getId());
    }
}