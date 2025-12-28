/**
 * TagSelector - A reusable utility for searchable tag selection
 *
 * Usage:
 * const tagSelector = new TagSelector({
 *     container: '.tag-selector-wrapper',
 *     tags: [{name: 'javascript', color: '#f7df1e'}, ...],
 *     onTagSelected: (tagName) => { ... },
 *     onTagRemoved: (tagName) => { ... }
 * });
 */
class TagSelector {
    constructor(options) {
        this.options = {
            container: options.container,
            tags: options.tags || [],
            selectedTags: options.selectedTags || [],
            placeholder: options.placeholder || 'Search and select tags...',
            onTagSelected: options.onTagSelected || (() => {}),
            onTagRemoved: options.onTagRemoved || (() => {}),
            onSelectionChanged: options.onSelectionChanged || (() => {}),
            maxHeight: options.maxHeight || '200px',
            debounceMs: options.debounceMs || 50,
            allowDuplicates: options.allowDuplicates || false
        };

        this.selectedTags = [...this.options.selectedTags];
        this.isDropdownVisible = false;
        this.highlightedIndex = -1;
        this.currentSuggestion = '';
        this.searchTimeout = null;

        this.init();
    }

    init() {
        this.createDOM();
        this.attachEventListeners();
        this.renderSelectedTags();
    }

    createDOM() {
        const container = document.querySelector(this.options.container);
        if (!container) {
            throw new Error(`TagSelector container not found: ${this.options.container}`);
        }

        container.innerHTML = `
            <div class="tag-input-container">
                <div class="selected-tags" data-selected-tags></div>
                <div class="search-input-wrapper">
                    <input type="text"
                           class="tag-search-input"
                           placeholder="${this.options.placeholder}"
                           autocomplete="off"
                           spellcheck="false"
                           data-search-input>
                    <div class="search-suggestion" data-search-suggestion></div>
                </div>
                <input type="hidden" data-hidden-input>
            </div>
            <div class="tag-dropdown" data-dropdown style="display: none;">
                <div class="tag-dropdown-content">
                    ${this.options.tags.map(tag => `
                        <div class="tag-option" data-tag-name="${tag.name}" data-tag-color="${tag.color || '#6c757d'}">
                            <span class="tag-color-indicator" style="background-color: ${tag.color || '#6c757d'}"></span>
                            <span>${tag.name}</span>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;

        // Store DOM references
        this.elements = {
            container,
            inputContainer: container.querySelector('.tag-input-container'),
            selectedTagsContainer: container.querySelector('[data-selected-tags]'),
            searchInput: container.querySelector('[data-search-input]'),
            searchSuggestion: container.querySelector('[data-search-suggestion]'),
            hiddenInput: container.querySelector('[data-hidden-input]'),
            dropdown: container.querySelector('[data-dropdown]'),
            dropdownContent: container.querySelector('.tag-dropdown-content')
        };
    }

