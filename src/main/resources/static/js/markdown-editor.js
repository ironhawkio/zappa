// Markdown Editor with Live Preview

class MarkdownEditor {
    constructor(textareaId, previewId, options = {}) {
        this.textarea = document.getElementById(textareaId);
        this.preview = document.getElementById(previewId);
        this.options = {
            livePreview: true,
            toolbar: true,
            shortcuts: true,
            ...options
        };

        this.init();
    }

    init() {
        if (!this.textarea) return;

        this.createToolbar();
        this.setupLivePreview();
        this.setupShortcuts();
        this.setupResizing();
    }

    createToolbar() {
        if (!this.options.toolbar) return;

        const toolbar = document.createElement('div');
        toolbar.className = 'markdown-toolbar btn-toolbar mb-2';
        toolbar.innerHTML = `
            <div class="btn-group me-2" role="group">
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertMarkdown('**', '**')" title="Bold (Ctrl+B)">
                    <i class="fas fa-bold"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertMarkdown('*', '*')" title="Italic (Ctrl+I)">
                    <i class="fas fa-italic"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertMarkdown('\`', '\`')" title="Inline Code">
                    <i class="fas fa-code"></i>
                </button>
            </div>
            <div class="btn-group me-2" role="group">
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertMarkdown('# ', '')" title="H1">
                    H1
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertMarkdown('## ', '')" title="H2">
                    H2
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertMarkdown('### ', '')" title="H3">
                    H3
                </button>
            </div>
            <div class="btn-group me-2" role="group">
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertList()" title="List">
                    <i class="fas fa-list-ul"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertNumberedList()" title="Numbered List">
                    <i class="fas fa-list-ol"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertQuote()" title="Quote">
                    <i class="fas fa-quote-left"></i>
                </button>
            </div>
            <div class="btn-group me-2" role="group">
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertTable()" title="Table">
                    <i class="fas fa-table"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertCodeBlock()" title="Code Block">
                    <i class="fas fa-code"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="insertLink()" title="Link">
                    <i class="fas fa-link"></i>
                </button>
            </div>
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-sm btn-outline-info" onclick="insertPanel('Important')" title="Important Panel">
                    <i class="fas fa-info-circle"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-warning" onclick="insertPanel('Warning')" title="Warning Panel">
                    <i class="fas fa-exclamation-triangle"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-primary" onclick="insertPanel('Note')" title="Note Panel">
                    <i class="fas fa-sticky-note"></i>
                </button>
            </div>
        `;

        this.textarea.parentNode.insertBefore(toolbar, this.textarea);
    }

    setupLivePreview() {
        if (!this.options.livePreview || !this.preview) return;

        this.textarea.addEventListener('input', () => {
            this.updatePreview();
        });

        // Initial preview
        this.updatePreview();
    }

    updatePreview() {
        if (!this.preview) return;

        const content = this.textarea.value;
        if (!content.trim()) {
            this.preview.innerHTML = '<div class="text-muted">Live preview will appear here...</div>';
            return;
        }

        // Send to server for rendering
        fetch('/api/markdown/render', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ content: content })
        })
        .then(response => response.text())
        .then(html => {
            this.preview.innerHTML = `<div class="confluence-content">${html}</div>`;
        })
        .catch(error => {
            console.error('Error rendering markdown:', error);
            this.preview.innerHTML = '<div class="text-danger">Error rendering preview</div>';
        });
    }

    setupShortcuts() {
        if (!this.options.shortcuts) return;

        this.textarea.addEventListener('keydown', (e) => {
            if (e.ctrlKey || e.metaKey) {
                switch (e.key) {
                    case 'b':
                        e.preventDefault();
                        this.wrapSelection('**', '**');
                        break;
                    case 'i':
                        e.preventDefault();
                        this.wrapSelection('*', '*');
                        break;
                    case 'k':
                        e.preventDefault();
                        this.insertLink();
                        break;
                    case '`':
                        e.preventDefault();
                        this.wrapSelection('`', '`');
                        break;
                }
            }

            // Tab for indentation
            if (e.key === 'Tab') {
                e.preventDefault();
                this.insertAtCursor('    ');
            }
        });
    }

    setupResizing() {
        // Auto-resize textarea
        this.textarea.addEventListener('input', () => {
            this.textarea.style.height = 'auto';
            this.textarea.style.height = this.textarea.scrollHeight + 'px';
        });
    }

    wrapSelection(before, after) {
        const start = this.textarea.selectionStart;
        const end = this.textarea.selectionEnd;
        const selectedText = this.textarea.value.substring(start, end);
        const replacement = before + selectedText + after;

        this.replaceSelection(replacement);

        // Position cursor correctly
        if (selectedText === '') {
            this.textarea.setSelectionRange(start + before.length, start + before.length);
        } else {
            this.textarea.setSelectionRange(start, start + replacement.length);
        }
    }

    insertAtCursor(text) {
        const start = this.textarea.selectionStart;
        this.replaceSelection(text);
        this.textarea.setSelectionRange(start + text.length, start + text.length);
    }

    replaceSelection(text) {
        const start = this.textarea.selectionStart;
        const end = this.textarea.selectionEnd;
        const before = this.textarea.value.substring(0, start);
        const after = this.textarea.value.substring(end);

        this.textarea.value = before + text + after;
        this.textarea.dispatchEvent(new Event('input'));
    }

    insertLink() {
        const url = prompt('Enter URL:');
        const text = prompt('Enter link text:') || url;
        if (url) {
            this.insertAtCursor(`[${text}](${url})`);
        }
    }
}

// Global functions for toolbar buttons
function insertMarkdown(before, after = '') {
    const editor = getActiveEditor();
    if (editor) {
        editor.wrapSelection(before, after);
    }
}

function insertList() {
    const editor = getActiveEditor();
    if (editor) {
        editor.insertAtCursor('- ');
    }
}

function insertNumberedList() {
    const editor = getActiveEditor();
    if (editor) {
        editor.insertAtCursor('1. ');
    }
}

function insertQuote() {
    const editor = getActiveEditor();
    if (editor) {
        editor.insertAtCursor('> ');
    }
}

function insertTable() {
    const editor = getActiveEditor();
    if (editor) {
        const table = `| Column 1 | Column 2 | Column 3 |
|----------|----------|----------|
| Row 1    | Data     | Data     |
| Row 2    | Data     | Data     |

`;
        editor.insertAtCursor(table);
    }
}

function insertCodeBlock() {
    const editor = getActiveEditor();
    if (editor) {
        const language = prompt('Enter language (optional):') || '';
        editor.insertAtCursor(`\`\`\`${language}\n\n\`\`\`\n`);
    }
}

function insertLink() {
    const editor = getActiveEditor();
    if (editor) {
        editor.insertLink();
    }
}

function insertPanel(type) {
    const editor = getActiveEditor();
    if (editor) {
        const panelText = `> **${type}:** Your message here\n\n`;
        editor.insertAtCursor(panelText);
    }
}

function getActiveEditor() {
    // Return the currently focused editor instance
    return window.currentMarkdownEditor;
}

// Initialize markdown editors on page load
document.addEventListener('DOMContentLoaded', function() {
    // Auto-initialize any markdown editors
    const contentTextarea = document.getElementById('content');
    const preview = document.getElementById('markdown-preview');

    if (contentTextarea) {
        window.currentMarkdownEditor = new MarkdownEditor('content', 'markdown-preview');

        // Focus handling
        contentTextarea.addEventListener('focus', () => {
            window.currentMarkdownEditor = window.currentMarkdownEditor;
        });
    }
});