# Column Layout

Users rearrange a grid once and expect it to stay that way. `DataTableState` holds everything they
change — column widths, which columns they hid, the order they put them in, the sort, the filters —
and hands it over as one snapshot you can store.

```kotlin
// Save
val encoded = tableState.captureLayout().encodeToString()
settings.putString("employees.layout", encoded)

// Restore, before the first composition
val tableState = rememberDataTableState()
remember {
    settings.getString("employees.layout")
        ?.let { DataTableLayout.decodeFromString(it) }
        ?.let(tableState::applyLayout)
}
```

That is the whole feature. The rest of this page is what is in the snapshot, and what happens when
it and the table disagree.

## What a layout holds

| | |
|---|---|
| `columnWidths` | What the user dragged columns to. Columns absent keep their declared width. |
| `hiddenColumns` | Columns the user hid. |
| `columnOrder` | Column keys in the order the user put them. |
| `sortBy` / `multiSortBy` | The sort in force. |
| `filters` | Filter queries per column. |

It is keyed entirely by `DataTableHeader.key` and holds nothing about the data, so a layout
belongs to a *user*, not to a query — save it once and apply it to every page of results.

`DataTableLayout` is a plain `data class`. Keep it whole with `encodeToString`, or map its fields
onto a type of your own — `Dp` is a `Float` in `dp` behind `.value`.

!!! tip "Restoring filters is a choice"
    A saved layout carries the filters the user left behind, which is usually what they want back.
    Where it is not — a report that should open showing everything — drop them on the way in:
    `layout.copy(filters = emptyMap())`.

## Hiding columns

The table has its own menu for this — JavaFX's table menu button, in Compose:

```kotlin
DataTable(
    // ...
    showColumnMenuButton = true,
)
```

A button appears at the trailing edge of the header; clicking it lists every column with a
checkbox. Nothing else to build, and what the user picks is part of the layout the snapshot
carries.

Columns whose header declares `visible = false` are not offered — see below. The button is drawn
as part of the default header, so `hideDefaultHeader` or a custom `headerContent` leaves it out.

Driving it yourself is the same one call the menu makes:

```kotlin
tableState.setColumnHidden("notes", true)
tableState.isColumnHidden("notes")
```

This is a different thing from `DataTableHeader.visible`, and they do not fight:

| | |
|---|---|
| `visible = false` on the header | **This column is not available** — a caller's decision, about permissions or context. |
| `setColumnHidden(key, true)` | **The user does not want to see it** — theirs, and saved with their layout. |

Hiding adds to `visible`; showing cannot overrule it. A restored layout has no business bringing
back a column the application has decided this user may not see.

## Ordering columns

`columnOrder` is a list of column keys. It does not have to be complete — columns it leaves out
follow the ones it names, in declaration order, which is where a column added since the layout was
saved appears.

```kotlin
tableState.columnOrder = listOf("salary", "name")   // those two first, the rest as declared
tableState.moveColumn("salary", 0)                  // or move one at a time
tableState.columnOrder = emptyList()                // back to declaration order
```

`moveColumn` counts positions over the visible leaf columns in display order, frozen ones first,
and clamps an index outside the table rather than throwing.

!!! note "Groups move as blocks"
    With [grouped headers](columns.md#nested-grouped-headers), the order applies at every level of
    the tree: naming a column moves its group among the top-level columns *and* moves that column
    within its group. What it cannot do is take a column out of the group it was declared in — a
    group whose columns were scattered across the table would no longer be a group.

There is no drag-to-reorder in the header yet; `moveColumn` is the seam it will sit on.

## Capturing and applying

`captureLayout()` reads what is **displayed**, so it captures a sort or a set of filters the caller
controls exactly as well as one the table owns.

`applyLayout()` is not quite symmetric, and the asymmetry is the controlled/uncontrolled split
everywhere else in the table:

| | |
|---|---|
| Widths, hidden columns, order | Always applied — the table owns them. |
| Sort and filters | Applied only when the table owns them. Supply `onSortChange` or `onFiltersChange` and the table renders what *you* pass, so restore those from the snapshot into your own state. |

```kotlin
// Controlled sorting and filtering: apply the parts the table cannot.
val layout = DataTableLayout.decodeFromString(saved) ?: DataTableLayout()
tableState.applyLayout(layout)      // widths, hidden columns, order
sort = layout.sortBy                // your state
filters = layout.filters            // your state
```

`resetLayout()` drops the arrangement — widths, hidden columns, order — and deliberately leaves
sorting and filtering alone: they are what the table is *showing*, not how it is arranged, and a
"reset the layout" button that silently cleared a user's filters would be a surprise.
`resetColumnWidths()` is still there for the narrower case.

## When the layout and the table disagree

A layout outlives the table it was saved from — columns get added, removed, and renamed between
releases — so applying one never fails:

- A width, a hidden flag, or an order entry naming a column that no longer exists is ignored.
- A column the layout has never heard of keeps its declared width and sits after the ones it
  names, which is where a newly added column shows up.
- `decodeFromString` returns `null` for anything that is not a layout — an empty preference, a
  value another version of the app wrote, JSON from somewhere else — rather than throwing. Fall
  back to the defaults and carry on:

```kotlin
val layout = stored?.let { DataTableLayout.decodeFromString(it) } ?: DataTableLayout()
```

- Within a layout it does recognise, an entry it cannot read is skipped rather than failing the
  whole restore. One unreadable width is not a reason to lose the other twenty.

## What is not in a layout

- **The current page**, and the rows per page. Which page you are on is not how a table is
  arranged — persist those yourself if you want them back.
- **Selection, expansion, cell focus, and scroll position.** These belong to a session, not to a
  layout.
- **The headers themselves.** A layout references columns by key; it never carries their titles,
  widths-as-declared, or content.
