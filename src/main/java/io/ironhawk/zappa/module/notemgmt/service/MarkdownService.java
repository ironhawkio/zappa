package io.ironhawk.zappa.module.notemgmt.service;

public interface MarkdownService {

    /**
     * Convert markdown text to HTML with Confluence-style formatting
     */
    String renderToHtml(String markdown);

    /**
     * Check if content appears to be markdown
     */
    boolean isMarkdown(String content);

    /**
     * Convert plain text to markdown-ready format
     */
    String prepareForMarkdown(String plainText);

    /**
     * Extract table of contents from markdown content
     */
    String generateTableOfContents(String markdown);

    /**
     * Sanitize HTML output for safe rendering
     */
    String sanitizeHtml(String html);
}