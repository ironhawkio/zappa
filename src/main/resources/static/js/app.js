// Zappa Note Management - Frontend JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    alerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    // Add fade-in animation to cards
    const cards = document.querySelectorAll('.card');
    cards.forEach((card, index) => {
        setTimeout(() => {
            card.classList.add('fade-in');
        }, index * 50);
    });

    // Auto-resize textareas
    const textareas = document.querySelectorAll('textarea');
    textareas.forEach(textarea => {
        autoResize(textarea);
        textarea.addEventListener('input', () => autoResize(textarea));
    });

    // Enhanced tag input with autocomplete-like behavior
    setupTagInput();
});

function autoResize(textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = textarea.scrollHeight + 'px';
}

function setupTagInput() {
    const tagInput = document.getElementById('tagNames');
    if (!tagInput) return;

    // Add enter key handling for tags
    tagInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            const form = this.closest('form');
            if (form) {
                form.submit();
            }
        }
    });

    // Visual feedback for tag input
    tagInput.addEventListener('input', function() {
        const tags = this.value.split(',').map(t => t.trim()).filter(t => t);

        // Simple validation feedback
        if (tags.length > 10) {
            this.classList.add('is-invalid');
        } else {
            this.classList.remove('is-invalid');
        }
    });
}

// Loading utilities
function showButtonLoading(button, loadingText = null) {
    if (button) {
        button.classList.add('loading');
        button.disabled = true;

        // If loading text is provided, store original and update
        if (loadingText) {
            button.setAttribute('data-original-html', button.innerHTML);
            button.innerHTML = `<div class="loading-spinner me-2"></div>${loadingText}`;
            button.classList.remove('loading'); // Remove the transparent text class
            button.classList.add('loading-with-text');
        }
    }
}

function hideButtonLoading(button) {
    if (button) {
        // Restore original content if it was stored
        const originalHtml = button.getAttribute('data-original-html');
        if (originalHtml) {
            button.innerHTML = originalHtml;
            button.removeAttribute('data-original-html');
        }

        button.classList.remove('loading', 'loading-with-text');
        button.disabled = false;
    }
}

function showLoadingSpinner(container, text = 'Loading...') {
    if (!container) return;

    const spinner = document.createElement('div');
    spinner.className = 'loading-indicator d-flex align-items-center justify-content-center py-3';
    spinner.innerHTML = `
        <div class="loading-spinner"></div>
        <span class="loading-text">${text}</span>
    `;

    container.appendChild(spinner);
    return spinner;
}

function hideLoadingSpinner(container) {
    if (!container) return;

    const indicator = container.querySelector('.loading-indicator');
    if (indicator) {
        indicator.remove();
    }
}

// Form submission with loading states
function setupFormSubmission() {
    const forms = document.querySelectorAll('form[data-loading]');

    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitButton = form.querySelector('button[type="submit"], input[type="submit"]');

            if (submitButton) {
                // Check if this is a file upload form
                const fileInputs = form.querySelectorAll('input[type="file"]');
                const hasFiles = Array.from(fileInputs).some(input => input.files && input.files.length > 0);

                // Show different loading states for file uploads vs regular forms
                if (hasFiles && fileInputs.length > 0) {
                    const fileCount = Array.from(fileInputs).reduce((total, input) => total + (input.files ? input.files.length : 0), 0);

                    // Show loading with text for file uploads
                    showButtonLoading(submitButton, `Uploading ${fileCount} file(s)...`);
                } else {
                    // Show regular loading spinner for other forms
                    showButtonLoading(submitButton);
                }

                // Extended timeout for file uploads (2 minutes)
                const timeout = hasFiles ? 120000 : 30000;
                setTimeout(() => {
                    hideButtonLoading(submitButton);
                }, timeout);
            }
        });
    });
}

// Global utility functions
function addTag(tagName) {
    const tagInput = document.getElementById('tagNames');
    if (!tagInput) return;

    const currentTags = tagInput.value.split(',').map(t => t.trim()).filter(t => t);

    if (!currentTags.includes(tagName)) {
        currentTags.push(tagName);
        tagInput.value = currentTags.join(', ');

        // Trigger input event for validation
        tagInput.dispatchEvent(new Event('input'));

        // Visual feedback
        tagInput.focus();
        tagInput.setSelectionRange(tagInput.value.length, tagInput.value.length);
    }
}

function removeTag(tagName) {
    const tagInput = document.getElementById('tagNames');
    if (!tagInput) return;

    const currentTags = tagInput.value.split(',').map(t => t.trim()).filter(t => t);
    const filteredTags = currentTags.filter(tag => tag !== tagName);

    tagInput.value = filteredTags.join(', ');
    tagInput.dispatchEvent(new Event('input'));
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(function() {
        // Show temporary feedback
        showToast('Copied to clipboard!', 'success');
    }).catch(function() {
        // Fallback for older browsers
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showToast('Copied to clipboard!', 'success');
    });
}

function showToast(message, type = 'info') {
    // Create a simple toast notification
    const toast = document.createElement('div');
    toast.className = `alert alert-${type} position-fixed`;
    toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 250px;';
    toast.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check' : 'info'}-circle me-2"></i>
        ${message}
    `;

    document.body.appendChild(toast);

    // Auto-remove after 3 seconds
    setTimeout(() => {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }, 3000);
}

// Form validation helpers
function validateForm(form) {
    const title = form.querySelector('#title');

    if (!title || !title.value.trim()) {
        showToast('Title is required!', 'danger');
        if (title) title.focus();
        return false;
    }

    return true;
}

// Enhanced search functionality
function setupSearch() {
    const searchInput = document.querySelector('input[name="search"]');
    if (!searchInput) return;

    let searchTimeout;

    searchInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);

        // Debounce search to avoid too many requests
        searchTimeout = setTimeout(() => {
            if (this.value.length >= 3) {
                // Could implement real-time search here
                console.log('Searching for:', this.value);
            }
        }, 500);
    });
}

// Initialize search functionality
document.addEventListener('DOMContentLoaded', setupSearch);

// Initialize form loading states
document.addEventListener('DOMContentLoaded', setupFormSubmission);