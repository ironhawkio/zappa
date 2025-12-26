package io.ironhawk.zappa.module.notemgmt.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.repository.NoteTagRepository;
import io.ironhawk.zappa.module.notemgmt.repository.TagRepository;
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
}