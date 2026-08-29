# Compose DataTable

A highly customizable, feature-rich `DataTable` component for Compose Desktop built entirely on Foundation APIs -- no Material dependency required.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.stephenwanjala/datatable)](https://central.sonatype.com/artifact/io.github.stephenwanjala/datatable)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**[Documentation](https://stephenwanjala.github.io/DataTable/latest/)** ·
[Getting Started](https://stephenwanjala.github.io/DataTable/latest/getting-started/) ·
[API Reference](https://stephenwanjala.github.io/DataTable/latest/api/) ·
[Migration](https://stephenwanjala.github.io/DataTable/latest/migration/)

## Demo

### Large Dataset with Sorting, Selection & Pagination

![Large dataset demo](doc/large_dataset_sample.gif)

### Loading State

![Loading state example](doc/loading_example.gif)

### Empty State

![Empty state example](doc/empty_state_sample.png)

## Features

- **Column sorting** -- single-click header to sort, Ctrl+click for multi-column sort
- **Row selection** -- none, single, or multi-select, tracked by row key
- **Row expansion** -- expand rows to show custom detail content
- **Nested headers** -- group columns under a spanning header band, to any depth
- **Frozen/pinned columns** -- pin columns to the left edge so they don't scroll horizontally
- **Column resizing** -- drag column edges to resize
- **Pagination** -- configurable page size with items-per-page selector
- **Server-side data** -- `manualSorting` / `manualPagination` hand sorting and paging to your database
- **Grouping** -- group rows by a key with custom group header and summary rows
- **Keyboard navigation** -- Arrow keys, Enter, Space, Home, End; opt into `cellNavigation` for a cell cursor, Left/Right, Tab, and Page Up/Down
- **Column menu** -- `showColumnMenuButton` puts a show/hide menu in the header, like JavaFX's table menu button
- **Column reordering** -- `reorderableColumns` lets a user drag a header to a new position, with groups moving as a block; `moveColumn` and `columnOrder` drive it programmatically
- **Saved layouts** -- `captureLayout()` / `applyLayout()` snapshot column widths, hidden columns, order, sort, and filters, with an encoded string to store per user
- **Filter row** -- mark a column `filterable` for a field under its header, with a custom `filterPredicate` or `filterContent`; `manualFiltering` hands the whole thing to your query
- **Column formatting** -- a per-column `format` decides what a cell reads (money, percentages, dates, booleans, or your own), while the value stays raw for sorting and editing
- **Cell editing** -- editable columns with per-column validation, an editor that opens on the raw value behind a formatted display, and custom `editorContent` editors
- **Range selection & clipboard** -- Shift+arrows or Shift+click select a block of cells, Ctrl+C copies it as tab-separated text; `onCopy` takes the copy over and hands you the rows and columns as your own types
- **Row hover & alternating colors** -- visual row highlighting
- **Right-click context menu** -- callback with item and position
- **Text overflow** -- per-column `maxLines` and `TextOverflow` control
- **Custom cell content** -- full composable control over any cell or header
- **Fully themeable** -- customize all colors and text styles without any theming framework
- **Zero Material dependency** -- built on Compose Foundation only

## Not included

So you can rule it in or out quickly: a block of cells can be copied but not pasted into or edited
as a block, cell selection is a single rectangle, and editing has no row-level commit and no undo.
Filtering is one query per column, ANDed, with no OR across columns and no built-in checklist of
the values that occur. Columns can be reordered by dragging their headers, but only by dragging —
there is no keyboard equivalent. There is also no export, no tree tables, and no accessibility
semantics. Frozen columns pin left only. The
[documentation](https://stephenwanjala.github.io/DataTable/latest/#what-it-does-not-do) spells each of
these out.

## Installation

```kotlin
dependencies {
    implementation("io.github.stephenwanjala:datatable:0.4.0")
}
```

## Quick Start

```kotlin
data class Person(val id: Int, val name: String, val age: Int, val email: String)

val headers = listOf(
    DataTableHeader<Person>(key = "id", title = "ID", value = { it.id }, width = 60.dp),
    DataTableHeader(key = "name", title = "Name", value = { it.name }, width = 150.dp),
    DataTableHeader(key = "age", title = "Age", value = { it.age }, width = 80.dp),
    DataTableHeader(key = "email", title = "Email", value = { it.email }, width = 250.dp),
)

DataTable(
    items = people,
    headers = headers,
    itemKey = { it.id },
)
```

`itemKey` must be unique across `items` -- it backs row identity while scrolling as well as
selection and expansion.

See the [documentation](https://stephenwanjala.github.io/DataTable/latest/) for the full guide.

## Sample Gallery

The repository ships a gallery app exercising every feature -- nested headers, server-side paging,
all three densities, keyboard navigation, cell editing, range selection and copy, loading and
empty states:

```bash
./gradlew :composeApp:run
```

Releases also carry prebuilt demo packages -- `.deb` and `.tar.gz` for Linux, `.msi` for Windows,
`.dmg` for macOS -- on the [Releases page](https://github.com/stephenWanjala/DataTable/releases).

## Building the Docs

```bash
# API reference -> build/dokka
./gradlew :DataTable:dokkaGeneratePublicationHtml

# Docs toolchain, in a venv (many distros ship an externally-managed Python)
python3 -m venv .venv
.venv/bin/pip install -r requirements-docs.txt

# Mount the API reference, then serve on http://127.0.0.1:8000
cp -r build/dokka docs/api
.venv/bin/mkdocs serve
```

## Requirements

- Kotlin 2.2+
- Compose Multiplatform 1.11.1 (exposed as an `api` dependency)
- JVM 11+ — the artifact is compiled to Java 11 bytecode

Building this repository needs JDK 21.

## License

```
Copyright 2025 Wanjala Stephen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
