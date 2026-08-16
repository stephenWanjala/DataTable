# Compose DataTable

A highly customizable, feature-rich `DataTable` component for Compose Desktop built entirely on Foundation APIs -- no Material dependency required.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.stephenwanjala/datatable)](https://central.sonatype.com/artifact/io.github.stephenwanjala/datatable)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

**[Documentation](https://stephenwanjala.github.io/DataTable/)** ·
[Getting Started](https://stephenwanjala.github.io/DataTable/getting-started/) ·
[API Reference](https://stephenwanjala.github.io/DataTable/api/) ·
[Migration](https://stephenwanjala.github.io/DataTable/migration/)

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
- **Keyboard navigation** -- Arrow keys, Enter, Space, Home, End
- **Row hover & alternating colors** -- visual row highlighting
- **Right-click context menu** -- callback with item and position
- **Text overflow** -- per-column `maxLines` and `TextOverflow` control
- **Custom cell content** -- full composable control over any cell or header
- **Fully themeable** -- customize all colors and text styles without any theming framework
- **Zero Material dependency** -- built on Compose Foundation only

## Installation

```kotlin
dependencies {
    implementation("io.github.stephenwanjala:datatable:0.3.0")
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

See the [documentation](https://stephenwanjala.github.io/DataTable/) for the full guide.

## Sample Gallery

The repository ships a gallery app exercising every feature -- nested headers, server-side paging,
all three densities, keyboard navigation, loading and empty states:

```bash
./gradlew :composeApp:run
```

## Building the Docs

```bash
./gradlew :DataTable:dokkaGeneratePublicationHtml   # API reference -> build/dokka
pip install mkdocs-material                          # once
cp -r build/dokka docs/api
mkdocs serve                                         # http://127.0.0.1:8000
```

## Requirements

- Kotlin 2.x+
- Compose Multiplatform 1.9+
- JVM 21+

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
