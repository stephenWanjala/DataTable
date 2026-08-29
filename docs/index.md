# Compose DataTable

A highly customizable, feature-rich `DataTable` component for **Compose Desktop**, built entirely
on Foundation APIs — no Material dependency required.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.stephenwanjala/datatable)](https://central.sonatype.com/artifact/io.github.stephenwanjala/datatable)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

```kotlin
DataTable(
    items = people,
    headers = headers,
    itemKey = { it.id },
)
```

[Get started](getting-started.md){ .md-button .md-button--primary }
[API reference](api/index.html){ .md-button }

## What it does

<div class="grid cards" markdown>

-   **Columns that behave**

    Fixed or weighted widths, per-column alignment, ellipsis, visibility toggles, custom cell and
    header composables, drag-to-resize, left-pinned frozen columns, and grouped headers nested to
    any depth.

-   **A filter row that pairs with your query**

    Mark a column `filterable` and it gets a field under its header; give it a `filterPredicate`
    for a range or an exact match, or a `filterContent` for a dropdown. Filtering is a
    `Map<String, String>` you can hoist, save, or turn into a `WHERE` clause with
    `manualFiltering`.

-   **Formatted for reading, raw underneath**

    A `format` lambda per column — money, percentages, dates, booleans, or your own — decides
    what a cell reads, while the value stays raw for sorting, editing, and validation. Copy takes
    the formatted text, the way a spreadsheet does.

-   **Sorting that scales**

    Single-column, multi-column with ++ctrl++ +click, custom comparators — or hand sorting off to
    your database with `manualSorting` and translate header clicks into `ORDER BY`.

-   **Selection by key, not by identity**

    Selection and expansion are tracked by the key from `itemKey`, so they survive item instances
    being replaced and never depend on your row type implementing `equals`/`hashCode`.

-   **Built for large data**

    Virtualized rows, client-side or server-side pagination, and `manualPagination` so the table
    holds one page while your query holds the rest.

-   **Keyboard driven**

    Arrow keys, ++home++/++end++, ++enter++, ++space++. Focus is tracked by key, so it follows its
    row across a re-sort instead of sticking to a position. Turn on `cellNavigation` and the same
    holds a cell cursor across two axes.

-   **Editing in place**

    Mark a column `editable` and cells edit on ++enter++, ++f2++, a double-click, or just by
    typing. Per-column validation, an editor that opens on the raw value behind a formatted
    display, and `editorContent` for pickers. The table reports the edit; your model stays the
    only copy.

-   **Select a block, copy it out**

    ++shift++ +arrows or ++shift++ +click extend a rectangle of cells; ++ctrl+c++ puts it on the
    clipboard as tab-separated text. Intercept it with `onCopy` and you get the rows and columns
    back as your own types — the seam an exporter hangs off.

-   **Themeable without a theme**

    Every color and text style is a parameter. Three densities. Works with Material, or with
    nothing at all.

</div>

## Requirements

| | |
|---|---|
| Kotlin | 2.2+ |
| Compose Multiplatform | 1.11.1 |
| JVM | 11+ |

The artifact is compiled to Java 11 bytecode, so it runs on any JVM from 11 up. It is built and
tested against Compose Multiplatform 1.11.1, which it exposes as an `api` dependency — Gradle will
resolve your Compose version up to that if you are on an older one. Kotlin metadata is emitted at
language level 2.2, so a 2.2 compiler can read it.

!!! note "Desktop only, for now"
    The library targets Compose Desktop. It uses AWT cursors for the column-resize handle, so it
    is not yet a Compose Multiplatform artifact.

## Try it

The repository ships a gallery app that exercises every feature — nested headers, server-side
paging, all three densities, keyboard navigation, cell editing, range selection and copy, loading
and empty states:

```bash
./gradlew :composeApp:run
```

Each release also carries prebuilt demo packages — `.deb` and `.tar.gz` for Linux, `.msi` for
Windows, `.dmg` for macOS — on the
[Releases page](https://github.com/stephenWanjala/DataTable/releases).

## What it does not do

The honest boundary of the component as it stands, so you can rule it in or out before building on
it:

- **No paste.** A block of cells can be selected and copied, but not filled from the clipboard,
  so a round-trip through a spreadsheet is one-way.
- **One block at a time.** Cell selection is a single rectangle — there is no ++ctrl++ +click to
  build up several disjoint blocks.
- **No row-level commit.** Editing commits one cell at a time — there is no editing row that
  validates across columns or rolls back as a unit, and no undo stack.
- **No export.** CSV, Excel, and PDF are the caller's job — though `onCopy` hands you the
  selected rows and columns as a starting point.
- **No column reordering by dragging**, and no built-in column chooser — `visible` is a flag you
  drive from your own UI.
- **No layout persistence.** Column widths, sort, cell focus, and scroll live only as long as the
  composition. See [Interactions](guide/interactions.md#column-widths).
- **Filtering is one query per column, ANDed.** The filter row holds a string per column and
  every column has to match. There is no OR across columns, no filter that spans two of them, and
  no Excel-style checklist of the values that occur — a custom `filterContent` is where those get
  built.
- **No tree tables.** `groupBy` is one level deep, flat, and always expanded — see
  [Grouping](guide/grouping.md#limitations).
- **No accessibility semantics.** Rows and cells carry no roles or content descriptions, so screen
  reader support is whatever Compose infers from the text.
- **Frozen columns pin left only**, and rows cannot be frozen at all.

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
