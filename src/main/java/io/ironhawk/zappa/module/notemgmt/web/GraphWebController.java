package io.ironhawk.zappa.module.notemgmt.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLink;
import io.ironhawk.zappa.module.notemgmt.service.GroupService;
import io.ironhawk.zappa.module.notemgmt.service.NoteLinkService;
import io.ironhawk.zappa.module.notemgmt.service.NoteService;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphWebController {

    private final NoteService noteService;
    private final NoteLinkService noteLinkService;
    private final GroupService groupService;
    private final TagService tagService;

    @GetMapping
    public String showGraphVisualization(
        @RequestParam(defaultValue = "") String group,
        @RequestParam(defaultValue = "false") boolean includeSubGroups,
        @RequestParam(defaultValue = "") String tags,
        @RequestParam(defaultValue = "any") String tagFilter, // "any" or "all"
        Model model) {

        List<Note> notes = getFilteredNotes(group, includeSubGroups, tags, tagFilter, model);
        List<NoteLink> links = getLinksForNotes(notes);

        model.addAttribute("totalNodes", notes.size());
        model.addAttribute("totalLinks", links.size());
        model.addAttribute("allGroups", groupService.getRootGroups());
        model.addAttribute("allTags", tagService.getAllTags());
        model.addAttribute("selectedTags", tags);
        model.addAttribute("tagFilter", tagFilter);

        return "graph/visualization";
    }

    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> getGraphData(
        @RequestParam(defaultValue = "") String group,
        @RequestParam(defaultValue = "false") boolean includeSubGroups,
        @RequestParam(defaultValue = "") String tags,
        @RequestParam(defaultValue = "any") String tagFilter) {

        List<Note> notes = getFilteredNotes(group, includeSubGroups, tags, tagFilter, null);
        List<NoteLink> links = getLinksForNotes(notes);

        // Create nodes data
        List<Map<String, Object>> nodes = notes.stream()
            .map(note -> {
                Map<String, Object> node = new HashMap<>();
                node.put("id", note.getId().toString());
                node.put("title", note.getTitle());
                node.put("content", note.getContent());
                node.put("createdAt", note.getCreatedAt().toString());

                // Calculate node properties
                long linkCount = noteLinkService.countLinksForNote(note.getId());
                node.put("linkCount", linkCount);
                // More pronounced size scaling: base 15, scale by linkCount * 8, max 60
                node.put("size", Math.max(15, Math.min(60, 15 + linkCount * 8))); // Size based on connections

                // Default color
                node.put("color", "#6c757d");

                return node;
            })
            .collect(Collectors.toList());

        // Create links data
        List<Map<String, Object>> linksData = links.stream()
            .map(link -> {
                Map<String, Object> linkData = new HashMap<>();
                linkData.put("id", link.getId().toString());
                linkData.put("source", link.getSourceNote().getId().toString());
                linkData.put("target", link.getTargetNote().getId().toString());
                linkData.put("type", link.getLinkType().toString());
                linkData.put("weight", link.getWeight());
                linkData.put("bidirectional", link.getIsBidirectional());

                // Line style based on weight
                linkData.put("strokeWidth", Math.max(1, link.getWeight() / 2));
                linkData.put("opacity", Math.max(0.3, link.getWeight() / 10.0));

                return linkData;
            })
            .collect(Collectors.toList());

        Map<String, Object> graphData = new HashMap<>();
        graphData.put("nodes", nodes);
        graphData.put("links", linksData);

        return graphData;
    }

    @GetMapping("/node/{nodeId}")
    @ResponseBody
    public Map<String, Object> getNodeData(@PathVariable UUID nodeId) {
        Optional<Note> noteOpt = noteService.getNoteWithTags(nodeId);
        if (noteOpt.isEmpty()) {
            return Collections.emptyMap();
        }

        Note note = noteOpt.get();
        List<NoteLink> outgoingLinks = noteLinkService.findOutgoingLinks(nodeId);
        List<NoteLink> incomingLinks = noteLinkService.findIncomingLinks(nodeId);

        Map<String, Object> nodeData = new HashMap<>();
        nodeData.put("id", note.getId().toString());
        nodeData.put("title", note.getTitle());
        nodeData.put("content", note.getContent());
        nodeData.put("tags", note.getNoteTags().stream()
            .map(nt -> nt.getTag().getName())
            .collect(Collectors.toList()));

        nodeData.put("outgoingLinks", outgoingLinks.stream()
            .map(link -> {
                Map<String, Object> linkMap = new HashMap<>();
                linkMap.put("target", link.getTargetNote().getTitle());
                linkMap.put("type", link.getLinkType().toString());
                linkMap.put("weight", link.getWeight());
                return linkMap;
            })
            .collect(Collectors.toList()));

        nodeData.put("incomingLinks", incomingLinks.stream()
            .map(link -> {
                Map<String, Object> linkMap = new HashMap<>();
                linkMap.put("source", link.getSourceNote().getTitle());
                linkMap.put("type", link.getLinkType().toString());
                linkMap.put("weight", link.getWeight());
                return linkMap;
            })
            .collect(Collectors.toList()));

        nodeData.put("totalLinks", outgoingLinks.size() + incomingLinks.size());
        nodeData.put("averageWeight", noteLinkService.getAverageWeightForNote(nodeId));

        return nodeData;
    }

    @GetMapping("/stats")
    @ResponseBody
    public Map<String, Object> getGraphStats() {
        List<Object[]> linkTypeStats = noteLinkService.getLinkTypeStatistics();
        List<UUID> mostConnected = noteLinkService.findMostConnectedNotes(5);
        List<UUID> orphaned = noteLinkService.findOrphanedNotes();

        Map<String, Object> stats = new HashMap<>();

        // Link type distribution
        Map<String, Long> linkTypes = linkTypeStats.stream()
            .collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> (Long) row[1]
            ));
        stats.put("linkTypeDistribution", linkTypes);

        // Hub nodes
        List<Map<String, Object>> hubs = mostConnected.stream()
            .map(nodeId -> noteService.getNoteById(nodeId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(note -> {
                Map<String, Object> hubData = new HashMap<>();
                hubData.put("id", note.getId().toString());
                hubData.put("title", note.getTitle());
                hubData.put("linkCount", noteLinkService.countLinksForNote(note.getId()));
                return hubData;
            })
            .collect(Collectors.toList());
        stats.put("hubs", hubs);

        stats.put("totalNodes", noteService.getAllNotes().size());
        stats.put("totalLinks", noteLinkService.getAllLinks().size());
        stats.put("orphanedNodes", orphaned.size());

        return stats;
    }

    private String getSourceColor(String source) {
        return switch (source) {
            case "BOOKMARK" -> "#ffc107";      // Yellow
            case "CONFLUENCE" -> "#0052cc";    // Blue
            case "GITHUB_PROJECT" -> "#24292f"; // Dark
            case "ONEDRIVE" -> "#0078d4";      // Light Blue
            case "WEB" -> "#28a745";           // Green
            case "MANUAL" -> "#6c757d";        // Gray
            default -> "#17a2b8";              // Teal
        };
    }

    private List<Note> getFilteredNotes(String group, boolean includeSubGroups, String tags, String tagFilter, Model model) {
        List<Note> notes;

        // First filter by group
        if (!group.isEmpty()) {
            try {
                UUID groupId = UUID.fromString(group);
                Optional<Group> selectedGroup = groupService.getGroupById(groupId);

                if (selectedGroup.isPresent()) {
                    if (includeSubGroups) {
                        notes = noteService.findNotesByGroupIncludingSubGroups(groupId);
                    } else {
                        notes = noteService.findNotesByGroup(groupId);
                    }

                    if (model != null) {
                        model.addAttribute("selectedGroup", selectedGroup.get());
                        model.addAttribute("includeSubGroups", includeSubGroups);
                    }
                } else {
                    notes = noteService.getAllNotes();
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid group ID: {}", group);
                notes = noteService.getAllNotes();
            }
        } else {
            notes = noteService.getAllNotes();
        }

        // Then filter by tags if specified
        if (!tags.isEmpty()) {
            List<String> tagNames = Arrays.asList(tags.split(","));
            tagNames = tagNames.stream().map(String::trim).filter(tag -> !tag.isEmpty()).collect(Collectors.toList());

            if (!tagNames.isEmpty()) {
                if ("all".equals(tagFilter)) {
                    // Notes must have ALL specified tags
                    notes = noteService.findNotesByAllTags(tagNames);
                } else {
                    // Notes must have ANY of the specified tags
                    notes = noteService.findNotesByAnyTags(tagNames);
                }
            }
        }

        return notes;
    }

    private List<NoteLink> getLinksForNotes(List<Note> notes) {
        if (notes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> noteIds = notes.stream().map(Note::getId).collect(Collectors.toSet());

        // Get all links and filter to only include links between the filtered notes
        return noteLinkService.getAllLinks().stream()
            .filter(link -> noteIds.contains(link.getSourceNote().getId()) &&
                           noteIds.contains(link.getTargetNote().getId()))
            .collect(Collectors.toList());
    }
}