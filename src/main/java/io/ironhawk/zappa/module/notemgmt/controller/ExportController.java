package io.ironhawk.zappa.module.notemgmt.controller;

import io.ironhawk.zappa.module.notemgmt.entity.Note;
import io.ironhawk.zappa.module.notemgmt.entity.Tag;
import io.ironhawk.zappa.module.notemgmt.service.NoteService;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
import io.ironhawk.zappa.module.notemgmt.service.NoteTagService;
import io.ironhawk.zappa.security.entity.User;
import io.ironhawk.zappa.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final NoteService noteService;
    private final TagService tagService;
    private final NoteTagService noteTagService;
    private final CurrentUserService currentUserService;

    @GetMapping("/notes-and-tags-report")
    public ResponseEntity<String> exportNotesAndTagsReport() {
        User currentUser = currentUserService.getCurrentUser();
        log.info("EXEC: Generating notes and tags report for user: {}", currentUser.getUsername());

        long startTime = System.currentTimeMillis();

        try {
            StringBuilder report = new StringBuilder();

            // Report Header
            report.append("# NOTES AND TAGS ANALYSIS REPORT\n");
            report.append("Generated: ").append(new Date().toString()).append("\n");
            report.append("User: ").append(currentUser.getUsername()).append("\n\n");

            // 1. Tag Overview
            List<Tag> allTags = tagService.getAllTags();
            report.append("## TAG SYSTEM OVERVIEW\n");
            report.append("Total Tags: ").append(allTags.size()).append("\n\n");

            // Key tags
            List<Tag> keyTags = allTags.stream().filter(Tag::isKey).collect(Collectors.toList());
            if (!keyTags.isEmpty()) {
                report.append("### Key Tags (Important/Priority Tags):\n");
                for (Tag tag : keyTags) {
                    Long usageCount = noteTagService.countNotesForTag(tag.getId());
                    report.append("- ").append(tag.getName())
                          .append(" (used in ").append(usageCount).append(" notes)")
                          .append(tag.getColor() != null ? " [color: " + tag.getColor() + "]" : "")
                          .append("\n");
                }
                report.append("\n");
            }

            // All tags with usage stats
            report.append("### All Tags with Usage Statistics:\n");
            Map<Tag, Long> tagUsageMap = new HashMap<>();
            for (Tag tag : allTags) {
                Long count = noteTagService.countNotesForTag(tag.getId());
                tagUsageMap.put(tag, count);
            }

            tagUsageMap.entrySet().stream()
                .sorted(Map.Entry.<Tag, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    Tag tag = entry.getKey();
                    Long count = entry.getValue();
                    report.append("- ").append(tag.getName())
                          .append(": ").append(count).append(" notes")
                          .append(tag.isKey() ? " ⭐(KEY)" : "")
                          .append(tag.getColor() != null ? " [" + tag.getColor() + "]" : "")
                          .append("\n");
                });
            report.append("\n");

            // 2. Notes Analysis
            List<Note> allNotes = noteService.getAllNotes();
            report.append("## NOTES ANALYSIS\n");
            report.append("Total Notes: ").append(allNotes.size()).append("\n\n");

            // All notes with their tagging patterns
            report.append("### All Notes with Tagging Patterns:\n");
            allNotes.forEach(note -> {
                    List<Tag> noteTags = tagService.findTagsByNoteId(note.getId());
                    String tagList = noteTags.stream()
                        .map(tag -> tag.getName() + (tag.isKey() ? "⭐" : ""))
                        .collect(Collectors.joining(", "));

                    report.append("\n**Note: ").append(note.getTitle()).append("**\n");
                    report.append("Created: ").append(note.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
                    report.append("Tags: [").append(tagList.isEmpty() ? "none" : tagList).append("]\n");

                    // Content preview (first 200 chars)
                    if (note.getContent() != null && !note.getContent().trim().isEmpty()) {
                        String preview = note.getContent().length() > 200 ?
                            note.getContent().substring(0, 200) + "..." :
                            note.getContent();
                        report.append("Content Preview: ").append(preview.replaceAll("\\n", " ")).append("\n");
                    }
                });

            report.append("\n");

            // 3. Tag Co-occurrence Patterns
            report.append("## TAG CO-OCCURRENCE PATTERNS\n");
            report.append("Tags that frequently appear together:\n\n");

            Map<String, Integer> tagPairCounts = new HashMap<>();
            for (Note note : allNotes) {
                List<Tag> noteTags = tagService.findTagsByNoteId(note.getId());
                if (noteTags.size() > 1) {
                    for (int i = 0; i < noteTags.size(); i++) {
                        for (int j = i + 1; j < noteTags.size(); j++) {
                            String pair = noteTags.get(i).getName() + " + " + noteTags.get(j).getName();
                            tagPairCounts.merge(pair, 1, Integer::sum);
                        }
                    }
                }
            }

            tagPairCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1) // Only pairs that appear more than once
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
                .forEach(entry -> report.append("- ").append(entry.getKey())
                                       .append(": ").append(entry.getValue()).append(" times\n"));

            // 4. Content Analysis Summary
            report.append("\n## CONTENT ANALYSIS SUMMARY\n");

            // Common keywords across all notes
            Map<String, Integer> wordFrequency = new HashMap<>();
            for (Note note : allNotes) {
                String fullText = (note.getTitle() + " " + (note.getContent() != null ? note.getContent() : "")).toLowerCase();
                Arrays.stream(fullText.split("[^a-zA-Z0-9-]+"))
                    .filter(word -> word.length() > 3 && word.length() < 20)
                    .filter(word -> !isStopWord(word))
                    .forEach(word -> wordFrequency.merge(word, 1, Integer::sum));
            }

            report.append("### Most Common Keywords (potential new tags):\n");
            wordFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() > 2) // Words appearing in multiple notes
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .forEach(entry -> report.append("- ").append(entry.getKey())
                                       .append(": ").append(entry.getValue()).append(" occurrences\n"));

            // 5. ChatGPT Analysis Prompt
            report.append("\n## SUGGESTED CHATGPT ANALYSIS PROMPT\n");
            report.append("```\n");
            report.append("Based on this tagging system and content analysis, please:\n");
            report.append("1. Identify patterns in how I tag content\n");
            report.append("2. Suggest improvements to my tagging consistency\n");
            report.append("3. Recommend new tags that would better categorize my content\n");
            report.append("4. When I provide new content, suggest appropriate tags based on these patterns\n");
            report.append("5. Point out any gaps in my current tagging system\n");
            report.append("```\n");

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("EXEC: Export report completed - {} notes, {} tags analyzed, execution_time: {}ms",
                     allNotes.size(), allTags.size(), executionTime);

            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header("Content-Disposition", "attachment; filename=notes-tags-report.txt")
                .body(report.toString());

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("EXEC: Export report failed - execution_time: {}ms, error: {}", executionTime, e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error generating report: " + e.getMessage());
        }
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out",
            "this", "that", "with", "have", "from", "they", "know", "want", "been", "good", "much", "some", "time"
        );
        return stopWords.contains(word.toLowerCase());
    }
}