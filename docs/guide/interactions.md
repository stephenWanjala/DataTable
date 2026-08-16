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

## Keyboard navigation

Click anywhere in the table to give it focus, then:

| Key | Action |
|-----|--------|
| ++arrow-up++ / ++arrow-down++ | Move the focused row |
| ++enter++ | Fire `onRowClick` on the focused row |
| ++space++ | Toggle selection on the focused row |
| ++home++ | Focus the first row |
| ++end++ | Focus the last row |

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

!!! note "State is not persisted"
    Column widths, sort, focus, and scroll position live only as long as the composition. Saving
    and restoring a user's grid layout across restarts is not built in — hoist the pieces you care
    about and persist them yourself.

## Scrolling behaviour

Horizontal scrolling responds to a horizontal wheel or trackpad swipe, and to ++shift++ + vertical
wheel. Press-and-drag deliberately does **not** pan the table — that would fight the column resize
handles and text selection.

Scrollbars can be hidden with `showScrollbars = false`.
