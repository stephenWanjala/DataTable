# Interactions

## Row callbacks

```kotlin
DataTable(
    // ...
    onRowClick = { person -> println("Clicked: ${person.name}") },
    onRowDoubleClick = { person -> println("Double-clicked: ${person.name}") },
    onRowContextMenu = { person, offset -> println("Right-click: ${person.name} at $offset") },
)
```

`onRowContextMenu` hands you the row and the pointer position, so you can position your own popup:

```kotlin
var menuTarget by remember { mutableStateOf<Pair<Person, Offset>?>(null) }

DataTable(
    // ...
    onRowContextMenu = { person, offset -> menuTarget = person to offset },
)

menuTarget?.let { (person, offset) ->
    // position a Popup at `offset` and act on `person`
}
```

!!! tip "Only pass `onRowDoubleClick` if you use it"
    Gesture detection has to wait for a possible second tap before it can report a single one.
    The table skips that wait entirely when no double-click handler is supplied, so leaving it
    `null` keeps single clicks instant.

    Supplying it also claims the gesture from
    [double-click-to-edit](editing.md#opening-and-closing-an-editor).

## Keyboard navigation

Click anywhere in the table to give it focus, or ++tab++ to it, then:

| Key | Action |
|-----|--------|
| ++arrow-up++ / ++arrow-down++ | Move the focused row |
| ++enter++ | Fire `onRowClick` on the focused row |
| ++space++ | Toggle selection on the focused row |
| ++home++ | Focus the first row |
| ++end++ | Focus the last row |

++ctrl+c++ copies the checked rows, or the focused row when none are checked — see
[Clipboard copy](selection.md#clipboard-copy). It is left unconsumed when there is nothing to
copy, so it stays available to the surrounding application.

That is row navigation, which is always on. Setting `cellNavigation = true` — or declaring any
editable column — moves focus down to the cell and adds ++arrow-left++/++arrow-right++, ++tab++,
++page-up++/++page-down++, ++shift++ +movement to select a block, ++ctrl+a++, and the editing
shortcuts. See [Cell Editing](editing.md#cell-navigation) and
[Range selection](selection.md#cell-range-selection).

### Tab order

Per-row checkboxes and expand buttons are deliberately **outside** the Tab order. One stop per
control per row would mean two presses per visible row just to get past the table — unusable on
real data. Both remain clickable, and ++space++ on the focused row toggles its selection without
needing to reach the checkbox.

What stays reachable by ++tab++: the table itself as a single stop, the header's select-all
checkbox, and the footer's page buttons and rows-per-page selector. The cost of tabbing past a
table is therefore fixed — a forty-row table takes the same presses as a three-row one.

Focus is tracked **by key**, exposed as `state.focusedKey`. Because it is a key rather than a
position, focus stays on the same row when the table is re-sorted instead of sticking to an offset.

If the focused row leaves the view — filtered out, or on another page — the next arrow key starts
again from the first row, and ++enter++/++space++ do nothing rather than acting on whatever row
has drifted into that position.

The table takes focus on click rather than on appearing, so dropping one into a form does not
steal focus from whatever the user was typing in. To start it focused, call
[`focusRow`](#datatablestate) yourself.

## DataTableState

`rememberDataTableState()` gives you programmatic control over scrolling, focus, and column widths.

```kotlin
val tableState = rememberDataTableState()

DataTable(
    // ...
    state = tableState,
)
```

### Scrolling

```kotlin
val scope = rememberCoroutineScope()

scope.launch { tableState.animateScrollToItem(index = 42) }
scope.launch { tableState.scrollToItem(index = 0) }

val firstVisible = tableState.firstVisibleItemIndex
```

### Focus

```kotlin
val focused = tableState.focusedKey        // Any?, null when nothing is focused

tableState.focusRow(person.id)             // move focus
tableState.focusRow(null)                  // clear it
```

Under `cellNavigation` the same state also carries the focused column, as
[`focusedCell`](editing.md#cell-navigation).

`focusRow` only moves focus; it does not scroll. `DataTableState` never sees `items`, so it cannot
resolve a key to a position on its own. Pair the two when you need both:

```kotlin
val position = people.indexOfFirst { it.id == personId }
tableState.focusRow(personId)
if (position >= 0) {
    scope.launch { tableState.animateScrollToItem(position) }
}
```

Focusing a key that is not currently displayed is harmless — nothing draws the indicator.

### Column widths

```kotlin
Button(onClick = { tableState.resetColumnWidths() }) {
    Text("Reset Columns")
}
```

Clears every user resize, reverting to the widths declared on the headers.

Widths are part of a user's layout, along with the columns they hid, the order they put them in,
the sort, and the filters — `tableState.captureLayout()` hands all of it over as one snapshot to
store, and `applyLayout` puts it back. See [Column Layout](layout.md).

!!! note "Cell focus and scroll are not part of it"
    A layout describes how a table is *arranged*. Focus, selection, expansion, and scroll position
    belong to a session and live only as long as the composition.

## Scrolling behaviour

Horizontal scrolling responds to a horizontal wheel or trackpad swipe, and to ++shift++ + vertical
wheel. Press-and-drag deliberately does **not** pan the table — that would fight the column resize
handles and text selection.

Scrollbars can be hidden with `showScrollbars = false`, which applies to the vertical and
horizontal bars together.

### Styling the scrollbars

The table draws Compose Desktop's own `VerticalScrollbar` and `HorizontalScrollbar`, so they are
styled the way every other scrollbar in your application is — through `LocalScrollbarStyle`:

```kotlin
CompositionLocalProvider(
    LocalScrollbarStyle provides LocalScrollbarStyle.current.copy(
        thickness = 12.dp,
        hoverColor = Color(0x99000000),
        unhoverColor = Color(0x33000000),
    )
) {
    DataTable(/* ... */)
}
```

`ScrollbarStyle` carries `thickness`, `minimalHeight`, `shape`, `hoverDurationMillis`,
`unhoverColor`, and `hoverColor`. `DataTable` has no scrollbar parameters of its own: a knob for
one of those six would leave the other five to the composition local anyway, and set the table's
scrollbars apart from the rest of the window for no good reason.
