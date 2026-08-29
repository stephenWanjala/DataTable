# Filtering

Mark a column `filterable` and it gets a field under its header. That is the whole opt-in — the
filter row appears as soon as one column asks for it, and does not exist otherwise.

```kotlin
DataTableHeader(
    key = "name",
    title = "Name",
    value = { it.name },
    filterable = true,
    filterPlaceholder = "Contains…",
)
```

Filtering runs **before** sorting and paging, so everything downstream sees the rows that
survived: the page count, the footer's total, select-all, and keyboard navigation are all about
the filtered table.

## What the queries are

A filter is a `String` per column, held as a `Map<String, String>` keyed by `DataTableHeader.key`.
Columns are **ANDed** — a row has to satisfy every active filter, which is what a row of fields
reads as. A blank query filters nothing.

Keeping the value a plain string is what lets the whole filter set be lifted out of the table:
hoisted into your state, saved with a user's layout, or turned into a `WHERE` clause.

### The default match

Without a predicate of its own, a column matches on a case-insensitive **contains**, against both
the text the cell shows and the raw value. A salary column formatted as `$92,000` is found by
typing `92,0` *or* `92000` — the user does not have to know which one the column is really made
of.

### Per-column predicates

A contains match is wrong for anything but text. `filterPredicate` replaces it:

```kotlin
DataTableHeader(
    key = "salary",
    title = "Salary",
    value = { it.salary },
    format = DataTableFormatters.currency(),
    filterable = true,
    filterPlaceholder = "Min",
    // The query is a floor, not a substring.
    filterPredicate = { employee, query ->
        query.toDoubleOrNull()?.let { employee.salary >= it } ?: true
    },
)
```

Returning `true` for a query you cannot parse — as the half-typed `1` of `1000` will be — leaves
the table unfiltered instead of empty while the user is still typing.

## Custom filter controls

`filterContent` replaces the text field with a control of your own. It is handed a
`ColumnFilterController` carrying the current `query`, plus `setQuery` and `clear`. Supplying it
opts the column in on its own — there is no `filterable = true` to remember beside it.

```kotlin
DataTableHeader(
    key = "department",
    title = "Department",
    value = { it.department },
    filterContent = { controller ->
        Box {
            var expanded by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { expanded = true }) {
                Text(controller.query.ifEmpty { "All" })
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("All") }, onClick = { controller.clear() })
                departments.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { controller.setQuery(option) },
                    )
                }
            }
        }
    },
    filterPredicate = { employee, query -> employee.department == query },
)
```

A control whose state is not naturally one string encodes it into one — `">1000"`, `"IT|Finance"`,
`"2026-01-01..2026-03-31"` — and its `filterPredicate` decodes it again. The pair is where any
filter more elaborate than a text box gets built.

## Owning the filters yourself

Filtering is **controlled** the moment you supply `onFiltersChange`, exactly as sorting is with
`onSortChange`: the table renders the `filters` you pass and never changes them, so you have to
feed the new value back.

```kotlin
var filters by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

DataTable(
    items = employees,
    headers = headers,
    itemKey = { it.id },
    filters = filters,
    onFiltersChange = { filters = it },
)
```

That is what a "clear all" button, a saved layout, or a filter chip strip above the table hangs
off. Leave `onFiltersChange` out and the table keeps its own filters internally.

Filtering applies to any **visible** column with a query, whether or not it draws a field — so a
filter UI of your own can drive the table's matching without a filter row at all. Hiding a column
stops its filter applying, rather than leaving rows missing for a reason the user can no longer
see.

## Server-side filtering

`manualFiltering = true` tells the table that `items` has already been filtered. The filter row
still renders and still reports what the user types; the table just does not filter again.

```kotlin
LaunchedEffect(page, sort, filters) {
    rows = null
    // Restarting on each keystroke is the debounce: the previous delay is cancelled, so a burst
    // of typing costs one query rather than one per character.
    delay(350.milliseconds)
    total = repository.count(filters)
    rows = repository.page(page * pageSize, pageSize, sort, filters)
}

DataTable(
    items = rows.orEmpty(),
    headers = headers,
    itemKey = { it.id },

    manualFiltering = true,
    filters = filters,
    onFiltersChange = { filters = it; page = 0 },

    manualPagination = true,
    totalItems = total,        // the *filtered* count, or the pager lies
    currentPage = page,
    onPageChange = { page = it },
)
```

Two things to get right, and they are the same two in any grid that pages against a server:

!!! warning "`totalItems` has to be the filtered count"
    Under `manualPagination` the table cannot know how many rows matched. Pass the count of the
    filtered set, not the table's total, or the pager offers pages that come back empty.

!!! warning "Debounce before you query"
    `onFiltersChange` fires on **every keystroke**. Typing `Nairobi` is seven callbacks. A
    `LaunchedEffect` keyed on the filters, with a `delay` before the query, collapses them into
    one — as above.

## Keyboard and focus

++esc++ clears the focused filter field, and the × at its trailing edge does the same with the
mouse. ++tab++ walks the fields.

While a filter has focus the table's own key handling stands down completely, so typing `a` goes
into the field rather than opening a cell editor, and the arrow keys move the caret rather than
the row cursor. This holds for a custom `filterContent` too, without it having to do anything.

## Empty results

A filter that matches nothing is not the same as a table with no data, and the table says so —
`No rows match the filter`, rather than `No data available`. Replace it with `noResultsContent`:

```kotlin
DataTable(
    // ...
    noResultsContent = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nothing matches those filters.")
            TextButton(onClick = { filters = emptyMap() }) { Text("Clear filters") }
        }
    },
)
```

## Filtering and the rest of the table

| | |
|---|---|
| **Sorting and paging** | Run after filtering. Changing a filter returns to the first page, so a narrowed table never opens on a page that no longer exists. |
| **Select-all** | Selects the rows that matched. Rows filtered out are left alone, selected or not — selection is by key, so anything already selected survives being filtered away and comes back with it. |
| **Focus and cell ranges** | Keyed by row, so focus on a filtered-out row simply stops drawing; a cell range with a corner outside the filtered set is cleared rather than redrawn somewhere else. |
| **`groupBy`** | Groups are built from the filtered rows, so an emptied group disappears rather than showing an empty band. |

## Styling

The filter row has its own background and field colors, and its own text style:

```kotlin
colors = DataTableDefaults.colors(
    filterRow = Color(0xFFF7F7F7),
    filterField = Color.White,
),
textStyles = DataTableDefaults.textStyles(
    filterField = MaterialTheme.typography.bodySmall,
),
```

## Limitations

- **One query per column, ANDed.** No OR across columns, and no filter spanning two of them. A
  filter over several fields at once belongs above the table, feeding `items`.
- **No built-in checklist** of the values that occur — `filterContent` plus your own `distinct()`
  is where an Excel-style value picker gets built.
- **Every filter is a string.** Anything richer encodes itself into one and decodes in its
  predicate.