    attachEventListeners() {
        const { searchInput, dropdown } = this.elements;

        // Search functionality
        searchInput.addEventListener('input', (e) => {
            const searchTerm = e.target.value;
            this.updateSuggestion(searchTerm);

            clearTimeout(this.searchTimeout);
            this.searchTimeout = setTimeout(() => {
                const matchCount = this.filterTagOptions(searchTerm.toLowerCase());
                if (searchTerm.trim() && matchCount > 0) {
                    this.showDropdown();
                } else if (!searchTerm.trim()) {
                    this.hideDropdown();
                }
            }, this.options.debounceMs);
        });

        // Focus handler
        searchInput.addEventListener('focus', () => {
            const searchTerm = searchInput.value.trim();
            if (searchTerm) {
                const matchCount = this.filterTagOptions(searchTerm.toLowerCase());
                if (matchCount > 0) {
                    this.showDropdown();
                }
            } else {
                this.filterTagOptions('');
                this.showDropdown();
            }
        });

        // Keyboard navigation
        searchInput.addEventListener('keydown', (e) => {
            this.handleKeydown(e);
        });

        // Dropdown clicks
        dropdown.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopImmediatePropagation();

            const tagOption = e.target.closest('.tag-option');
            if (tagOption) {
                const tagName = tagOption.getAttribute('data-tag-name');
                if (tagName) {
                    this.selectTag(tagName);
                }
            }
        });

        // Click outside to hide
        document.addEventListener('click', (e) => {
            if (!e.target.closest(this.options.container)) {
                this.hideDropdown();
            }
        });

        // Hide on scroll/resize
        window.addEventListener('scroll', () => this.hideDropdown(), { passive: true });
        window.addEventListener('resize', () => this.hideDropdown(), { passive: true });
    }

    handleKeydown(e) {
        const visibleOptions = Array.from(this.elements.dropdown.querySelectorAll('.tag-option:not([style*="display: none"])'));

        switch (e.key) {
            case 'Enter':
                e.preventDefault();
                if (this.highlightedIndex >= 0 && visibleOptions[this.highlightedIndex]) {
                    const tagName = visibleOptions[this.highlightedIndex].getAttribute('data-tag-name');
                    this.selectTag(tagName);
                } else if (this.currentSuggestion && this.elements.searchInput.value.trim()) {
                    this.selectTag(this.currentSuggestion);
                } else if (visibleOptions.length === 1) {
                    const tagName = visibleOptions[0].getAttribute('data-tag-name');
                    this.selectTag(tagName);
                } else if (visibleOptions.length > 1) {
                    this.highlightedIndex = 0;
                    this.updateHighlight(visibleOptions);
                }
                break;

            case 'Tab':
                if (this.currentSuggestion && this.elements.searchInput.value.trim() !== this.currentSuggestion) {
                    e.preventDefault();
                    this.elements.searchInput.value = this.currentSuggestion;
                    this.updateSuggestion(this.currentSuggestion);
                }
                break;

            case 'ArrowDown':
                e.preventDefault();
                if (visibleOptions.length > 0) {
                    this.highlightedIndex = Math.min(this.highlightedIndex + 1, visibleOptions.length - 1);
                    this.updateHighlight(visibleOptions);
                    this.showDropdown();
                }
                break;

            case 'ArrowUp':
                e.preventDefault();
                if (visibleOptions.length > 0) {
                    this.highlightedIndex = Math.max(this.highlightedIndex - 1, -1);
                    this.updateHighlight(visibleOptions);
                    this.showDropdown();
                }
                break;

            case 'Escape':
                this.hideDropdown();
                this.highlightedIndex = -1;
                break;
        }
    }

    updateSuggestion(searchTerm) {
        if (!searchTerm) {
            this.elements.searchSuggestion.innerHTML = '';
            this.currentSuggestion = '';
            return;
        }

        const unselectedTags = this.options.tags.filter(tag =>
            !this.selectedTags.includes(tag.name)
        );

        const matchingTags = unselectedTags.filter(tag =>
            tag.name.toLowerCase().includes(searchTerm.toLowerCase())
        );

        if (matchingTags.length === 0) {
            this.elements.searchSuggestion.innerHTML = '';
            this.currentSuggestion = '';
            return;
        }

        // Sort by relevance
        matchingTags.sort((a, b) => {
            const aLower = a.name.toLowerCase();
            const bLower = b.name.toLowerCase();
            const searchLower = searchTerm.toLowerCase();

            if (aLower === searchLower && bLower !== searchLower) return -1;
            if (aLower !== searchLower && bLower === searchLower) return 1;
            if (aLower.startsWith(searchLower) && !bLower.startsWith(searchLower)) return -1;
            if (!aLower.startsWith(searchLower) && bLower.startsWith(searchLower)) return 1;
            if (aLower.startsWith(searchLower) && bLower.startsWith(searchLower)) {
                return a.name.length - b.name.length;
            }
            return a.name.localeCompare(b.name);
        });

        const bestMatch = matchingTags[0];
        const exactMatch = bestMatch.name.toLowerCase() === searchTerm.toLowerCase();

        if (exactMatch) {
            if (matchingTags.length > 1) {
                this.elements.searchSuggestion.innerHTML = `<span class="suggestion-completion">(+${matchingTags.length - 1} more)</span>`;
            } else {
                this.elements.searchSuggestion.innerHTML = '';
            }
            this.currentSuggestion = '';
        } else if (bestMatch.name.toLowerCase().startsWith(searchTerm.toLowerCase())) {
            const matchPart = bestMatch.name.substring(0, searchTerm.length);
            const completionPart = bestMatch.name.substring(searchTerm.length);

            let suggestionHTML = `<span class="suggestion-match">${matchPart}</span><span class="suggestion-completion">${completionPart}</span>`;

            if (matchingTags.length > 1) {
                suggestionHTML += `<span class="suggestion-completion"> (+${matchingTags.length - 1} more)</span>`;
            }

            this.elements.searchSuggestion.innerHTML = suggestionHTML;
            this.currentSuggestion = bestMatch.name;
        } else {
            this.elements.searchSuggestion.innerHTML = `<span class="suggestion-completion">${matchingTags.length} match${matchingTags.length > 1 ? 'es' : ''}</span>`;
            this.currentSuggestion = '';
        }
    }

    filterTagOptions(searchTerm) {
        const options = this.elements.dropdown.querySelectorAll('.tag-option');
        let hasVisibleOptions = false;
        let matchingTags = [];

        this.highlightedIndex = -1;
        options.forEach(option => option.classList.remove('highlighted'));

        options.forEach(option => {
            const tagName = option.getAttribute('data-tag-name');
            if (!tagName) return;

            const tagNameLower = tagName.toLowerCase();
            const isSelected = this.selectedTags.includes(tagName);

            let matches = false;
            let exactMatch = false;

            if (!searchTerm) {
                matches = !isSelected;
            } else {
                matches = tagNameLower.includes(searchTerm.toLowerCase()) && !isSelected;
                exactMatch = tagNameLower === searchTerm.toLowerCase();
            }

            if (matches) {
                option.style.display = 'flex';
                hasVisibleOptions = true;
                this.highlightMatchingText(option, tagName, searchTerm);

                matchingTags.push({
                    element: option,
                    name: tagName,
                    nameLower: tagNameLower,
                    startsWithSearch: searchTerm ? tagNameLower.startsWith(searchTerm.toLowerCase()) : false,
                    exactMatch: exactMatch
                });
            } else {
                option.style.display = 'none';
                this.resetHighlighting(option);
            }
        });

        // Sort and reorder matching tags
        if (searchTerm && matchingTags.length > 0) {
            matchingTags.sort((a, b) => {
                if (a.exactMatch && !b.exactMatch) return -1;
                if (!a.exactMatch && b.exactMatch) return 1;
                if (a.startsWithSearch && !b.startsWithSearch) return -1;
                if (!a.startsWithSearch && b.startsWithSearch) return 1;
                return a.name.localeCompare(b.name);
            });

            matchingTags.forEach(tag => {
                this.elements.dropdownContent.appendChild(tag.element);
            });
        }

        // Show/hide no results message
        this.updateNoResultsMessage(hasVisibleOptions, searchTerm);

        return matchingTags.length;
    }

    highlightMatchingText(option, tagName, searchTerm) {
        if (!searchTerm) {
            this.resetHighlighting(option);
            return;
        }

        const tagTextSpan = option.querySelector('span:last-child');
        if (!tagTextSpan) return;

        const searchLower = searchTerm.toLowerCase();
        const tagNameLower = tagName.toLowerCase();
        const matchIndex = tagNameLower.indexOf(searchLower);

        if (matchIndex >= 0) {
            const beforeMatch = tagName.substring(0, matchIndex);
            const matchText = tagName.substring(matchIndex, matchIndex + searchTerm.length);
            const afterMatch = tagName.substring(matchIndex + searchTerm.length);

            tagTextSpan.innerHTML = beforeMatch +
                '<span class="match-highlight">' + matchText + '</span>' +
                afterMatch;
        } else {
            tagTextSpan.textContent = tagName;
        }
    }

    resetHighlighting(option) {
        const tagTextSpan = option.querySelector('span:last-child');
        if (tagTextSpan) {
            const tagName = option.getAttribute('data-tag-name');
            if (tagName) {
                tagTextSpan.textContent = tagName;
            }
        }
    }

    updateHighlight(visibleOptions) {
        visibleOptions.forEach(option => option.classList.remove('highlighted'));
        if (this.highlightedIndex >= 0 && visibleOptions[this.highlightedIndex]) {
            visibleOptions[this.highlightedIndex].classList.add('highlighted');
        }
    }

    updateNoResultsMessage(hasVisibleOptions, searchTerm) {
        let noResultsMsg = this.elements.dropdown.querySelector('.no-tags-found');

        if (!hasVisibleOptions && searchTerm) {
            if (!noResultsMsg) {
                noResultsMsg = document.createElement('div');
                noResultsMsg.className = 'no-tags-found';
                noResultsMsg.textContent = 'No tags found';
                this.elements.dropdownContent.appendChild(noResultsMsg);
            }
            noResultsMsg.style.display = 'block';
        } else if (noResultsMsg) {
            noResultsMsg.style.display = 'none';
        }
    }

    showDropdown() {
        if (this.isDropdownVisible) return;
        this.isDropdownVisible = true;
        this.elements.dropdown.classList.add('show');
    }

    hideDropdown() {
        if (!this.isDropdownVisible) return;

        this.isDropdownVisible = false;
        this.elements.dropdown.classList.remove('show');

        // Clear highlights and reset text highlighting
        this.elements.dropdown.querySelectorAll('.tag-option.highlighted').forEach(option => {
            option.classList.remove('highlighted');
        });
        this.elements.dropdown.querySelectorAll('.tag-option').forEach(option => {
            this.resetHighlighting(option);
        });
        this.highlightedIndex = -1;

        this.elements.searchInput.value = '';
        this.elements.searchSuggestion.innerHTML = '';
        this.currentSuggestion = '';
    }

    selectTag(tagName) {
        if (!this.options.allowDuplicates && this.selectedTags.includes(tagName)) {
            return;
        }

        this.selectedTags.push(tagName);
        this.renderSelectedTags();
        this.updateHiddenInput();
        this.hideDropdown();

        // Call callbacks
        this.options.onTagSelected(tagName);
        this.options.onSelectionChanged(this.selectedTags);
    }

    removeTag(tagName) {
        this.selectedTags = this.selectedTags.filter(tag => tag !== tagName);
        this.renderSelectedTags();
        this.updateHiddenInput();

        // Call callbacks
        this.options.onTagRemoved(tagName);
        this.options.onSelectionChanged(this.selectedTags);
    }

    renderSelectedTags() {
        this.elements.selectedTagsContainer.innerHTML = '';
        this.selectedTags.forEach(tagName => {
            const tagElement = document.createElement('span');
            tagElement.className = 'selected-tag';
            tagElement.innerHTML = `
                <span>${tagName}</span>
                <span class="tag-remove" data-remove-tag="${tagName}">&times;</span>
            `;

            // Add click handler for remove button
            tagElement.querySelector('[data-remove-tag]').addEventListener('click', (e) => {
                e.stopPropagation();
                this.removeTag(tagName);
            });

            this.elements.selectedTagsContainer.appendChild(tagElement);
        });
    }

    updateHiddenInput() {
        this.elements.hiddenInput.value = this.selectedTags.join(', ');
    }

    // Public methods for external control
    getSelectedTags() {
        return [...this.selectedTags];
    }

    setSelectedTags(tags) {
        this.selectedTags = [...tags];
        this.renderSelectedTags();
        this.updateHiddenInput();
    }

    addTag(tagName) {
        this.selectTag(tagName);
    }

    clear() {
        this.selectedTags = [];
        this.renderSelectedTags();
        this.updateHiddenInput();
        this.options.onSelectionChanged(this.selectedTags);
    }

    destroy() {
        clearTimeout(this.searchTimeout);
        // Remove event listeners would be here if needed
    }
}