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
    row across a re-sort instead of sticking to a position.

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
paging, all three densities, keyboard navigation, loading and empty states:

```bash
./gradlew :composeApp:run
```

Each release also carries prebuilt demo packages — `.deb` and `.tar.gz` for Linux, `.msi` for
Windows, `.dmg` for macOS — on the
[Releases page](https://github.com/stephenWanjala/DataTable/releases).

## What it does not do

The honest boundary of the component as it stands, so you can rule it in or out before building on
it:

- **No cell editing.** There is no edit mode, commit/cancel lifecycle, or per-cell validation. A
  `cellContent` composable can hold an editor, but you own all of its behaviour.
- **No cell-level selection.** Selection and keyboard focus are per row. Arrow keys move rows;
  there is no cell cursor, no ++shift++ +click range selection, and no ++shift++ +arrow extension.
- **No clipboard integration.** Copying a block of rows or cells is not built in.
- **No filtering UI.** Filter `items` yourself before handing them over; there is no filter row and
  no `manualFiltering` counterpart to `manualSorting`.
- **No export.** CSV, Excel, and PDF are the caller's job.
- **No column reordering by dragging**, and no built-in column chooser — `visible` is a flag you
  drive from your own UI.
- **No layout persistence.** Column widths, sort, focus, and scroll live only as long as the
  composition. See [Interactions](guide/interactions.md#column-widths).
- **No per-column formatter.** `value` is rendered with `toString()`; formatting means a
  `cellContent` composable, and sorting a formatted column means a `comparator`.
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
