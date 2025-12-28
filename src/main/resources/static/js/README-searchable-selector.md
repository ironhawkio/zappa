# SearchableSelector Utility

A reusable JavaScript utility for creating searchable dropdowns with autocomplete for any data type.

## Quick Start

### 1. Include Required Files

```html
<link rel="stylesheet" href="/css/searchable-selector.css">
<script src="/js/searchable-selector.js"></script>
<script src="/js/searchable-examples.js"></script>
```

### 2. Create HTML Container

```html
<div class="searchable-selector-wrapper tag-selector" style="min-width: 300px;"></div>
```

### 3. Initialize with Data

```javascript
// For Tags
const tags = [{name: 'javascript', color: '#f7df1e'}, {name: 'java', color: '#ed8b00'}];
const tagSelector = createTagSelector('.tag-selector', tags, {
    onSelectionChanged: (selectedTags) => console.log('Selected:', selectedTags)
});

// For Notes
const notes = [{id: '1', title: 'My Note', content: 'Note content'}];
const noteSelector = createNoteSelector('.note-selector', notes, {
    onSelectionChanged: (selectedNotes) => console.log('Selected:', selectedNotes)
});

// For Groups
const groups = [{id: '1', name: 'Work', icon: 'fas fa-briefcase', color: '#007bff'}];
const groupSelector = createGroupSelector('.group-selector', groups, {
    onSelectionChanged: (selectedGroups) => console.log('Selected:', selectedGroups)
});
```

## Advanced Usage

### Custom Data Type

```javascript
const customSelector = new SearchableSelector({
    container: '.custom-selector',
    data: yourData,
    searchProperties: ['title', 'description', 'tags'],
    displayProperty: 'title',
    valueProperty: 'id',
    placeholder: 'Search your data...',

    renderOption: (item) => `
        <div>
            <strong>${item.title}</strong>
            <br>
            <small class="text-muted">${item.description}</small>
        </div>
    `,

    renderSelectedItem: (item) => `
        <i class="${item.icon}"></i> ${item.title}
    `,

    onSelectionChanged: (selectedItems) => {
        console.log('Selection changed:', selectedItems);
    }
});
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `container` | string | required | CSS selector for container element |
| `data` | array | `[]` | Array of data items to search |
| `searchProperty` | string | `'name'` | Primary property to search in |
| `searchProperties` | array | `[searchProperty]` | Multiple properties to search |
| `displayProperty` | string | `'name'` | Property to display in UI |
| `valueProperty` | string | `'id'` | Property to use as unique value |
| `placeholder` | string | `'Search...'` | Input placeholder text |
| `multiSelect` | boolean | `true` | Allow multiple selections |
| `allowDuplicates` | boolean | `false` | Allow duplicate selections |
| `minSearchLength` | number | `0` | Minimum characters before search |
| `debounceMs` | number | `50` | Search debounce delay |
| `maxHeight` | string | `'200px'` | Max dropdown height |

### Callbacks

```javascript
{
    onSelectionChanged: (selectedItems) => {
        // Called when selection changes
        updateUI(selectedItems);
    },

    onItemSelected: (item) => {
        // Called when an item is selected
        console.log('Selected:', item);
    },

    onItemRemoved: (item) => {
        // Called when an item is removed
        console.log('Removed:', item);
    }
}
```

### Public Methods

```javascript
const selector = new SearchableSelector(options);

// Get current selection
const selected = selector.getSelectedItems();

// Set selection programmatically
selector.setSelectedItems([item1, item2]);

// Add item to selection
selector.addItem(newItem);

// Clear all selections
selector.clear();

// Update data
selector.updateData(newDataArray);

// Destroy instance
selector.destroy();
```

## CSS Customization

### Size Variants

```html
<!-- Compact -->
<div class="searchable-selector-wrapper compact"></div>

<!-- Normal (default) -->
<div class="searchable-selector-wrapper"></div>

<!-- Large -->
<div class="searchable-selector-wrapper large"></div>
```

### States

```html
<!-- Single select -->
<div class="searchable-selector-wrapper single-select"></div>

<!-- Read-only -->
<div class="searchable-selector-wrapper readonly"></div>

<!-- Disabled -->
<div class="searchable-selector-wrapper disabled"></div>

<!-- Error state -->
<div class="searchable-selector-wrapper error"></div>

<!-- Success state -->
<div class="searchable-selector-wrapper success"></div>
```

## Real-World Examples

### 1. Tag Filter for Graph

```javascript
// HTML
<div class="searchable-selector-wrapper tag-filter" style="min-width: 300px;"></div>

// JavaScript
const tagFilter = createTagSelector('.tag-filter', allTags, {
    onSelectionChanged: (selectedTags) => {
        applyTagFilter(selectedTags.map(tag => tag.name));
    }
});
```

### 2. Note Linking

```javascript
// HTML
<div class="searchable-selector-wrapper note-linker"></div>

// JavaScript
const noteLinker = createNoteSelector('.note-linker', availableNotes, {
    multiSelect: false,
    onItemSelected: (note) => {
        createLinkToNote(note.id);
    }
});
```

### 3. Group Assignment

```javascript
// HTML
<div class="searchable-selector-wrapper group-assign"></div>

// JavaScript
const groupAssigner = createGroupSelector('.group-assign', userGroups, {
    multiSelect: false,
    onItemSelected: (group) => {
        assignToGroup(group.id);
    }
});
```

## Integration with Existing Forms

```html
<form>
    <div class="mb-3">
        <label class="form-label">Select Tags:</label>
        <div class="searchable-selector-wrapper" id="tagSelector"></div>
    </div>

    <div class="mb-3">
        <label class="form-label">Link to Notes:</label>
        <div class="searchable-selector-wrapper" id="noteSelector"></div>
    </div>

    <button type="submit">Save</button>
</form>

<script>
document.addEventListener('DOMContentLoaded', function() {
    // Initialize selectors
    const tagSelector = createTagSelector('#tagSelector', tags);
    const noteSelector = createNoteSelector('#noteSelector', notes);

    // Form submission
    document.querySelector('form').addEventListener('submit', function(e) {
        const selectedTags = tagSelector.getSelectedItems();
        const selectedNotes = noteSelector.getSelectedItems();

        // Use selections in form data
        console.log('Form data:', { selectedTags, selectedNotes });
    });
});
</script>
```

This utility provides a consistent, reusable search experience across your entire application!