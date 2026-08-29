# Selection & Expansion

Both are tracked by **row key** — the value returned by `itemKey` — not by the item itself.

That means selection and expansion survive item instances being replaced (a refresh from your
repository, say), and your row type never needs to implement `equals`/`hashCode`.

## Selection

```kotlin
var selectedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }

DataTable(
    items = people,
    headers = headers,
    itemKey = { it.id },                        // this is the selection identity
    showSelect = true,                          // show checkboxes
    selectionMode = SelectionMode.MULTI,        // NONE, SINGLE, or MULTI
    selectedKeys = selectedKeys,
    onSelectionChange = { selectedKeys = it },
)
```

### Modes

| Mode | Behaviour |
|------|-----------|
| `SelectionMode.NONE` | No selection UI. Row clicks still fire `onRowClick`. |
| `SelectionMode.SINGLE` | One row at a time. Clicking a selected row deselects it. |
| `SelectionMode.MULTI` | Multiple rows, with a select-all checkbox in the header. |

`selectionMode` defaults to `MULTI` when `showSelect = true`, and `NONE` otherwise.

### Resolving keys back to items

```kotlin
val selectedPeople = people.filter { it.id in selectedKeys }
```

If you need this often, keep a lookup rather than filtering repeatedly:

```kotlin
val byId = remember(people) { people.associateBy { it.id } }
val selectedPeople = selectedKeys.mapNotNull { byId[it] }
```

### Select-all

In `MULTI` mode the header checkbox selects every row in `items` — not just the current page.

!!! warning "Except under manual pagination"
    With [`manualPagination`](sorting-and-paging.md#server-side-data), `items` **is** the current
    page, so select-all covers only the loaded rows. The table cannot select rows it has never
    seen. Implement "select all N matching rows" yourself if you need it.

## Expansion

Same model, separate set:

```kotlin
var expandedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }

DataTable(
    // ...
    itemKey = { it.id },
    showExpand = true,
    expandedKeys = expandedKeys,
    onExpandChange = { expandedKeys = it },
    expandContent = { person ->
        Column(Modifier.padding(16.dp)) {
            Text("Details for ${person.name}")
            Text("Email: ${person.email}")
        }
    },
)
```

`showExpand` adds a chevron column at the leading edge. `expandContent` renders below the row when
it is expanded, spanning the full table width.

Expand-all and collapse-all are just set operations:

```kotlin
Button(onClick = { expandedKeys = people.map { it.id }.toSet() }) { Text("Expand All") }
Button(onClick = { expandedKeys = emptySet() }) { Text("Collapse All") }
```

## Cell range selection

Row selection is the checkbox column. **Cell** selection is a block — the rectangle between an
anchor and the cursor — and it comes with `cellNavigation`, alongside the cell cursor described
in [Cell Editing](editing.md#cell-navigation).

| Key | Action |
|-----|--------|
| ++shift++ + any movement key | Extend the block instead of moving the cursor alone |
| ++shift++ +click | Extend the block to the clicked cell |
| ++ctrl+a++ | Select every cell |
| any movement key without ++shift++ | Collapse the block back to one cell |

`++shift++` works with all of ++arrow-up++/++arrow-down++/++arrow-left++/++arrow-right++,
++home++/++end++, ++ctrl+home++/++ctrl+end++, and ++page-up++/++page-down++, so
++ctrl+shift+end++ selects from the cursor to the bottom-right corner. ++tab++ is the one
exception — ++shift++ already means "backwards" there.

```kotlin
val range: CellRange? = tableState.selectedRange   // anchor + focus corners

tableState.isCellInRange(person.id, "salary")      // membership, in constant time

tableState.selectRange(
    anchor = CellPosition(people.first().id, "name"),
    focus = CellPosition(people.last().id, "salary"),
)
tableState.clearRange()
```

Either corner may be the top-left one — `anchor` is simply where the selection started — so read
membership through `isCellInRange` rather than comparing corners yourself.

Like the cursor, a range is held as **keys**, not coordinates. Re-sorting the table keeps the same
cells highlighted rather than sweeping the rectangle across whichever rows moved into those
positions. A range whose corner leaves the view entirely — filtered out, or on another page — is
dropped rather than guessed at.

## Clipboard copy

++ctrl+c++ copies the selection as tab-separated text, which is what spreadsheets paste natively.
What "the selection" means runs narrowest first:

1. The selected block of cells, if there is one.
2. Otherwise the checked rows, across every visible column.
3. Otherwise the focused cell alone.
4. Otherwise the focused row, across every visible column.

If none of those exist, ++ctrl+c++ is left **unconsumed** — an application with its own copy
binding keeps it whenever the table has nothing to give.

Copy works without `cellNavigation` too: a table navigated by row still has checked rows worth
putting on the clipboard.

### Taking the payload yourself

`onCopy` intercepts the copy before it reaches the clipboard, and hands you the rows and columns
rather than a flattened string:

```kotlin
DataTable(
    // ...
    onCopy = { selection ->
        // selection.rows    : List<Person>, in display order
        // selection.columns : List<DataTableHeader<Person>>, in display order
        // selection.cells   : List<List<String>>, row-major, as the cells read
        exportCsv(selection.rows, selection.columns)
    },
)
```

Because you get your own row type back — not text the exporter would have to re-parse — this is
the seam a CSV or XLSX export hangs off, applied to exactly what the user selected, sorted and
paged as they are looking at it.

`cells` is what the cells *read*: each column's `value` put through its
[`format`](columns.md#formatting), so a copy carries the same currency symbols and dates the user
was looking at. An exporter that wants the numbers behind them has both halves — `columns` carries
every column's `value` extractor, and `rows` the items to run it against.

!!! warning "`onCopy` replaces the clipboard write, it does not run alongside it"
    Supplying `onCopy` puts you in charge of where the copy goes. A handler that only logs or
    displays the selection leaves the clipboard **empty** — which looks exactly like ++ctrl+c++
    being broken.

    To do both, call `copyToSystemClipboard()` yourself:

    ```kotlin
    onCopy = { selection ->
        auditLog.record(selection.rows.size)
        selection.copyToSystemClipboard()
    }
    ```

    It returns `false` rather than throwing when there is no clipboard to write to — a headless
    JVM, or a session where another application is holding it — so a copy that cannot complete
    leaves the table working. There is also a plain `copyToSystemClipboard(text)` for handlers
    that build their own format and still want it on the clipboard.

`toTabSeparated(includeHeader = true)` leads with a row of column titles; it is off by default,
matching what a spreadsheet puts on the clipboard when you copy a range.

Values containing a tab, a newline, or a double quote are quoted and their quotes doubled, so a
cell holding free-text notes cannot silently split one row into several on paste.

!!! warning "Cells need a `value` to be copyable"
    Cell text comes from each column's `value`. A column that renders only through `cellContent`
    has no text the table can read back and copies as empty. Give it a `value` and it becomes
    sortable and copyable at once, while `cellContent` keeps control of how it looks.

### What is not there

There is no **paste**: filling a block of cells from the clipboard is not supported, so a
round-trip through a spreadsheet is one-way. Selection is also a single rectangle — there is no
++ctrl++ +click to build up several disjoint blocks.

## Why keys

The obvious alternative — `Set<Person>` — has three problems this design avoids:

1. It requires `equals`/`hashCode` on your row type, which is not always yours to change.
2. Selection breaks when items are replaced by equal-but-distinct instances, as happens on any
   refresh that rebuilds the list.
3. Checking "is everything selected" means comparing whole objects on every recomposition.

Keys are small, stable, and cheap to compare.
