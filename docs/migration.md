# Migration

Each release is listed newest first.

## Unreleased

Cell-level focus, grid keyboard navigation, in-place cell editing, cell range selection,
clipboard copy, per-column display formatting, a filter row, a saveable column layout, and
horizontal virtualization. Almost all of it is additive: a table that does not opt in behaves as
it did in 0.4.0, with three exceptions noted below — ++ctrl+c++ is now bound, `scrollbarThickness`
is removed, and **row height** has a default, which is the one change that can alter how an
existing table looks.

### Rows have a uniform height, and columns are virtualized

**This is the one change that can alter an existing table's appearance.** `DataTable` gained a
`rowHeight`, defaulting to one line of body text at the table's density — 52dp at `DEFAULT`, 44dp
at `COMFORTABLE`, 36dp at `COMPACT`. Every data row is exactly that tall.

Before, a row grew to fit its tallest cell, so a column wrapping onto three lines made every row
three lines deep. Now that column is clipped instead. Two ways to take it:

```kotlin
// The content has a fixed size that just needs more room — a chip, an avatar, two lines.
DataTable(/* ... */, rowHeight = 56.dp)

// The number of lines genuinely varies per row.
DataTable(/* ... */, rowHeight = null)
```

If your table already had a **frozen column**, its rows were being pinned at 50dp anyway — that
was hardcoded, undocumented, and a point short of what a line of text at `DEFAULT` density needs,
so text was clipped by a pixel. Those tables get a pixel back and lose the inconsistency: a row
is now the same height whether or not anything is pinned.

