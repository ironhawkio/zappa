/**
 * SearchableSelector - A reusable utility for searchable selection of any data type
 *
 * Usage Examples:
 *
 * // For Tags
 * const tagSelector = new SearchableSelector({
 *     container: '.tag-selector-wrapper',
 *     data: [{name: 'javascript', color: '#f7df1e'}, ...],
 *     searchProperty: 'name',
 *     displayProperty: 'name',
 *     placeholder: 'Search tags...',
 *     renderOption: (item) => `<span class="color-dot" style="background:${item.color}"></span> ${item.name}`,
 *     onSelectionChanged: (selectedItems) => { ... }
 * });
 *
 * // For Notes
 * const noteSelector = new SearchableSelector({
 *     container: '.note-selector-wrapper',
 *     data: [{id: '123', title: 'My Note', content: 'Note content'}, ...],
 *     searchProperty: 'title',
 *     displayProperty: 'title',
 *     placeholder: 'Search notes...',
 *     renderOption: (note) => `<strong>${note.title}</strong><br><small>${note.content.substring(0, 50)}...</small>`,
 *     onSelectionChanged: (selectedNotes) => { ... }
 * });
 *
 * // For Groups
 * const groupSelector = new SearchableSelector({
 *     container: '.group-selector-wrapper',
 *     data: [{id: '456', name: 'Work', icon: 'fas fa-briefcase'}, ...],
 *     searchProperty: 'name',
 *     displayProperty: 'name',
 *     placeholder: 'Search groups...',
 *     renderOption: (group) => `<i class="${group.icon}"></i> ${group.name}`,
 *     multiSelect: false,
 *     onSelectionChanged: (selectedGroups) => { ... }
 * });
 */
class SearchableSelector {
    constructor(options) {
        this.options = {
            container: options.container,
            data: options.data || [],
            selectedItems: options.selectedItems || [],

            // Search configuration
            searchProperty: options.searchProperty || 'name',
            searchProperties: options.searchProperties || [options.searchProperty || 'name'],
            displayProperty: options.displayProperty || 'name',
            valueProperty: options.valueProperty || 'id',

            // UI configuration
            placeholder: options.placeholder || 'Search and select...',
            multiSelect: options.multiSelect !== false, // Default to true
            allowDuplicates: options.allowDuplicates || false,
            maxHeight: options.maxHeight || '200px',
            debounceMs: options.debounceMs || 50,
            minSearchLength: options.minSearchLength || 0,

            // Rendering customization
            renderOption: options.renderOption || this.defaultRenderOption.bind(this),
            renderSelectedItem: options.renderSelectedItem || this.defaultRenderSelectedItem.bind(this),

            // Callbacks
            onSelectionChanged: options.onSelectionChanged || (() => {}),
            onItemSelected: options.onItemSelected || (() => {}),
            onItemRemoved: options.onItemRemoved || (() => {}),

            // Custom CSS classes
            cssClasses: {
                container: 'searchable-input-container',
                selectedItems: 'selected-items',
                searchInput: 'searchable-input',
                suggestion: 'search-suggestion',
                dropdown: 'searchable-dropdown',
                option: 'searchable-option',
                selectedItem: 'selected-item',
                ...options.cssClasses
            }
        };

        this.selectedItems = [...this.options.selectedItems];
        this.isDropdownVisible = false;
        this.highlightedIndex = -1;
        this.currentSuggestion = '';
        this.searchTimeout = null;

        this.init();
    }

    init() {
        this.createDOM();
        this.attachEventListeners();
        this.renderSelectedItems();
    }

    createDOM() {
        const container = document.querySelector(this.options.container);
        if (!container) {
            throw new Error(`SearchableSelector container not found: ${this.options.container}`);
        }

        container.innerHTML = `
            <div class="${this.options.cssClasses.container}">
                ${this.options.multiSelect ? `<div class="${this.options.cssClasses.selectedItems}" data-selected-items></div>` : ''}
                <div class="search-input-wrapper">
                    <input type="text"
                           class="${this.options.cssClasses.searchInput}"
                           placeholder="${this.options.placeholder}"
                           autocomplete="off"
                           spellcheck="false"
                           data-search-input>
                    <div class="${this.options.cssClasses.suggestion}" data-search-suggestion></div>
                </div>
                <input type="hidden" data-hidden-input>
            </div>
            <div class="${this.options.cssClasses.dropdown}" data-dropdown style="display: none; max-height: ${this.options.maxHeight};">
                <div class="dropdown-content">
                    ${this.options.data.map(item => this.createOptionHTML(item)).join('')}
                </div>
            </div>
        `;

        this.cacheElements(container);
    }

