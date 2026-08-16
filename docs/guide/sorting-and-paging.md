# Sorting & Paging

## Sorting

### Single column

```kotlin
var sortState by remember { mutableStateOf(SortState()) }

DataTable(
    // ...
    sortBy = sortState,
    onSortChange = { sortState = it },
)
```

Clicking a header cycles ascending → descending → none.

### Multi-column

Hold ++ctrl++ while clicking headers to build up a sort. A small priority number appears beside
each indicator.

```kotlin
var multiSort by remember { mutableStateOf<List<SortState>>(emptyList()) }

DataTable(
    // ...
    multiSortBy = multiSort,
    onMultiSortChange = { multiSort = it },
)
```

Multi-sort takes precedence over `sortBy` when non-empty. A plain (non-++ctrl++) click clears it.

### Custom comparators

By default a column sorts on whatever `value` returns, compared as `Comparable`. That breaks as
soon as `value` formats for display:

```kotlin
DataTableHeader(
    key = "salary",
    title = "Salary",
    value = { "$${"%.2f".format(it.salary)}" },   // "$1,234.00" sorts as a string!
    comparator = compareBy { it.salary },          // so sort on the number instead
)
```

`comparator` always wins over the default comparison.

### Disabling

`sortable = false` on a header makes it inert.

## Controlled vs uncontrolled

Supplying `onSortChange` makes sorting **controlled**: the table renders whatever `sortBy` says
and never changes it on its own, exactly like `TextField(value, onValueChange)`.

```kotlin
// Controlled — you own the state
var sort by remember { mutableStateOf(SortState()) }
DataTable(sortBy = sort, onSortChange = { sort = it }, /* ... */)

// Uncontrolled — the table owns it, click-to-sort just works
DataTable(/* no sortBy, no onSortChange */)
```

!!! danger "Controlled means you must feed the value back"
    ```kotlin
    // The callback fires. Nothing sorts.
    DataTable(sortBy = SortState(), onSortChange = { analytics.track(it) })
    ```
    If you only want to observe, keep the state as well as tracking it.

`onMultiSortChange` and `onPageChange` behave identically for multi-sort and for the current page.
You can mix modes freely — controlled sorting with uncontrolled paging is fine.

## Pagination

```kotlin
var currentPage by remember { mutableStateOf(0) }
var itemsPerPage by remember { mutableStateOf(25) }

DataTable(
    // ...
    showPagination = true,
    itemsPerPage = itemsPerPage,
    currentPage = currentPage,
    onPageChange = { currentPage = it },
    itemsPerPageOptions = listOf(10, 25, 50, 100),
    onItemsPerPageChange = {
        itemsPerPage = it
        currentPage = 0
    },
)
```

`showPagination` swaps the row-count footer for page controls: first/previous/next/last, a page
indicator, a range readout, and a rows-per-page menu when `onItemsPerPageChange` is supplied.

Changing the page size resets to the first page, since a larger page can put the current one past
the end.

## Server-Side Data

By default the table sorts and pages `items` itself, which means every row has to be in memory.
For an ERP-scale table backed by a database, hand it one page at a time and let SQL do the work.

```kotlin
var page by remember { mutableStateOf(0) }
var pageSize by remember { mutableStateOf(25) }
var sort by remember { mutableStateOf(SortState("name", SortOrder.ASCENDING)) }

// Re-query whenever the page or the sort changes
val result by produceState<PageResult?>(null, page, pageSize, sort) {
    value = repository.findPeople(offset = page * pageSize, limit = pageSize, sort = sort)
}

DataTable(
    items = result?.rows.orEmpty(),   // just this page
    headers = headers,
    itemKey = { it.id },
    loading = result == null,

    manualSorting = true,             // rows arrive already ordered
    sortBy = sort,
    onSortChange = { sort = it; page = 0 },

    showPagination = true,
    manualPagination = true,          // items IS the current page
    totalItems = result?.total ?: 0,  // required: only you know the real total
    itemsPerPage = pageSize,
    currentPage = page,
    onPageChange = { page = it },
    onItemsPerPageChange = { pageSize = it; page = 0 },
)
```

| Parameter | Effect |
|-----------|--------|
| `manualSorting = true` | The table never reorders `items`. Headers still show sort indicators and report clicks, so you translate them into `ORDER BY`. |
| `manualPagination = true` | The table never slices `items` — it renders them as the current page. Requires `totalItems`. |
| `totalItems` | Row count across every page, used for the page count and the footer's range. Defaults to `items.size`, which is only correct when the table holds all the rows. |

The two are independent. Sorting server-side while paging client-side is unusual but legal.

!!! danger "`manualPagination` requires `totalItems`"
    It throws when missing, rather than quietly reporting a single page — the table holds one page
    and genuinely cannot work out how many there are.

!!! warning "Select-all covers the loaded page only"
    See [Selection](selection.md#select-all).

### Resetting the page on sort

Sorting server-side changes which rows land on page 1, so a stale page index will look wrong.
Reset it in the same callback:

```kotlin
onSortChange = { sort = it; page = 0 },
```

The table does not do this for you — under `manualSorting` it has no idea whether your new
ordering invalidates the current offset.
