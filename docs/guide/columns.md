# Columns

Columns are declared with `DataTableHeader<T>`. One instance per column, in display order.

```kotlin
DataTableHeader<Person>(
    key = "salary",                          // unique column identifier
    title = "Salary",                        // header label
    value = { "$${it.salary}" },             // value extractor for display
    width = 120.dp,                          // fixed width (null = fill with weight)
    align = TextAlign.End,                   // text alignment
    sortable = true,                         // enable sorting
    fixed = false,                           // true = frozen/pinned column
    visible = true,                          // toggle visibility
    maxLines = 1,                            // text line limit
    overflow = TextOverflow.Ellipsis,        // overflow strategy
    comparator = compareBy { it.salary },    // custom sort comparator
    cellContent = { item ->                  // custom cell composable
        Text("$${String.format("%.2f", item.salary)}")
    },
    headerContent = {                        // custom header composable
        Text("Salary (USD)", fontWeight = FontWeight.Bold)
    },
)
```

`key` must be unique — it identifies the column for sorting and for resize overrides.

## Width

A column is either **fixed** or **weighted**:

| `width` | Behaviour |
|---------|-----------|
| `120.dp` | Exactly that wide. |
| `null` (default) | Takes an equal share of the leftover space. |

Mixing both in one table is fine. Mixing them *under a single grouped header* is not — see
[Nested headers](#nested-grouped-headers).

## Text overflow

The defaults (`maxLines = Int.MAX_VALUE`, `overflow = TextOverflow.Clip`) let text wrap, which
gives rows of differing heights. Most tables want the opposite:

```kotlin
DataTableHeader(
    key = "notes",
    title = "Notes",
    value = { it.notes },
    width = 300.dp,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)
```

## Custom content

`cellContent` and `headerContent` replace the default text rendering entirely. This is where the
table stops caring that it has no Material dependency — put whatever you like in a cell.

```kotlin
DataTableHeader(
    key = "status",
    title = "Status",
    width = 150.dp,
    cellContent = { item ->
        AssistChip(
            onClick = {},
            label = { Text(item.status) },
        )
    },
)
```

!!! tip "Hoist your header list"
    `DataTableHeader` is `@Immutable`, but a list rebuilt inline on every recomposition is still a
    new list. Wrap it in `remember` — keyed on whatever it depends on — so the table can skip
    re-sorting:

    ```kotlin
    val headers = remember(showNotesColumn) { listOf(/* ... */) }
    ```

## Visibility

`visible = false` removes a column from rendering without disturbing the rest of the list:

```kotlin
var showNotes by remember { mutableStateOf(true) }

val headers = remember(showNotes) {
    listOf(
        // ...
        DataTableHeader(key = "notes", title = "Notes", value = { it.notes }, visible = showNotes),
    )
}
```

## Frozen (pinned) columns

`fixed = true` pins a column to the left edge so it does not scroll horizontally.

```kotlin
DataTableHeader<Person>(
    key = "id",
    title = "ID",
    value = { it.id },
    width = 60.dp,
    fixed = true,
)
```

!!! danger "Frozen columns need an explicit width"
    The frozen section sits outside the horizontally scrolling area, so a weighted column there
    has nothing to take a share of. `fixed = true` without a `width` throws, naming the column.

## Resizable columns

```kotlin
DataTable(
    // ...
    resizableColumns = true,
    minColumnWidth = 50.dp,
)
```

Drag the right edge of any header to resize. Overrides live on `DataTableState`; call
`state.resetColumnWidths()` to clear them.

Grouped headers resize with their children — a group's width is recomputed from its leaves, so
the band stays aligned.

## Nested (grouped) headers

Give a header `children` and it becomes a **group**: a band drawn above its children, spanning
them. Only the leaves render as real columns — sorting, resizing, and cell content all belong to
them.

```kotlin
val headers = listOf(
    DataTableHeader<Person>(key = "id", title = "ID", value = { it.id }, width = 60.dp),
    DataTableHeader(
        key = "contact",
        title = "Contact",
        children = listOf(
            DataTableHeader(key = "email", title = "Email", value = { it.email }, width = 200.dp),
            DataTableHeader(key = "phone", title = "Phone", value = { it.phone }, width = 140.dp),
        ),
    ),
)
```

```
┌──────┬───────────────────────────────┐
│      │            Contact            │
│  ID  ├───────────────┬───────────────┤
│      │     Email     │     Phone     │
├──────┼───────────────┼───────────────┤
```

Groups nest to any depth. A leaf sitting beside a group — `ID` above — stretches over the full
header height and centres its title, rather than leaving blank rows above it.

A group's own `value`, `width`, and sorting settings are ignored. It takes its width from the
leaves beneath it.

!!! danger "Two rules, both enforced"
    - Leaves under one group must be **either all fixed-width or all weighted**. A group sums its
      leaves, and the two sizing models cannot be added together.
    - A group cannot **straddle the freeze boundary** — mark every column under it `fixed = true`,
      or none of them. Half a group would scroll out from under the other half.

    Both throw with a message naming the group, rather than misrendering.
