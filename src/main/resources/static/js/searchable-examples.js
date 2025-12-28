/**
 * SearchableSelector Usage Examples
 * Include this file to see practical implementations for different data types
 */

// Initialize searchable selectors for different data types
window.SearchableSelectorExamples = {

    /**
     * Tag Selector Example
     */
    initTagSelector: function(containerSelector, tags, options = {}) {
        return new SearchableSelector({
            container: containerSelector,
            data: tags,
            searchProperty: 'name',
            displayProperty: 'name',
            valueProperty: 'name',
            placeholder: 'Search tags...',
            renderOption: (tag) => `
                <span class="color-indicator" style="background-color: ${tag.color || '#6c757d'}"></span>
                <span>${tag.name}</span>
            `,
            renderSelectedItem: (tag) => `<span>${tag.name}</span>`,
            ...options
        });
    },

    /**
     * Note Selector Example
     */
    initNoteSelector: function(containerSelector, notes, options = {}) {
        return new SearchableSelector({
            container: containerSelector,
            data: notes,
            searchProperties: ['title', 'content'],
            displayProperty: 'title',
            valueProperty: 'id',
            placeholder: 'Search notes...',
            renderOption: (note) => `
                <div>
                    <strong>${note.title}</strong>
                    <br>
                    <small class="text-muted">
                        ${note.content ? note.content.substring(0, 60) + '...' : 'No content'}
                    </small>
                </div>
            `,
            renderSelectedItem: (note) => `<span>${note.title}</span>`,
            ...options
        });
    },

    /**
     * Group Selector Example
     */
    initGroupSelector: function(containerSelector, groups, options = {}) {
        return new SearchableSelector({
            container: containerSelector,
            data: groups,
            searchProperty: 'name',
            displayProperty: 'name',
            valueProperty: 'id',
            placeholder: 'Search groups...',
            renderOption: (group) => `
                <span class="icon-indicator">
                    <i class="${group.icon || 'fas fa-folder'}" style="color: ${group.color || '#6c757d'}"></i>
                </span>
                <span>${group.name}</span>
                ${group.description ? `<small class="text-muted ms-auto">${group.description}</small>` : ''}
            `,
            renderSelectedItem: (group) => `
                <i class="${group.icon || 'fas fa-folder'}" style="color: ${group.color || '#6c757d'}"></i>
                <span>${group.name}</span>
            `,
            ...options
        });
    },

    /**
     * User Selector Example
     */
    initUserSelector: function(containerSelector, users, options = {}) {
        return new SearchableSelector({
            container: containerSelector,
            data: users,
            searchProperties: ['name', 'email', 'username'],
            displayProperty: 'name',
            valueProperty: 'id',
            placeholder: 'Search users...',
            renderOption: (user) => `
                <div class="d-flex align-items-center">
                    <div class="me-2">
                        ${user.avatar ?
                            `<img src="${user.avatar}" class="rounded-circle" width="24" height="24">` :
                            `<div class="bg-secondary rounded-circle d-flex align-items-center justify-content-center" style="width: 24px; height: 24px; font-size: 0.7rem; color: white;">${user.name.charAt(0).toUpperCase()}</div>`
                        }
                    </div>
                    <div>
                        <strong>${user.name}</strong>
                        <br>
                        <small class="text-muted">${user.email}</small>
                    </div>
                </div>
            `,
            renderSelectedItem: (user) => `<span>${user.name}</span>`,
            ...options
        });
    },

    /**
     * Generic Multi-Property Selector
     */
    initGenericSelector: function(containerSelector, data, config) {
        return new SearchableSelector({
            container: containerSelector,
            data: data,
            searchProperties: config.searchProperties || ['name'],
            displayProperty: config.displayProperty || 'name',
            valueProperty: config.valueProperty || 'id',
            placeholder: config.placeholder || 'Search...',
            renderOption: config.renderOption || ((item) => `<span>${item[config.displayProperty || 'name']}</span>`),
            renderSelectedItem: config.renderSelectedItem || ((item) => `<span>${item[config.displayProperty || 'name']}</span>`),
            ...config.options
        });
    }
};

/**
 * Quick initialization functions for common use cases
 */

// Quick tag selector
window.createTagSelector = function(containerSelector, tags, callbacks = {}) {
    return SearchableSelectorExamples.initTagSelector(containerSelector, tags, {
        onSelectionChanged: callbacks.onSelectionChanged || (() => {}),
        onItemSelected: callbacks.onTagSelected || (() => {}),
        onItemRemoved: callbacks.onTagRemoved || (() => {})
    });
};

// Quick note selector
window.createNoteSelector = function(containerSelector, notes, callbacks = {}) {
    return SearchableSelectorExamples.initNoteSelector(containerSelector, notes, {
        onSelectionChanged: callbacks.onSelectionChanged || (() => {}),
        onItemSelected: callbacks.onNoteSelected || (() => {}),
        onItemRemoved: callbacks.onNoteRemoved || (() => {})
    });
};

// Quick group selector
window.createGroupSelector = function(containerSelector, groups, callbacks = {}) {
    return SearchableSelectorExamples.initGroupSelector(containerSelector, groups, {
        multiSelect: false, // Groups typically single-select
        onSelectionChanged: callbacks.onSelectionChanged || (() => {}),
        onItemSelected: callbacks.onGroupSelected || (() => {}),
        onItemRemoved: callbacks.onGroupRemoved || (() => {})
    });
};