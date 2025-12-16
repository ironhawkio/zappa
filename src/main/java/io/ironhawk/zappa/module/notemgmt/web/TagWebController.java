package io.ironhawk.zappa.module.notemgmt.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.service.NoteTagService;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagWebController {

    private final TagService tagService;
    private final NoteTagService noteTagService;

    @GetMapping
    public String listTags(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "15") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "") String color,
        Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Tag> tags;

        if (!search.isEmpty()) {
            tags = tagService.searchTags(search, pageable);
            model.addAttribute("search", search);
        } else if (!color.isEmpty()) {
            List<Tag> tagList = tagService.findTagsByColor(color);
            tags = tagService.getTags(pageable); // Fallback for pagination
            model.addAttribute("selectedColor", color);
        } else {
            tags = tagService.getTags(pageable);
        }

        // Add usage counts for each tag
        tags.getContent().forEach(tag -> {
            long usageCount = noteTagService.countNotesForTag(tag.getId());
            // We'll pass this in the view logic
        });

        model.addAttribute("tags", tags);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tags.getTotalPages());
        model.addAttribute("unusedTags", tagService.findUnusedTags());
        model.addAttribute("popularTags", tagService.findPopularTags());

        return "tags/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("tag", new Tag());
        return "tags/form";
    }

    @PostMapping
    public String createTag(
        @ModelAttribute Tag tag,
        RedirectAttributes redirectAttributes) {

        try {
            Tag createdTag = tagService.createTag(tag);
            redirectAttributes.addFlashAttribute("success",
                "Tag '" + createdTag.getName() + "' created successfully!");
            return "redirect:/tags";
        } catch (Exception e) {
            log.error("Error creating tag", e);
            redirectAttributes.addFlashAttribute("error",
                "Error creating tag: " + e.getMessage());
            return "redirect:/tags/new";
        }
    }

    @GetMapping("/{id}")
    public String viewTag(@PathVariable UUID id, Model model) {
        return tagService.getTagById(id)
            .map(tag -> {
                model.addAttribute("tag", tag);
                model.addAttribute("usageCount", noteTagService.countNotesForTag(id));
                model.addAttribute("relatedNotes", tagService.findTagsByNoteId(id));
                model.addAttribute("coOccurringTags", tagService.findCoOccurringTags(id));
                return "tags/view";
            })
            .orElse("redirect:/tags");
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable UUID id, Model model) {
        return tagService.getTagById(id)
            .map(tag -> {
                model.addAttribute("tag", tag);
                model.addAttribute("usageCount", noteTagService.countNotesForTag(id));
                return "tags/edit";
            })
            .orElse("redirect:/tags");
    }

    @PostMapping("/{id}/update")
    public String updateTag(
        @PathVariable UUID id,
        @ModelAttribute Tag tag,
        RedirectAttributes redirectAttributes) {

        try {
            tag.setId(id);
            Tag updatedTag = tagService.updateTag(tag);
            redirectAttributes.addFlashAttribute("success",
                "Tag '" + updatedTag.getName() + "' updated successfully!");
            return "redirect:/tags/" + id;
        } catch (Exception e) {
            log.error("Error updating tag", e);
            redirectAttributes.addFlashAttribute("error",
                "Error updating tag: " + e.getMessage());
            return "redirect:/tags/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteTag(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            Tag tag = tagService.getTagById(id).orElse(null);
            String tagName = tag != null ? tag.getName() : "Unknown";

            tagService.deleteTag(id);
            redirectAttributes.addFlashAttribute("success",
                "Tag '" + tagName + "' deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting tag", e);
            redirectAttributes.addFlashAttribute("error",
                "Error deleting tag: " + e.getMessage());
        }
        return "redirect:/tags";
    }

    @PostMapping("/cleanup-unused")
    public String cleanupUnusedTags(RedirectAttributes redirectAttributes) {
        try {
            List<Tag> unusedTags = tagService.findUnusedTags();
            int count = unusedTags.size();

            tagService.deleteUnusedTags();
            redirectAttributes.addFlashAttribute("success",
                "Cleaned up " + count + " unused tags!");
        } catch (Exception e) {
            log.error("Error cleaning up unused tags", e);
            redirectAttributes.addFlashAttribute("error",
                "Error cleaning up tags: " + e.getMessage());
        }
        return "redirect:/tags";
    }

    @GetMapping("/unused")
    public String listUnusedTags(Model model) {
        List<Tag> unusedTags = tagService.findUnusedTags();
        model.addAttribute("unusedTags", unusedTags);
        return "tags/unused";
    }

    @GetMapping("/popular")
    public String listPopularTags(Model model) {
        List<Tag> popularTags = tagService.findPopularTags();
        model.addAttribute("popularTags", popularTags);
        return "tags/popular";
    }
}