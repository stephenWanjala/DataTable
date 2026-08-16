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
| Kotlin | 2.x |
| Compose Multiplatform | 1.9+ |
| JVM | 21+ |

!!! note "Desktop only, for now"
    The library targets Compose Desktop. It uses AWT cursors for the column-resize handle, so it
    is not yet a Compose Multiplatform artifact.

## Try it

The repository ships a gallery app that exercises every feature — nested headers, server-side
paging, all three densities, keyboard navigation, loading and empty states:

```bash
./gradlew :composeApp:run
```

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