    cacheElements(container) {
        this.elements = {
            container,
            inputContainer: container.querySelector(`.${this.options.cssClasses.container}`),
            selectedItemsContainer: container.querySelector('[data-selected-items]'),
            searchInput: container.querySelector('[data-search-input]'),
            searchSuggestion: container.querySelector('[data-search-suggestion]'),
            hiddenInput: container.querySelector('[data-hidden-input]'),
            dropdown: container.querySelector('[data-dropdown]'),
            dropdownContent: container.querySelector('.dropdown-content')
        };
    }

    createOptionHTML(item) {
        const value = this.getItemValue(item);
        const display = this.getItemDisplay(item);

        return `
            <div class="${this.options.cssClasses.option}"
                 data-item-value="${value}"
                 data-item-display="${display}"
                 data-item-data='${JSON.stringify(item)}'>
                ${this.options.renderOption(item)}
            </div>
        `;
    }

    attachEventListeners() {
        const { searchInput, dropdown } = this.elements;

        // Search functionality
        searchInput.addEventListener('input', (e) => {
            const searchTerm = e.target.value;

            if (searchTerm.length >= this.options.minSearchLength) {
                this.updateSuggestion(searchTerm);

                clearTimeout(this.searchTimeout);
                this.searchTimeout = setTimeout(() => {
                    const matchCount = this.filterOptions(searchTerm.toLowerCase());
                    if (searchTerm.trim() && matchCount > 0) {
                        this.showDropdown();
                    } else if (!searchTerm.trim()) {
                        this.hideDropdown();
                    }
                }, this.options.debounceMs);
            } else {
                this.hideDropdown();
            }
        });

        // Focus handler
        searchInput.addEventListener('focus', () => {
            const searchTerm = searchInput.value.trim();
            if (searchTerm.length >= this.options.minSearchLength) {
                const matchCount = this.filterOptions(searchTerm.toLowerCase());
                if (matchCount > 0) {
                    this.showDropdown();
                }
            } else {
                this.filterOptions('');
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

            const option = e.target.closest(`.${this.options.cssClasses.option}`);
            if (option) {
                const itemData = JSON.parse(option.getAttribute('data-item-data'));
                this.selectItem(itemData);
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
        const visibleOptions = Array.from(this.elements.dropdown.querySelectorAll(`.${this.options.cssClasses.option}:not([style*="display: none"])`));

        switch (e.key) {
            case 'Enter':
                e.preventDefault();
                if (this.highlightedIndex >= 0 && visibleOptions[this.highlightedIndex]) {
                    const itemData = JSON.parse(visibleOptions[this.highlightedIndex].getAttribute('data-item-data'));
                    this.selectItem(itemData);
                } else if (this.currentSuggestion) {
                    const suggestionItem = this.options.data.find(item =>
                        this.getItemDisplay(item).toLowerCase() === this.currentSuggestion.toLowerCase()
                    );
                    if (suggestionItem) {
                        this.selectItem(suggestionItem);
                    }
                } else if (visibleOptions.length === 1) {
                    const itemData = JSON.parse(visibleOptions[0].getAttribute('data-item-data'));
                    this.selectItem(itemData);
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
        if (!searchTerm || !this.elements.searchSuggestion) {
            this.elements.searchSuggestion.innerHTML = '';
            this.currentSuggestion = '';
            return;
        }

        const unselectedItems = this.options.data.filter(item =>
            !this.isItemSelected(item)
        );

        const matchingItems = unselectedItems.filter(item =>
            this.itemMatches(item, searchTerm)
        );

        if (matchingItems.length === 0) {
            this.elements.searchSuggestion.innerHTML = '';
            this.currentSuggestion = '';
            return;
        }

        // Sort by relevance
        matchingItems.sort((a, b) => this.compareItemRelevance(a, b, searchTerm));

        const bestMatch = matchingItems[0];
        const bestMatchDisplay = this.getItemDisplay(bestMatch);
        const exactMatch = bestMatchDisplay.toLowerCase() === searchTerm.toLowerCase();

        if (exactMatch) {
            if (matchingItems.length > 1) {
                this.elements.searchSuggestion.innerHTML = `<span class="suggestion-completion">(+${matchingItems.length - 1} more)</span>`;
            } else {
                this.elements.searchSuggestion.innerHTML = '';
            }
            this.currentSuggestion = '';
        } else if (bestMatchDisplay.toLowerCase().startsWith(searchTerm.toLowerCase())) {
            const matchPart = bestMatchDisplay.substring(0, searchTerm.length);
            const completionPart = bestMatchDisplay.substring(searchTerm.length);

            let suggestionHTML = `<span class="suggestion-match">${matchPart}</span><span class="suggestion-completion">${completionPart}</span>`;

            if (matchingItems.length > 1) {
                suggestionHTML += `<span class="suggestion-completion"> (+${matchingItems.length - 1} more)</span>`;
            }

            this.elements.searchSuggestion.innerHTML = suggestionHTML;
            this.currentSuggestion = bestMatchDisplay;
        } else {
            this.elements.searchSuggestion.innerHTML = `<span class="suggestion-completion">${matchingItems.length} match${matchingItems.length > 1 ? 'es' : ''}</span>`;
            this.currentSuggestion = '';
        }
    }

    filterOptions(searchTerm) {
        const options = this.elements.dropdown.querySelectorAll(`.${this.options.cssClasses.option}`);
        let hasVisibleOptions = false;
        let matchingItems = [];

        this.highlightedIndex = -1;
        options.forEach(option => option.classList.remove('highlighted'));

        options.forEach(option => {
            const itemData = JSON.parse(option.getAttribute('data-item-data'));
            const isSelected = this.isItemSelected(itemData);

            let matches = false;
            if (!searchTerm) {
                matches = !isSelected;
            } else {
                matches = this.itemMatches(itemData, searchTerm) && !isSelected;
            }

            if (matches) {
                option.style.display = 'flex';
                hasVisibleOptions = true;
                this.highlightMatchingText(option, itemData, searchTerm);

                matchingItems.push({
                    element: option,
                    data: itemData,
                    relevance: this.getItemRelevance(itemData, searchTerm)
                });
            } else {
                option.style.display = 'none';
                this.resetHighlighting(option);
            }
        });

        // Sort and reorder matching items
        if (searchTerm && matchingItems.length > 0) {
            matchingItems.sort((a, b) => this.compareItemRelevance(a.data, b.data, searchTerm));
            matchingItems.forEach(item => {
                this.elements.dropdownContent.appendChild(item.element);
            });
        }

        this.updateNoResultsMessage(hasVisibleOptions, searchTerm);
        return matchingItems.length;
    }

    itemMatches(item, searchTerm) {
        const searchLower = searchTerm.toLowerCase();

        // Search in all configured properties
        return this.options.searchProperties.some(property => {
            const value = this.getNestedProperty(item, property);
            return value && value.toString().toLowerCase().includes(searchLower);
        });
    }

    getItemRelevance(item, searchTerm) {
        const display = this.getItemDisplay(item).toLowerCase();
        const searchLower = searchTerm.toLowerCase();

        if (display === searchLower) return 4; // Exact match
        if (display.startsWith(searchLower)) return 3; // Starts with
        if (display.includes(searchLower)) return 2; // Contains
        return 1; // Other match
    }

    compareItemRelevance(a, b, searchTerm) {
        const aRelevance = this.getItemRelevance(a, searchTerm);
        const bRelevance = this.getItemRelevance(b, searchTerm);

        if (aRelevance !== bRelevance) {
            return bRelevance - aRelevance; // Higher relevance first
        }

        // Same relevance, sort by length then alphabetically
        const aDisplay = this.getItemDisplay(a);
        const bDisplay = this.getItemDisplay(b);

        if (aRelevance === 3) { // Both start with search term
            return aDisplay.length - bDisplay.length; // Shorter first
        }

        return aDisplay.localeCompare(bDisplay); // Alphabetical
    }

    isItemSelected(item) {
        const itemValue = this.getItemValue(item);
        return this.selectedItems.some(selected =>
            this.getItemValue(selected) === itemValue
        );
    }

    getItemValue(item) {
        return this.getNestedProperty(item, this.options.valueProperty);
    }

    getItemDisplay(item) {
        return this.getNestedProperty(item, this.options.displayProperty);
    }

    getNestedProperty(obj, path) {
        return path.split('.').reduce((current, prop) => current && current[prop], obj);
    }

    defaultRenderOption(item) {
        return `<span>${this.getItemDisplay(item)}</span>`;
    }

    defaultRenderSelectedItem(item) {
        return `<span>${this.getItemDisplay(item)}</span>`;
    }

    highlightMatchingText(option, item, searchTerm) {
        if (!searchTerm) {
            this.resetHighlighting(option);
            return;
        }

        const displaySpan = option.querySelector('span:last-child');
        if (!displaySpan) return;

        const displayText = this.getItemDisplay(item);
        const searchLower = searchTerm.toLowerCase();
        const displayLower = displayText.toLowerCase();
        const matchIndex = displayLower.indexOf(searchLower);

        if (matchIndex >= 0) {
            const beforeMatch = displayText.substring(0, matchIndex);
            const matchText = displayText.substring(matchIndex, matchIndex + searchTerm.length);
            const afterMatch = displayText.substring(matchIndex + searchTerm.length);

            displaySpan.innerHTML = beforeMatch +
                '<span class="match-highlight">' + matchText + '</span>' +
                afterMatch;
        } else {
            displaySpan.textContent = displayText;
        }
    }

    resetHighlighting(option) {
        const displaySpan = option.querySelector('span:last-child');
        if (displaySpan) {
            const itemData = JSON.parse(option.getAttribute('data-item-data'));
            displaySpan.textContent = this.getItemDisplay(itemData);
        }
    }

    updateHighlight(visibleOptions) {
        visibleOptions.forEach(option => option.classList.remove('highlighted'));
        if (this.highlightedIndex >= 0 && visibleOptions[this.highlightedIndex]) {
            visibleOptions[this.highlightedIndex].classList.add('highlighted');
        }
    }

    updateNoResultsMessage(hasVisibleOptions, searchTerm) {
        let noResultsMsg = this.elements.dropdown.querySelector('.no-results-found');

        if (!hasVisibleOptions && searchTerm) {
            if (!noResultsMsg) {
                noResultsMsg = document.createElement('div');
                noResultsMsg.className = 'no-results-found';
                noResultsMsg.textContent = 'No results found';
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
        this.elements.dropdown.querySelectorAll(`.${this.options.cssClasses.option}.highlighted`).forEach(option => {
            option.classList.remove('highlighted');
        });
        this.elements.dropdown.querySelectorAll(`.${this.options.cssClasses.option}`).forEach(option => {
            this.resetHighlighting(option);
        });
        this.highlightedIndex = -1;

        this.elements.searchInput.value = '';
        if (this.elements.searchSuggestion) {
            this.elements.searchSuggestion.innerHTML = '';
        }
        this.currentSuggestion = '';
    }

    selectItem(item) {
        if (!this.options.multiSelect) {
            // Single select - replace current selection
            this.selectedItems = [item];
        } else {
            // Multi select - add to selection
            if (!this.options.allowDuplicates && this.isItemSelected(item)) {
                return;
            }
            this.selectedItems.push(item);
        }

        this.renderSelectedItems();
        this.updateHiddenInput();
        this.hideDropdown();

        // Call callbacks
        this.options.onItemSelected(item);
        this.options.onSelectionChanged(this.selectedItems);
    }

    removeItem(item) {
        const itemValue = this.getItemValue(item);
        this.selectedItems = this.selectedItems.filter(selected =>
            this.getItemValue(selected) !== itemValue
        );

        this.renderSelectedItems();
        this.updateHiddenInput();

        // Call callbacks
        this.options.onItemRemoved(item);
        this.options.onSelectionChanged(this.selectedItems);
    }

    renderSelectedItems() {
        if (!this.options.multiSelect || !this.elements.selectedItemsContainer) return;

        this.elements.selectedItemsContainer.innerHTML = '';
        this.selectedItems.forEach(item => {
            const itemElement = document.createElement('span');
            itemElement.className = this.options.cssClasses.selectedItem;
            itemElement.innerHTML = `
                ${this.options.renderSelectedItem(item)}
                <span class="item-remove" data-remove>&times;</span>
            `;

            // Add click handler for remove button
            itemElement.querySelector('[data-remove]').addEventListener('click', (e) => {
                e.stopPropagation();
                this.removeItem(item);
            });

            this.elements.selectedItemsContainer.appendChild(itemElement);
        });
    }

    updateHiddenInput() {
        const values = this.selectedItems.map(item => this.getItemValue(item));
        this.elements.hiddenInput.value = this.options.multiSelect ? values.join(', ') : values[0] || '';
    }

    // Public API methods
    getSelectedItems() {
        return [...this.selectedItems];
    }

    setSelectedItems(items) {
        this.selectedItems = [...items];
        this.renderSelectedItems();
        this.updateHiddenInput();
    }

    addItem(item) {
        this.selectItem(item);
    }

    clear() {
        this.selectedItems = [];
        this.renderSelectedItems();
        this.updateHiddenInput();
        this.options.onSelectionChanged(this.selectedItems);
    }

    updateData(newData) {
        this.options.data = newData;
        this.elements.dropdownContent.innerHTML = newData.map(item => this.createOptionHTML(item)).join('');
    }

    destroy() {
        clearTimeout(this.searchTimeout);
        // Additional cleanup could be added here
    }
}