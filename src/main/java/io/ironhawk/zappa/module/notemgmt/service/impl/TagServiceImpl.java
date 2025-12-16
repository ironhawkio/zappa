package io.ironhawk.zappa.module.notemgmt.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.repository.NoteTagRepository;
import io.ironhawk.zappa.module.notemgmt.repository.TagRepository;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
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

    @Override
    @Transactional
    public Tag createTag(Tag tag) {
        log.info("Creating new tag with name: {}", tag.getName());

        // Check if tag with same name already exists
        if (tagRepository.existsByNameIgnoreCase(tag.getName())) {
            throw new IllegalArgumentException("Tag with name '" + tag.getName() + "' already exists");
        }

        return tagRepository.save(tag);
    }

    @Override
    public Optional<Tag> getTagById(UUID id) {
        log.debug("Fetching tag with id: {}", id);
        return tagRepository.findById(id);
    }

    @Override
    @Transactional
    public Tag updateTag(Tag tag) {
        log.info("Updating tag with id: {}", tag.getId());

        if (!tagRepository.existsById(tag.getId())) {
            throw new IllegalArgumentException("Tag not found with id: " + tag.getId());
        }

        // Check if name is being changed to an existing name
        Optional<Tag> existingTag = tagRepository.findByNameIgnoreCase(tag.getName());
        if (existingTag.isPresent() && !existingTag.get().getId().equals(tag.getId())) {
            throw new IllegalArgumentException("Tag with name '" + tag.getName() + "' already exists");
        }

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
        log.debug("Fetching all tags");
        return tagRepository.findAll();
    }

    @Override
    public Optional<Tag> findTagByName(String name) {
        log.debug("Finding tag by name: {}", name);
        return tagRepository.findByNameIgnoreCase(name);
    }

    @Override
    public List<Tag> findTagsByNamePattern(String namePattern) {
        log.debug("Finding tags by name pattern: {}", namePattern);
        return tagRepository.findByNameContainingIgnoreCase(namePattern);
    }

    @Override
    public List<Tag> findTagsByColor(String color) {
        log.debug("Finding tags by color: {}", color);
        return tagRepository.findByColor(color);
    }

    @Override
    public Page<Tag> getTags(Pageable pageable) {
        log.debug("Fetching tags with pagination: {}", pageable);
        return tagRepository.findAll(pageable);
    }

    @Override
    public Page<Tag> searchTags(String namePattern, Pageable pageable) {
        log.debug("Searching tags with pattern: {} and pagination: {}", namePattern, pageable);
        return tagRepository.findByNameContainingIgnoreCase(namePattern, pageable);
    }

    @Override
    public List<Tag> findUnusedTags() {
        log.debug("Finding unused tags");
        return tagRepository.findUnusedTags();
    }

    @Override
    public List<Tag> findPopularTags() {
        log.debug("Finding popular tags");
        List<Object[]> results = tagRepository.findTagsWithUsageCount();
        return results.stream()
            .map(result -> (Tag) result[0])
            .toList();
    }

    @Override
    public List<Tag> findTagsWithMinUsage(int minUsage) {
        log.debug("Finding tags with minimum usage: {}", minUsage);
        return tagRepository.findTagsWithMinUsage(minUsage);
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
        log.debug("Checking if tag exists with name: {}", name);
        return tagRepository.existsByNameIgnoreCase(name);
    }

    @Override
    @Transactional
    public Tag getOrCreateTag(String name, String color) {
        log.debug("Getting or creating tag with name: {}", name);

        Optional<Tag> existingTag = tagRepository.findByNameIgnoreCase(name);
        if (existingTag.isPresent()) {
            return existingTag.get();
        }

        Tag newTag = Tag.of(name, color);

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