package io.ironhawk.zappa.module.notemgmt.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLink;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLinkType;
import io.ironhawk.zappa.module.notemgmt.service.GroupService;
import io.ironhawk.zappa.module.notemgmt.service.MarkdownService;
import io.ironhawk.zappa.module.notemgmt.service.NoteLinkService;
import io.ironhawk.zappa.module.notemgmt.service.NoteService;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
import io.ironhawk.zappa.module.notemgmt.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteWebController {

    private final NoteService noteService;
    private final TagService tagService;
    private final MarkdownService markdownService;
    private final NoteLinkService noteLinkService;
    private final GroupService groupService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String listNotes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "") String tag,
        @RequestParam(defaultValue = "") String tags,
        @RequestParam(defaultValue = "OR") String tagOperator,
        @RequestParam(defaultValue = "") String group,
        @RequestParam(defaultValue = "false") boolean includeSubGroups,
        Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Note> notes;

        if (!search.isEmpty()) {
            notes = noteService.searchNotes(search, pageable);
            model.addAttribute("search", search);
        } else if (!group.isEmpty()) {
            // Handle group filtering
            try {
                UUID groupId = UUID.fromString(group);
                if (includeSubGroups) {
                    notes = noteService.findNotesByGroupIncludingSubGroups(groupId, pageable);
                } else {
                    notes = noteService.findNotesByGroup(groupId, pageable);
                }

                Optional<Group> selectedGroup = groupService.getGroupById(groupId);
                selectedGroup.ifPresent(g -> {
                    model.addAttribute("selectedGroup", g);
                    model.addAttribute("includeSubGroups", includeSubGroups);
                });
            } catch (IllegalArgumentException e) {
                log.warn("Invalid group ID: {}", group);
                notes = noteService.getNotes(pageable);
            }
        } else if (!tags.isEmpty()) {
            // Handle multiple tag filtering with AND/OR logic
            List<String> tagList = List.of(tags.split(","));
            tagList = tagList.stream().map(String::trim).filter(t -> !t.isEmpty()).toList();

            if ("AND".equalsIgnoreCase(tagOperator)) {
                notes = noteService.findNotesByAllTags(tagList, pageable);
            } else {
                notes = noteService.findNotesByAnyTags(tagList, pageable);
            }

            model.addAttribute("selectedTags", String.join(", ", tagList));
            model.addAttribute("tagOperator", tagOperator);
        } else if (!tag.isEmpty()) {
            // Handle single tag filtering (backward compatibility)
            notes = noteService.findNotesByAnyTags(List.of(tag), pageable);
            model.addAttribute("selectedTag", tag);
        } else {
            notes = noteService.getNotes(pageable);
        }

        model.addAttribute("notes", notes);
        model.addAttribute("allTags", tagService.getAllTags());
        model.addAttribute("allGroups", groupService.getRootGroups());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notes.getTotalPages());

        return "notes/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("note", new Note());
        model.addAttribute("allTags", tagService.getAllTags());
        model.addAttribute("allGroups", groupService.getAllGroupsWithParent());
        return "notes/form";
    }

    @PostMapping
    public String createNote(
        @ModelAttribute Note note,
        @RequestParam(required = false) String tagNames,
        @RequestParam(required = false) String groupId,
        @RequestParam(required = false) String noteLinks,
        @RequestParam(value = "uploadFiles", required = false) MultipartFile[] attachments,
        RedirectAttributes redirectAttributes) {

        try {
            // Set group if provided
            if (groupId != null && !groupId.isEmpty()) {
                try {
                    UUID groupUuid = UUID.fromString(groupId);
                    Optional<Group> group = groupService.getGroupById(groupUuid);
                    group.ifPresent(note::setGroup);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid group ID provided: {}", groupId);
                }
            }

            Note createdNote = noteService.createNote(note);

            // Add tags if provided
            if (tagNames != null && !tagNames.trim().isEmpty()) {
                String[] tags = tagNames.split(",");
                for (String tagName : tags) {
                    Tag tag = tagService.getOrCreateTag(tagName.trim(), null);
                    noteService.addTagToNote(createdNote.getId(), tag.getId());
                }
            }

            // Create note links if provided
            if (noteLinks != null && !noteLinks.trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> links = objectMapper.readValue(noteLinks, List.class);

                    for (Map<String, Object> linkData : links) {
                        String targetNoteId = (String) linkData.get("noteId");
                        String linkTypeStr = (String) linkData.get("linkType");
                        Integer weight = (Integer) linkData.get("weight");

                        NoteLinkType linkType = NoteLinkType.valueOf(linkTypeStr);
                        noteLinkService.createLink(
                            createdNote.getId(),
                            UUID.fromString(targetNoteId),
                            linkType,
                            weight
                        );
                    }

                    log.info("Created {} links for new note {}", links.size(), createdNote.getId());
                } catch (Exception e) {
                    log.error("Error creating note links", e);
                    // Don't fail the note creation, just log the error
                }
            }

            // Handle file uploads if provided
            if (attachments != null && attachments.length > 0) {
                int uploadedCount = 0;
                for (MultipartFile file : attachments) {
                    if (!file.isEmpty()) {
                        try {
                            fileStorageService.storeFile(file, createdNote);
                            uploadedCount++;
                        } catch (Exception e) {
                            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                            // Continue with other files, don't fail the entire operation
                        }
                    }
                }
                if (uploadedCount > 0) {
                    redirectAttributes.addFlashAttribute("success",
                        String.format("Note created successfully with %d file attachment(s)!", uploadedCount));
                } else {
                    redirectAttributes.addFlashAttribute("success", "Note created successfully!");
                }
            } else {
                redirectAttributes.addFlashAttribute("success", "Note created successfully!");
            }

            return "redirect:/notes/" + createdNote.getId();
        } catch (Exception e) {
            log.error("Error creating note", e);
            redirectAttributes.addFlashAttribute("error", "Error creating note: " + e.getMessage());
            return "redirect:/notes/new";
        }
    }

    @GetMapping("/{id}")
    public String viewNote(@PathVariable UUID id, Model model) {
        return noteService.getNoteWithTags(id)
            .map(note -> {
                model.addAttribute("note", note);
                model.addAttribute("tags", tagService.findTagsByNoteId(id));

                // Render content as Markdown if applicable
                if (note.getContent() != null && !note.getContent().trim().isEmpty()) {
                    String renderedContent = markdownService.renderToHtml(note.getContent());
                    model.addAttribute("renderedContent", renderedContent);
                    model.addAttribute("isMarkdown", markdownService.isMarkdown(note.getContent()));
                }

                // Get linked notes
                List<NoteLink> outgoingLinks = noteLinkService.findOutgoingLinks(id);
                List<NoteLink> incomingLinks = noteLinkService.findIncomingLinks(id);
                Long totalLinkCount = noteLinkService.countLinksForNote(id);

                model.addAttribute("outgoingLinks", outgoingLinks);
                model.addAttribute("incomingLinks", incomingLinks);
                model.addAttribute("totalLinkCount", totalLinkCount);

                return "notes/view";
            })
            .orElse("redirect:/notes");
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable UUID id, Model model) {
        return noteService.getNoteById(id)
            .map(note -> {
                model.addAttribute("note", note);
                model.addAttribute("allTags", tagService.getAllTags());
                model.addAttribute("allGroups", groupService.getAllGroupsWithParent());
                model.addAttribute("noteTags", tagService.findTagsByNoteId(id));

                // Get existing links for editing
                List<NoteLink> outgoingLinks = noteLinkService.findOutgoingLinks(id);
                List<NoteLink> incomingLinks = noteLinkService.findIncomingLinks(id);
                model.addAttribute("outgoingLinks", outgoingLinks);
                model.addAttribute("incomingLinks", incomingLinks);
                model.addAttribute("totalLinkCount", noteLinkService.countLinksForNote(id));

                return "notes/edit";
            })
            .orElse("redirect:/notes");
    }

    @PostMapping("/{id}/update")
    public String updateNote(
        @PathVariable UUID id,
        @ModelAttribute Note note,
        @RequestParam(required = false) String tagNames,
        @RequestParam(required = false) String groupId,
        @RequestParam(value = "uploadFiles", required = false) MultipartFile[] attachments,
        RedirectAttributes redirectAttributes) {

        try {
            note.setId(id);

            // Set group if provided
            if (groupId != null && !groupId.isEmpty()) {
                try {
                    UUID groupUuid = UUID.fromString(groupId);
                    Optional<Group> group = groupService.getGroupById(groupUuid);
                    group.ifPresent(note::setGroup);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid group ID provided: {}", groupId);
                }
            } else {
                note.setGroup(null); // Remove from group
            }

            Note updatedNote = noteService.updateNote(note);

            // Update tags - remove existing and add new ones
            List<Tag> existingTags = tagService.findTagsByNoteId(id);
            for (Tag tag : existingTags) {
                noteService.removeTagFromNote(id, tag.getId());
            }

            if (tagNames != null && !tagNames.trim().isEmpty()) {
                String[] tags = tagNames.split(",");
                for (String tagName : tags) {
                    Tag tag = tagService.getOrCreateTag(tagName.trim(), null);
                    noteService.addTagToNote(id, tag.getId());
                }
            }

            // Handle file uploads if provided
            if (attachments != null && attachments.length > 0) {
                int uploadedCount = 0;
                Note existingNote = noteService.getNoteById(id).orElse(null);
                if (existingNote != null) {
                    for (MultipartFile file : attachments) {
                        if (!file.isEmpty()) {
                            try {
                                fileStorageService.storeFile(file, existingNote);
                                uploadedCount++;
                            } catch (Exception e) {
                                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                                // Continue with other files, don't fail the entire operation
                            }
                        }
                    }
                }
                if (uploadedCount > 0) {
                    redirectAttributes.addFlashAttribute("success",
                        String.format("Note updated successfully with %d new file attachment(s)!", uploadedCount));
                } else {
                    redirectAttributes.addFlashAttribute("success", "Note updated successfully!");
                }
            } else {
                redirectAttributes.addFlashAttribute("success", "Note updated successfully!");
            }

            return "redirect:/notes/" + id;
        } catch (Exception e) {
            log.error("Error updating note", e);
            redirectAttributes.addFlashAttribute("error", "Error updating note: " + e.getMessage());
            return "redirect:/notes/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteNote(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            noteService.deleteNote(id);
            redirectAttributes.addFlashAttribute("success", "Note deleted successfully!");
        } catch (Exception e) {
            log.error("Error deleting note", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting note: " + e.getMessage());
        }
        return "redirect:/notes";
    }
}