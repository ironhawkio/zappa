package io.ironhawk.zappa.module.notemgmt.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import io.ironhawk.zappa.module.notemgmt.service.MarkdownService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MarkdownServiceImpl implements MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownServiceImpl() {
        // Configure CommonMark with extensions
        List<Extension> extensions = Arrays.asList(
            TablesExtension.create(),
            HeadingAnchorExtension.create(),
            AutolinkExtension.create()
        );

        this.parser = Parser.builder()
            .extensions(extensions)
            .build();

        this.renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .sanitizeUrls(true)
            .build();
    }

    @Override
    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }

        try {
            // Handle URLs specially - don't render URLs as markdown
            if (isUrl(markdown.trim())) {
                return String.format(
                    "<div class=\"url-content\">" +
                    "<i class=\"fas fa-external-link-alt me-2\"></i>" +
                    "<a href=\"%s\" target=\"_blank\" class=\"external-link\">%s</a>" +
                    "</div>",
                    markdown.trim(), markdown.trim()
                );
            }

            Node document = parser.parse(markdown);
            String html = renderer.render(document);

            // Add Confluence-style enhancements
            html = enhanceWithConfluenceStyles(html);

            return html;
        } catch (Exception e) {
            log.error("Error rendering markdown", e);
            return "<div class=\"error\">Error rendering content</div>";
        }
    }

    @Override
    public boolean isMarkdown(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        // Check for common markdown patterns
        return content.contains("#") ||
               content.contains("**") ||
               content.contains("*") ||
               content.contains("```") ||
               content.contains("`") ||
               content.contains("- ") ||
               content.contains("1. ") ||
               content.contains("[") ||
               content.contains("|");
    }

    @Override
    public String prepareForMarkdown(String plainText) {
        if (plainText == null) {
            return "";
        }

        // Escape existing markdown characters if needed
        return plainText
            .replace("\\", "\\\\")
            .replace("`", "\\`");
    }

    @Override
    public String generateTableOfContents(String markdown) {
        if (markdown == null) {
            return "";
        }

        StringBuilder toc = new StringBuilder();
        toc.append("<div class=\"table-of-contents\">");
        toc.append("<h4><i class=\"fas fa-list me-2\"></i>Table of Contents</h4>");
        toc.append("<ul>");

        String[] lines = markdown.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("#")) {
                String heading = line.replaceAll("^#+\\s*", "").trim();
                String anchor = heading.toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", "-");

                int level = line.indexOf(' ') - line.indexOf('#');
                String indent = "margin-left: " + (level * 20) + "px;";

                toc.append(String.format(
                    "<li style=\"%s\"><a href=\"#%s\">%s</a></li>",
                    indent, anchor, heading
                ));
            }
        }

        toc.append("</ul></div>");
        return toc.toString();
    }

    @Override
    public String sanitizeHtml(String html) {
        if (html == null) {
            return "";
        }

        // Basic HTML sanitization - in production, use a proper library like OWASP Java HTML Sanitizer
        return html
            .replaceAll("<script[^>]*>.*?</script>", "")
            .replaceAll("<iframe[^>]*>.*?</iframe>", "")
            .replaceAll("javascript:", "")
            .replaceAll("on\\w+\\s*=", "");
    }

    private boolean isUrl(String content) {
        return content.startsWith("http://") || content.startsWith("https://");
    }

    private String enhanceWithConfluenceStyles(String html) {
        // Add Confluence-style CSS classes
        html = html.replaceAll("<h1>", "<h1 class=\"confluence-h1\">");
        html = html.replaceAll("<h2>", "<h2 class=\"confluence-h2\">");
        html = html.replaceAll("<h3>", "<h3 class=\"confluence-h3\">");
        html = html.replaceAll("<h4>", "<h4 class=\"confluence-h4\">");
        html = html.replaceAll("<h5>", "<h5 class=\"confluence-h5\">");
        html = html.replaceAll("<h6>", "<h6 class=\"confluence-h6\">");

        html = html.replaceAll("<table>", "<table class=\"confluence-table table table-striped\">");
        html = html.replaceAll("<code>", "<code class=\"confluence-code\">");
        html = html.replaceAll("<pre>", "<pre class=\"confluence-pre\">");
        html = html.replaceAll("<blockquote>", "<blockquote class=\"confluence-blockquote\">");
        html = html.replaceAll("<ul>", "<ul class=\"confluence-list\">");
        html = html.replaceAll("<ol>", "<ol class=\"confluence-list\">");

        // Add info panels for important content
        html = html.replaceAll(
            "<blockquote class=\"confluence-blockquote\">\\s*<p><strong>Important:</strong>",
            "<div class=\"confluence-panel panel-info\"><div class=\"panel-header\">" +
            "<i class=\"fas fa-info-circle\"></i> Important</div><div class=\"panel-body\">"
        );

        html = html.replaceAll(
            "<blockquote class=\"confluence-blockquote\">\\s*<p><strong>Warning:</strong>",
            "<div class=\"confluence-panel panel-warning\"><div class=\"panel-header\">" +
            "<i class=\"fas fa-exclamation-triangle\"></i> Warning</div><div class=\"panel-body\">"
        );

        html = html.replaceAll(
            "<blockquote class=\"confluence-blockquote\">\\s*<p><strong>Note:</strong>",
            "<div class=\"confluence-panel panel-note\"><div class=\"panel-header\">" +
            "<i class=\"fas fa-sticky-note\"></i> Note</div><div class=\"panel-body\">"
        );

        // Close panels
        html = html.replaceAll("</p>\\s*</blockquote>", "</div></div>");

        return html;
    }
}