**What it buys.** Because a row's height is known without measuring its cells, the table now
composes only the columns near the viewport, replacing each run that scrolled out of sight with
one spacer of the same width. A forty-column grid stops composing forty cells for every visible
row. This is why the two are one setting: under `rowHeight = null` a row's height depends on
cells that may not be composed, so culling switches itself off. See
[Columns](guide/columns.md#row-height-and-horizontal-virtualization).

One thing to check if you use `cellContent`: a culled cell is not composed, so an effect inside
one stops running while its column is off screen. `LazyColumn` already does this to rows scrolled
off the bottom, but a paginated table never scrolls off the bottom at all.

### `scrollbarThickness` is gone

`DataTable` no longer takes a `scrollbarThickness`. It never did anything — the parameter was
accepted and never read, in every version since the first, and its original KDoc said as much
("currently decorative") before that caveat was lost. Nothing renders differently for having
dropped it; only a call site that passed it stops compiling, which is the point — it was not
doing what it looked like it was doing.

Scrollbars are Compose Desktop's own, so style them through `LocalScrollbarStyle`, which covers
thickness along with colours, shape, and hover timing:

```kotlin
CompositionLocalProvider(
    LocalScrollbarStyle provides LocalScrollbarStyle.current.copy(thickness = 12.dp)
) {
    DataTable(/* ... */)
}
```

See [Styling the scrollbars](guide/interactions.md#styling-the-scrollbars).

### Cell navigation is opt-in

`cellNavigation = true` moves keyboard focus from the row down to the cell. Declaring any column
`editable = true` turns it on by itself, because an editor you cannot reach with the keyboard is
no use.

Leave both alone and nothing changes: ++arrow-up++/++arrow-down++ still walk rows, ++home++ and
++end++ still jump to the first and last row, ++enter++ still fires `onRowClick`, and no cell
cursor is ever drawn.

Once it *is* on, some keys mean something new. ++home++ and ++end++ move within the row and take
++ctrl++ to reach the ends of the table; ++tab++ walks cells instead of leaving the table in one
press, though it still falls through at the very first and very last cell so keyboard users are
never trapped; and ++enter++ on an editable column opens an editor rather than firing
`onRowClick`. ++space++ is unchanged and still toggles selection.

### Double-click opens an editor, unless you claimed it

In a table with editable columns, double-clicking a cell opens its editor. Supplying
`onRowDoubleClick` takes the gesture back — yours is called and no editor opens, so a detail
dialog and in-place editing can coexist. Tables with no editable columns are unaffected either
way.

### Ctrl+C is now bound, but only when there is something to copy

++ctrl+c++ copies the selected block of cells, or the checked rows, or the focused cell or row —
see [Clipboard copy](guide/selection.md#clipboard-copy). This is the one binding that applies
whether or not you opt into cell navigation, because a table navigated by row still has checked
rows worth copying.

It is deliberately left **unconsumed** when none of those exist, so an application with its own
++ctrl+c++ handler keeps it whenever the table has nothing to give. If you need it back
unconditionally, handle the key above the table — a `Modifier.onPreviewKeyEvent` on an ancestor
sees it first.

By default the table writes tab-separated text to the system clipboard itself. Supplying `onCopy`
*takes that over* — it replaces the clipboard write rather than running alongside it — and hands
you the rows and columns as your own types instead. A handler that wants both calls
`selection.copyToSystemClipboard()`.

### Shift now extends a selection in grid mode

With `cellNavigation` on, holding ++shift++ with a movement key extends a block of cells rather
than moving the cursor alone, and ++ctrl+a++ selects every cell. Neither key did anything in
0.4.0. ++tab++ is unaffected — ++shift++ still means "backwards" there.

### Columns format for display, and keep their values raw

`DataTableHeader` gains `format: ((Any?) -> String)?`, which turns the extracted value into the
text a cell shows. It changes nothing on its own — a column without one still renders
`value.toString()` — but it retires the workaround it replaces. Formatting inside `value` made a
column sort as text, which cost a `comparator`, and opened editors onto formatted text, which
cost an `editValue`. Move the formatting to `format` and both go away:

```kotlin
// Before
value = { "$${"%,.0f".format(it.salary)}" },
comparator = compareBy { it.salary },
editValue = { it.salary.toLong().toString() },

// After
value = { it.salary },
format = DataTableFormatters.currency(decimals = 0),
```

`DataTableFormatters` is a new object of ready-made formatters — `currency`, `number`, `percent`,
`date`, and `boolean` — each locale-aware, and each rendering nulls and unexpected types rather
than throwing during layout.

One behaviour did change for tables that already copy: `ClipboardSelection.cells` now holds the
formatted text rather than `value.toString()`. Columns without a `format` are unaffected, and a
handler that wants the raw values still has `columns` (with their `value` extractors) and `rows`.

### Columns can filter themselves

`DataTableHeader` gains `filterable`, and with it a filter row under the header. The row exists
only once some column asks for it, so nothing changes for a table that does not.

```kotlin
DataTableHeader(
    key = "name", title = "Name", value = { it.name },
    filterable = true, filterPlaceholder = "Contains…",
)
```

The default match is a case-insensitive contains against the cell's text *and* its raw value;
`filterPredicate` replaces it with a range, an exact match, or whatever the column needs, and
`filterContent` replaces the text field with a control of your own.

Filters are a `Map<String, String>` keyed by column, ANDed across columns, and applied ahead of
sorting and paging. Supply `onFiltersChange` to own them yourself — the same controlled/
uncontrolled split `onSortChange` already has — or `manualFiltering = true` to hand the whole
thing to a query, with the filter row still reporting what the user types. `noResultsContent`
covers the empty state a filter causes, which the table now tells apart from having no data.

Two knock-on changes for tables that already page or select:

- The footer's total and the page count are the **filtered** count. Under `manualPagination`
  that is `totalItems`, which now has to be the count of what matched.
- Select-all covers the rows that survived the filters, not every row in `items`. With no
  filters active — every table before this release — both are the same list.

### A user's arrangement can be saved and put back

`DataTableState.captureLayout()` returns a `DataTableLayout` — column widths, hidden columns,
column order, sort, and filters — and `applyLayout` puts one back. `encodeToString` and
`DataTableLayout.decodeFromString` turn it into a string for a preferences store and back, so
persisting a grid is two lines rather than a mapping layer:

```kotlin
settings.putString("employees.layout", tableState.captureLayout().encodeToString())

// next session, before the first composition
remember {
    settings.getString("employees.layout")
        ?.let { DataTableLayout.decodeFromString(it) }
        ?.let(tableState::applyLayout)
}
```

`showColumnMenuButton = true` gives the header its own menu for showing and hiding columns — the
equivalent of JavaFX's `isTableMenuButtonVisible` — so the common case needs no UI of your own.

Hiding and ordering columns are new state on the table, alongside the widths it already held:
`setColumnHidden`, `isColumnHidden`, `hiddenColumns`, `columnOrder`, `moveColumn`, and
`resetLayout`. Hiding *adds* to `DataTableHeader.visible` rather than overriding it — a column a
caller declared unavailable stays unavailable whatever a restored layout says — so nothing changes
for a table that never calls them.

`applyLayout` restores sort and filters only where the table owns them. A table given
`onSortChange` or `onFiltersChange` renders what its caller passes, so read those off the snapshot
into your own state; `captureLayout` reads what is *displayed* and so captures them either way.

Internally, the table's own sort and filter state moved onto `DataTableState` — which is what lets
a layout put them back. The contract is unchanged: both are still seeded from the parameters once
and then owned by the table until a callback takes them over. One consequence worth knowing: that
state now lives as long as the `DataTableState` rather than as long as the `DataTable` call, so a
table that is removed and re-composed against the same remembered state keeps its sort instead of
re-seeding from `sortBy`.

### Headers sort on release, not on press

`reorderableColumns = true` lets a user drag a header to a new position. Because a drag starts as
a press on the same header a click sorts, sorting now fires when the pointer comes **up** rather
than when it goes down — otherwise reaching for a column to move it would re-sort the table on the
way. A press and release without travel sorts exactly as before, and this applies whether or not
reordering is on, so the two behave the same.

A drag is hit-tested against the column's own siblings: a grouped column moves within its group
and stops at the edge of it, a group is dragged by its band and moves as one block, and the frozen
and scrolling sections are separate header rows, so nothing crosses the freeze boundary. There is
no keyboard equivalent — `moveColumn` remains the seam for UI of your own.

One related fix: a column the user has **hidden** now keeps its place in `columnOrder` while the
others are rearranged. Before, reordering anything while a column was hidden dropped it out of the
order, and it reappeared at the end of the table.

### New API

`DataTable` gains `cellNavigation` and `onCellEdit`. `DataTableHeader` gains `editable`,
`editValue`, `validateEdit`, `editorContent`, and `format` — all appended after `cellContent`, so
any positional construction of a header still compiles. `DataTableState` gains `focusedColumnKey`,
`focusedCell`, `isEditing`, `editingCell`, `editError`, `focusCell`, `startEditing`,
`cancelEditing`, and `revealFocusedCell`.

`DataTable` also gains `onCopy`, and `DataTableState` gains `selectionAnchor`, `selectedRange`,
`isCellInRange`, `selectRange`, `extendRangeTo`, and `clearRange`. `CellRange` and
`ClipboardSelection` are new types, alongside the top-level `copyToSystemClipboard(text)` and
`ClipboardSelection.copyToSystemClipboard(includeHeader)`.

`DataTable` also gains `filters`, `onFiltersChange`, `manualFiltering`, and `noResultsContent`,
and `DataTableHeader` gains `filterable`, `filterPlaceholder`, `filterPredicate`, and
`filterContent`. `ColumnFilterController` is a new type. The `DataTable` filtering parameters sit
with the other query parameters rather than at the end of the list, so a call passing arguments
positionally that far in needs them named — every one of them has a default, and named arguments
are how the other 30 are passed in practice.

`DataTable` gains `showColumnMenuButton`. `DataTableState` also gains `captureLayout`,
`applyLayout`, `resetLayout`, `setColumnHidden`,
`isColumnHidden`, `hiddenColumns`, `columnOrder`, and `moveColumn`, with `DataTableLayout` as a new
type.

`DataTable` gains `reorderableColumns`.

`DataTable` gains `rowHeight`, and `DataTableDefaults` gains `rowHeight(density)` to derive its
default. This one has a behavioural default rather than an inert one — see [Rows have a uniform
height](#rows-have-a-uniform-height-and-columns-are-virtualized) above.

`DataTable` **loses** `scrollbarThickness`, the only removal in this release. It was inert, and
`LocalScrollbarStyle` replaces it — see [above](#scrollbarthickness-is-gone).

`DataTableColors` gains `focusedCellBorder`, `editingCell`, `invalidCellBorder`, `selectedCell`,
`draggedColumn`, `columnDropIndicator`, `filterRow`, and `filterField`; `DataTableTextStyles` gains `cellEditor` and `filterField`. Both
are `data class`es with defaulted parameters, so existing `copy` and factory calls are unaffected.

See [Filtering](guide/filtering.md), [Column Layout](guide/layout.md),
[Cell Editing](guide/editing.md), and
[Range selection](guide/selection.md#cell-range-selection).

## 0.4.0

No signatures changed, so 0.4.0 compiles against 0.3.0 call sites untouched. It is a fix release:
four things that were documented in 0.3.0 but did not actually work now do, which means the table
may start behaving in ways your 0.3.0 workarounds were compensating for.

### Sorting cycles past ascending

Clicking a header in 0.3.0 always sorted **ascending**, however many times you clicked it. The
header's click handler captured the sort state from its first composition and never saw an update,
so the cycle logic always compared against an empty `SortState`.

Clicking now cycles ascending → descending → none, as
[Sorting](guide/sorting-and-paging.md#single-column) always described. Multi-sort with
++ctrl++ +click was stuck the same way and is fixed with it.

If you worked around this by inverting the order yourself in `onSortChange`, remove that — you will
now get the inversion twice.

### Keyboard navigation works at all

The table never took focus in 0.3.0, so no key event ever reached it and every shortcut was dead.
It now requests focus when clicked, and arrow keys, ++home++, ++end++, ++enter++, and ++space++
behave as [Interactions](guide/interactions.md#keyboard-navigation) documents.

Focus is taken on click, not on appearing, so a table dropped into a form still does not steal
focus from whatever the user is typing in.

### Tabbing past a table no longer costs a stop per row

Per-row checkboxes and expand buttons are no longer in the Tab order. In 0.3.0 they were, so Tab
walked two stops per visible row — a three-row table took eight presses to escape, and a realistic
one was unusable by keyboard. The cost is now fixed regardless of row count; see
[Tab order](guide/interactions.md#tab-order) for what stays reachable.

Both controls are still clickable, and ++space++ on the focused row still toggles selection. If you
relied on Tab reaching a row checkbox, use arrow keys and ++space++ instead.

### Weighted columns have a real width

A column with `width = null` collapsed to zero inside the horizontally scrolling area, because
`Modifier.weight` had nothing bounded to take a share of. Weighted columns now divide up the
viewport minus the fixed columns, the selection and expand controls, and any frozen section, with
`minColumnWidth` as the floor when the fixed columns already overflow.

Tables that declared a width on every column are unaffected. Tables that mixed the two will get
wider — and visible — weighted columns where they previously got nothing. See
[Width](guide/columns.md#width).

### Also in this release

- A test suite covering sorting, selection, pagination, keyboard navigation, tab order, column
  widths, and header-tree validation. The sort-cycle bug above is the one it found first.
- Built against Compose Multiplatform 1.11.1 (0.3.0 was built against 1.9.2), emitting Kotlin 2.2
  metadata and Java 11 bytecode. Compose is an `api` dependency, so Gradle will resolve your
  project's Compose artifacts up to 1.11.1 unless you constrain them — worth checking if you are
  pinned to an older Compose.
- The documented requirements were wrong before and are now correct: the artifact targets Java 11,
  not the "JVM 21+" the docs claimed. Nothing about the published bytecode changed.
- Dokka fails the build on an undocumented public member or a broken KDoc link, so the API
  reference stays complete.
- Releases now carry demo installers (`.deb`, `.tar.gz`, `.msi`, `.dmg`) built on each host OS.

## 0.3.0

No signatures changed, so 0.3.0 compiles against 0.2.0 call sites untouched. Three behaviour
changes are worth checking.

### Sorting and pagination are properly controlled

Supplying `onSortChange`, `onMultiSortChange`, or `onPageChange` now makes that piece of state
**controlled**: the table renders the parameter and never changes it on its own.

In 0.2.0 the table kept an internal copy that it updated on interaction regardless, so a caller
who passed the callback but ignored the parameter still saw sorting work. That now does nothing
visible — the click fires the callback, and the table waits for you to feed the value back:

```kotlin
// Broken in 0.3.0: the callback fires, but sortBy never changes
DataTable(
    sortBy = SortState(),                     // constant!
    onSortChange = { analytics.track(it) },
)

// Correct: hoist it
var sort by remember { mutableStateOf(SortState()) }
DataTable(
    sortBy = sort,
    onSortChange = { sort = it; analytics.track(it) },
)
```

Passing no callback is unchanged — the table owns the state, and click-to-sort works with no wiring.

### Nested headers render

`DataTableHeader.children` was accepted and silently ignored in every version up to 0.2.0. If you
set it, expecting nothing, you now get a grouped header. See
[Nested (Grouped) Headers](guide/columns.md#nested-grouped-headers) for the two sizing rules — both throw rather
than misrender.

### Misconfigured frozen columns throw

`fixed = true` without a `width` used to be quietly demoted to a normal scrolling column. It now
throws with a message naming the column. If pinning appeared not to work for you before, this is
why, and the fix is to give the column an explicit width.

### Also in this release

- `manualSorting`, `manualPagination`, and `totalItems` for [server-side data](guide/sorting-and-paging.md#server-side-data).
- Reworked pagination footer: grouped controls, a divider above it, hover states, and a
  rows-per-page menu that opens upward instead of off the bottom of the window.
- `DataTableHeader` and `SortState` are `@Immutable`, and Compose is exposed as `api` rather
  than `implementation` so consumers get it transitively.
- Press-and-drag no longer pans the table horizontally; wheel and trackpad scrolling are
  unchanged. This also stops drags fighting the column resize handles.

## 0.2.0

Selection, expansion, and keyboard focus are now tracked by **row key** instead of by item.
Every change below is a compile error on upgrade, not a silent behavior change, so the compiler
will point at each call site.

| 0.1.x | 0.2.0 |
|-------|-------|
| `selectedItems: Set<T>` | `selectedKeys: Set<Any>` |
| `expandedItems: Set<T>` | `expandedKeys: Set<Any>` |
| `itemKey` optional, defaulted to `hashCode()` | required, and moved ahead of `modifier` |
| `state.focusedRowIndex: Int` (`-1` when unfocused) | `state.focusedKey: Any?` (`null` when unfocused) |

### Selection and expansion

Hoist keys rather than items:

```kotlin
// 0.1.x
var selectedItems by remember { mutableStateOf<Set<Person>>(emptySet()) }
var expandedItems by remember { mutableStateOf<Set<Person>>(emptySet()) }

DataTable(
    items = people,
    headers = headers,
    selectedItems = selectedItems,
    onSelectionChange = { selectedItems = it },
    expandedItems = expandedItems,
    onExpandChange = { expandedItems = it },
)

// 0.2.0
var selectedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }
var expandedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }

DataTable(
    items = people,
    headers = headers,
    itemKey = { it.id },
    selectedKeys = selectedKeys,
    onSelectionChange = { selectedKeys = it },
    expandedKeys = expandedKeys,
    onExpandChange = { expandedKeys = it },
)
```

Where you previously read selected items directly, resolve them from keys:

```kotlin
val selectedPeople = people.filter { it.id in selectedKeys }
```

This is what the change buys you: selection and expansion now survive item instances being
replaced — a refresh from your repository, for example — and your row type no longer needs to
implement `equals`/`hashCode`.

### itemKey

`itemKey` is required and must be **unique across `items`**. Two rows sharing a key will select,
expand, and recycle as a single row. Use a database id or equivalent:

```kotlin
itemKey = { it.id },
```

The old `{ it.hashCode() }` default collides for equal-valued rows. It was already unsound for
`LazyColumn` identity, and now that it also backs selection, expansion, and focus, it is gone
rather than left as a trap.

It also moved ahead of `modifier` in the parameter list, keeping the Compose convention of
required parameters first. Call sites that pass it by name — as all the examples here do — are
unaffected by the reordering.

### Keyboard focus

```kotlin
// 0.1.x
val focused = state.focusedRowIndex          // -1 when nothing is focused

// 0.2.0
val focused = state.focusedKey               // null when nothing is focused
state.focusRow(person.id)                    // move focus programmatically
state.focusRow(null)                         // clear focus
```

Focus now stays on its row when the table is re-sorted, instead of holding a fixed position.
If the focused row leaves the view — filtered out, or on another page — the next arrow key
starts again from the first row.

### Fixes that may change what you see

No API change needed for these, but the rendering differs:

- **Alternating row colors** were offset by the total item count, flipping which rows were
  tinted on odd-sized lists. They now start correctly at the first row.
- **The keyboard focus indicator** never appeared in any configuration. It now renders on the
  focused row, so a `focusedRowBorder` color you set in 0.1.x becomes visible for the first time.
- **Enter and Space** acted on the wrong row when `groupBy` was set, because navigation walked
  the pre-grouping order. They now follow display order.
- **Arrow-key scrolling** overshot when group headers or summaries were present.
