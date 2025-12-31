package io.ironhawk.zappa.module.notemgmt.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.dto.TagCreateRequest;
import io.ironhawk.zappa.module.notemgmt.dto.TagResponse;
import io.ironhawk.zappa.module.notemgmt.dto.TagUpdateRequest;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.service.NoteTagService;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Validated
public class TagController {

    private final TagService tagService;
    private final NoteTagService noteTagService;

    @PostMapping
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody TagCreateRequest request) {
        log.info("Creating tag with name: {}", request.getName());

        try {
            Tag tag = Tag.of(request.getName(), request.getColor());

            Tag createdTag = tagService.createTag(tag);
            return ResponseEntity.status(HttpStatus.CREATED).body(toTagResponse(createdTag));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getTag(@PathVariable UUID id) {
        log.info("Fetching tag with id: {}", id);

        return tagService.getTagById(id)
            .map(tag -> ResponseEntity.ok(toTagResponse(tag)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<TagResponse>> getAllTags(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("Fetching tags with pagination: page={}, size={}, sortBy={}, direction={}",
                 page, size, sortBy, sortDirection);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Tag> tags = tagService.getTags(pageable);
        Page<TagResponse> responsePages = tags.map(this::toTagResponse);

        return ResponseEntity.ok(responsePages);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TagResponse> updateTag(@PathVariable UUID id, @Valid @RequestBody TagUpdateRequest request) {
        log.info("Updating tag with id: {}", id);

        if (!id.equals(request.getId())) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Tag tag = Tag.forUpdate(request.getId(), request.getName(), request.getColor());

            Tag updatedTag = tagService.updateTag(tag);
            return ResponseEntity.ok(toTagResponse(updatedTag));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable UUID id) {
        log.info("Deleting tag with id: {}", id);

        try {
            tagService.deleteTag(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<TagResponse>> searchTags(@RequestParam String query) {
        log.info("Searching tags with query: {}", query);

        List<Tag> tags = tagService.findTagsByNamePattern(query);
        List<TagResponse> responses = tags.stream()
            .map(this::toTagResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-name")
    public ResponseEntity<TagResponse> getTagByName(@RequestParam String name) {
        log.info("Fetching tag by name: {}", name);

        return tagService.findTagByName(name)
            .map(tag -> ResponseEntity.ok(toTagResponse(tag)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-color")
    public ResponseEntity<List<TagResponse>> getTagsByColor(@RequestParam String color) {
        log.info("Fetching tags by color: {}", color);

        List<Tag> tags = tagService.findTagsByColor(color);
        List<TagResponse> responses = tags.stream()
            .map(this::toTagResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/key")
    public ResponseEntity<List<TagResponse>> getKeyTags() {
        log.info("Fetching key tags");

        List<Tag> tags = tagService.getKeyTags();
        List<TagResponse> responses = tags.stream()
            .map(this::toTagResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/mark-key")
    public ResponseEntity<TagResponse> markAsKeyTag(@PathVariable UUID id) {
        log.info("Marking tag {} as key tag", id);

        Tag tag = tagService.markAsKeyTag(id);
        return ResponseEntity.ok(toTagResponse(tag));
    }

    @PostMapping("/{id}/unmark-key")
    public ResponseEntity<TagResponse> unmarkAsKeyTag(@PathVariable UUID id) {
        log.info("Unmarking tag {} as key tag", id);

        Tag tag = tagService.unmarkAsKeyTag(id);
        return ResponseEntity.ok(toTagResponse(tag));
    }

    @GetMapping("/unused")
    public ResponseEntity<List<TagResponse>> getUnusedTags() {
        log.info("Fetching unused tags");

        List<Tag> tags = tagService.findUnusedTags();
        List<TagResponse> responses = tags.stream()
            .map(this::toTagResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<TagResponse>> getPopularTags() {
        log.info("Fetching popular tags");

        List<Tag> tags = tagService.findPopularTags();
        List<TagResponse> responses = tags.stream()
            .map(this::toTagResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/with-min-usage")
    public ResponseEntity<List<TagResponse>> getTagsWithMinUsage(@RequestParam int minUsage) {
        log.info("Fetching tags with minimum usage: {}", minUsage);

        List<Tag> tags = tagService.findTagsWithMinUsage(minUsage);
        List<TagResponse> responses = tags.stream()
            .map(this::toTagResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-note/{noteId}")
    public ResponseEntity<List<TagResponse>> getTagsByNoteId(@PathVariable UUID noteId) {
        log.info("Fetching tags for note: {}", noteId);

        List<Tag> tags = tagService.findTagsByNoteId(noteId);
        List<TagResponse> responses = tags.stream()
            .map(this::toTagResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/co-occurring")
    public ResponseEntity<List<TagResponse>> getCoOccurringTags(@PathVariable UUID id) {
        log.info("Fetching co-occurring tags for tag: {}", id);

        List<Tag> tags = tagService.findCoOccurringTags(id);
        List<TagResponse> responses = tags.stream()
            .map(this::toTagResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkTagExists(@RequestParam String name) {
        boolean exists = tagService.tagExists(name);
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/get-or-create")
    public ResponseEntity<TagResponse> getOrCreateTag(@RequestParam String name, @RequestParam(required = false) String color) {
        log.info("Getting or creating tag with name: {}", name);

        Tag tag = tagService.getOrCreateTag(name, color);
        return ResponseEntity.ok(toTagResponse(tag));
    }

    @DeleteMapping("/unused")
    public ResponseEntity<Void> deleteUnusedTags() {
        log.info("Deleting unused tags");

        tagService.deleteUnusedTags();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Long> getTagUsageCount(@PathVariable UUID id) {
        long count = noteTagService.countNotesForTag(id);
        return ResponseEntity.ok(count);
    }

    private TagResponse toTagResponse(Tag tag) {
        Long usageCount = noteTagService.countNotesForTag(tag.getId());

        return TagResponse.builder()
            .id(tag.getId())
            .name(tag.getName())
            .color(tag.getColor())
            .isKey(tag.isKey())
            .createdAt(tag.getCreatedAt())
            .updatedAt(tag.getUpdatedAt())
            .usageCount(usageCount)
            .build();
    }
}