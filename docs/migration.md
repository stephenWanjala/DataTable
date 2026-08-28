# Migration

Each release is listed newest first.

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
