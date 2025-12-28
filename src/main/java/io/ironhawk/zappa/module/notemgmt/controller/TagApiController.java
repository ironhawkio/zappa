package io.ironhawk.zappa.module.notemgmt.controller;

import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagApiController {

    private final TagService tagService;

    /**
     * Get tags available for a specific group (includes global and inherited tags)
     */
    @GetMapping("/for-group/{groupId}")
    public List<Tag> getTagsForGroup(@PathVariable UUID groupId) {
        log.debug("API request: getting tags for group {}", groupId);
        return tagService.getTagsForGroup(groupId);
    }

    /**
     * Get tags available globally (when no group context)
     */
    @GetMapping("/for-group/global")
    public List<Tag> getGlobalTags() {
        log.debug("API request: getting global tags");
        return tagService.getTagsForGroup(null);
    }

    /**
     * Get only group-specific tags (not including global/inherited)
     */
    @GetMapping("/group-specific/{groupId}")
    public List<Tag> getGroupSpecificTags(@PathVariable UUID groupId) {
        log.debug("API request: getting group-specific tags for group {}", groupId);
        return tagService.getGroupSpecificTags(groupId);
    }

    /**
     * Get only global tags
     */
    @GetMapping("/global-only")
    public List<Tag> getGlobalTagsOnly() {
        log.debug("API request: getting global tags only");
        return tagService.getGlobalTags();
    }

    /**
     * Get popular tags for a group
     */
    @GetMapping("/popular/group/{groupId}")
    public List<Tag> getPopularTagsForGroup(@PathVariable UUID groupId) {
        log.debug("API request: getting popular tags for group {}", groupId);
        return tagService.findPopularTagsInGroup(groupId);
    }

    /**
     * Get popular tags globally
     */
    @GetMapping("/popular/global")
    public List<Tag> getPopularGlobalTags() {
        log.debug("API request: getting popular global tags");
        return tagService.findPopularTagsInGroup(null);
    }

    /**
     * Move tag to different group
     */
    @PutMapping("/{tagId}/move-to-group/{groupId}")
    public Tag moveTagToGroup(@PathVariable UUID tagId, @PathVariable UUID groupId) {
        log.info("API request: moving tag {} to group {}", tagId, groupId);
        return tagService.moveTagToGroup(tagId, groupId);
    }

    /**
     * Make tag global
     */
    @PutMapping("/{tagId}/make-global")
    public Tag makeTagGlobal(@PathVariable UUID tagId) {
        log.info("API request: making tag {} global", tagId);
        return tagService.makeTagGlobal(tagId);
    }

    /**
     * Create tag in specific group
     */
    @PostMapping("/in-group/{groupId}")
    public Tag createTagInGroup(@RequestBody CreateTagRequest request, @PathVariable UUID groupId) {
        log.info("API request: creating tag '{}' in group {}", request.getName(), groupId);

        Tag tag = Tag.builder()
            .name(request.getName())
            .color(request.getColor())
            .build();

        return tagService.createTagInGroup(tag, groupId);
    }

    /**
     * Create global tag
     */
    @PostMapping("/global")
    public Tag createGlobalTag(@RequestBody CreateTagRequest request) {
        log.info("API request: creating global tag '{}'", request.getName());

        Tag tag = Tag.builder()
            .name(request.getName())
            .color(request.getColor())
            .build();

        return tagService.createGlobalTag(tag);
    }

    // DTO for tag creation
    public static class CreateTagRequest {
        private String name;
        private String color;

        public CreateTagRequest() {}

        public CreateTagRequest(String name, String color) {
            this.name = name;
            this.color = color;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }
}