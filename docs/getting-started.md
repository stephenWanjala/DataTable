# Getting Started

## Installation

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
        implementation("io.github.stephenwanjala:datatable:0.5.0")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
        implementation 'io.github.stephenwanjala:datatable:0.5.0'
    }
    ```

=== "Version catalog"

    ```toml
    [libraries]
    datatable = { module = "io.github.stephenwanjala:datatable", version = "0.5.0" }
    ```

Compose is exposed as an `api` dependency, so `compose.runtime`, `compose.foundation`, and
`compose.ui` arrive transitively — you do not need to declare them again for the table's types.

Upgrading from an earlier version? See [Migration](migration.md).

## Your first table

```kotlin
data class Person(val id: Int, val name: String, val age: Int, val email: String)

val people = listOf(
    Person(1, "Alice Smith", 30, "alice@example.com"),
    Person(2, "Bob Johnson", 25, "bob@example.com"),
    Person(3, "Charlie Brown", 35, "charlie@example.com"),
)

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

That gives you a scrolling, sortable table. Clicking a header sorts by that column — no wiring
required, because with no `onSortChange` supplied the table owns its own sort state.

## The three required parameters

`items`

:   The rows to display. Under [server-side paging](guide/sorting-and-paging.md#server-side-data)
    this is just the current page.

`headers`

:   The column definitions. See [Columns](guide/columns.md).

`itemKey`

:   A stable, unique key per row.

## itemKey matters more than it looks

!!! warning "Keys must be unique across `items`"
    `itemKey` is the row's identity in three separate places: `LazyColumn` item identity while
    scrolling, membership in `selectedKeys`, and membership in `expandedKeys`. Two rows sharing a
    key will **select, expand, and recycle as a single row**.

Use a database id or something equally stable:

```kotlin
itemKey = { it.id }
```

Avoid deriving it from mutable content — `itemKey = { it.name }` breaks the moment two people
share a name, or one of them gets renamed.

## Hoisting state

Every interactive feature follows the same pattern: pass the current value, pass a callback,
store the result. Supplying the callback makes the table **controlled** for that feature.

```kotlin
var selectedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }
var sort by remember { mutableStateOf(SortState()) }

DataTable(
    items = people,
    headers = headers,
    itemKey = { it.id },

    showSelect = true,
    selectedKeys = selectedKeys,
    onSelectionChange = { selectedKeys = it },

    sortBy = sort,
    onSortChange = { sort = it },
)
```

Leave a callback out and the table manages that piece itself. Mixing is fine — controlled
sorting alongside uncontrolled pagination works exactly as you would expect.

## Where to next

- [Columns](guide/columns.md) — widths, alignment, custom cells, frozen and nested headers
- [Selection & Expansion](guide/selection.md) — the key-based model
- [Sorting & Paging](guide/sorting-and-paging.md) — including server-side data
- [Grouping](guide/grouping.md) — group headers and summary rows
- [Interactions](guide/interactions.md) — clicks, context menus, keyboard, `DataTableState`
- [Cell Editing](guide/editing.md) — editable columns, validation, custom editors
- [Theming](guide/theming.md) — colors, text styles, density, empty and loading states
