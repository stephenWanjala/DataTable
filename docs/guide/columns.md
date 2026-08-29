# Columns

Columns are declared with `DataTableHeader<T>`. One instance per column, in display order.

```kotlin
DataTableHeader<Person>(
    key = "salary",                          // unique column identifier
    title = "Salary",                        // header label
    value = { it.salary },                   // value extractor — raw, not formatted
    format = DataTableFormatters.currency(), // how that value is displayed
    width = 120.dp,                          // fixed width (null = fill with weight)
    align = TextAlign.End,                   // text alignment
    sortable = true,                         // enable sorting
    fixed = false,                           // true = frozen/pinned column
    visible = true,                          // toggle visibility
    maxLines = 1,                            // text line limit
    overflow = TextOverflow.Ellipsis,        // overflow strategy
    comparator = compareBy { it.salary },    // custom sort comparator
    cellContent = { item ->                  // custom cell composable, replacing `format`
        Text(item.salary.toString(), fontWeight = FontWeight.Bold)
    },
    headerContent = {                        // custom header composable
        Text("Salary (USD)", fontWeight = FontWeight.Bold)
    },
    filterable = true,                       // give it a field in the filter row
    filterPlaceholder = "Min",               // hint shown while the field is empty
    filterPredicate = { item, query ->       // default is a case-insensitive contains
        query.toDoubleOrNull()?.let { item.salary >= it } ?: true
    },
    editable = true,                         // edit this column in place
    editValue = { it.salary.toString() },    // raw text to seed the editor with
    validateEdit = { _, text ->              // null accepts, a message rejects
        if (text.toDoubleOrNull() == null) "Enter a number" else null
    },
)
```

`key` must be unique — it identifies the column for sorting, for filtering, and for resize
overrides.

## Width

A column is either **fixed** or **weighted**:

| `width` | Behaviour |
|---------|-----------|
| `120.dp` | Exactly that wide. |
| `null` (default) | Takes an equal share of the space the fixed columns leave. |

Weighted columns divide up the viewport minus the fixed columns, the selection and expand
controls, and any frozen section. When the fixed columns already overflow the viewport there is
nothing left to share, so each weighted column falls back to `minColumnWidth` and the table
scrolls.

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

## Formatting

`format` turns the extracted value into the text the cell shows. It is the whole of the
difference between a column that displays money and a column that *holds* money:

```kotlin
DataTableHeader(
    key = "salary",
    title = "Salary",
    value = { it.salary },                   // stays a Double
    format = DataTableFormatters.currency(), // reads "$92,000.00"
    align = TextAlign.End,
)
```

Formatting inside `value` instead — `value = { "$${it.salary}" }` — is the tempting shortcut, and
it quietly costs you three things. The column sorts as text, so `$1,000` lands between `$100` and
`$2`, and you need a `comparator` to undo it. An editable column opens its editor onto
`$92,000`, and you need an `editValue` to undo *that*. And anything else that reads the column —
validation, your own `onCopy` exporter — gets a string where a number was.

`format` keeps the value raw and applies only where text is shown:

| Reads the **raw** value | Reads the **formatted** text |
|---|---|
| Sorting and `comparator` | The rendered cell |
| `editValue`, `validateEdit`, and the `oldText` of a reported edit | Clipboard copy, including `selection.cells` |

Copy taking the formatted text is deliberate: what you see is what you paste, the way a
spreadsheet behaves. A handler that wants the underlying numbers still has them — `onCopy` hands
you `columns`, each with its own `value` extractor, and `rows` to run it against.

### Built-in formatters

`DataTableFormatters` covers the common cases:

```kotlin
DataTableFormatters.currency()                          // $1,299.00
DataTableFormatters.currency(Currency.getInstance("KES"))
DataTableFormatters.number(decimals = 2)                // 1,234.57
DataTableFormatters.number(grouping = false)            // 100234, for numeric ids
DataTableFormatters.percent(decimals = 1)               // 0.1234 -> 12.3%
DataTableFormatters.percent(fraction = false)           // 15.0   -> 15%
DataTableFormatters.date("dd MMM yyyy")                 // 07 Mar 2026
DataTableFormatters.boolean("Active", "Inactive")
```

Each takes a `locale` (and `date` a `zone`), defaulting to the machine's. Each is **total**: a
null value renders as its `nullText` — empty unless you set it — and a value of a type it cannot
handle falls back to `toString()` rather than throwing. A formatter runs during layout, once per
visible cell, so an exception there would take the table down with it.

They hold their locale and number format, so build one once rather than per recomposition:

```kotlin
private val money = DataTableFormatters.currency(locale = Locale.US)
// or, inside a composable:
val money = remember { DataTableFormatters.currency() }
```

Anything else is a lambda:

```kotlin
format = { value -> (value as? Int)?.let { "$it kg" } ?: "—" }
```

!!! note "`format` sees the value, not the row"
    It is `(Any?) -> String` — enough for the value in front of it, and nothing else. When the
    text depends on the rest of the row, that is what `cellContent` is for.

A column with `cellContent` draws itself and ignores `format` on screen, but copy still uses it —
which is how an icon column copies as `Active` rather than `true`.

## Filtering

`filterable` puts a field under the column's header, and the table filters on what is typed. It
has a guide of its own — see [Filtering](filtering.md) for predicates, custom controls, hoisted
filter state, and `manualFiltering`.

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

## Editing

`editable = true` lets a column be edited in place. `editValue` seeds the editor with the raw
value behind a formatted display, `validateEdit` rejects values before they are reported, and
`editorContent` replaces the built-in text field with a picker of your own.

Declaring any editable column also turns on cell-level focus and grid keyboard navigation, since
an editor you cannot reach is no use. See [Cell Editing](editing.md) for the whole feature.

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

<table class="header-demo">
  <colgroup>
    <col style="width: 15%">
    <col style="width: 50%">
    <col style="width: 35%">
  </colgroup>
  <thead>
    <tr>
      <th rowspan="2">ID</th>
      <th class="header-demo-group" colspan="2">Contact</th>
    </tr>
    <tr>
      <th>Email</th>
      <th>Phone</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>1</td>
      <td>ada@example.com</td>
      <td>+1 555 0100</td>
    </tr>
    <tr>
      <td>2</td>
      <td>grace@example.com</td>
      <td>+1 555 0101</td>
    </tr>
  </tbody>
</table>

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
