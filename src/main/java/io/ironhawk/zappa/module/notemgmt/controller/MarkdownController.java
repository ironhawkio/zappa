package io.ironhawk.zappa.module.notemgmt.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.service.MarkdownService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/markdown")
@RequiredArgsConstructor
public class MarkdownController {

    private final MarkdownService markdownService;

    @PostMapping("/render")
    public ResponseEntity<String> renderMarkdown(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.ok("");
            }

            String renderedHtml = markdownService.renderToHtml(content);
            return ResponseEntity.ok(renderedHtml);
        } catch (Exception e) {
            log.error("Error rendering markdown", e);
            return ResponseEntity.badRequest().body("Error rendering markdown: " + e.getMessage());
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkMarkdown(@RequestParam String content) {
        boolean isMarkdown = markdownService.isMarkdown(content);

        return ResponseEntity.ok(Map.of(
            "isMarkdown", isMarkdown,
            "contentLength", content.length(),
            "lineCount", content.split("\n").length
        ));
    }

    @PostMapping("/toc")
    public ResponseEntity<String> generateTableOfContents(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.ok("");
            }

            String toc = markdownService.generateTableOfContents(content);
            return ResponseEntity.ok(toc);
        } catch (Exception e) {
            log.error("Error generating table of contents", e);
            return ResponseEntity.badRequest().body("Error generating TOC: " + e.getMessage());
        }
    }
}