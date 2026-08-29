# Cell Editing

The table edits cells in place, and it never touches your data while doing it. A commit is
reported through `onCellEdit`; you apply it to your own model and pass the updated `items` back.
That is what keeps one source of truth in your state — and what makes undo, dirty tracking, and
server round-trips possible at all.

```kotlin
var people by remember { mutableStateOf(initialPeople) }

val headers = listOf(
    DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 200.dp),
    DataTableHeader(key = "role", title = "Role", value = { it.role }, width = 160.dp,
        editable = true,
    ),
)

DataTable(
    items = people,
    headers = headers,
    itemKey = { it.id },
    onCellEdit = { edit ->
        people = people.map { person ->
            if (person.id == edit.rowKey && edit.columnKey == "role") {
                person.copy(role = edit.newText)
            } else {
                person
            }
        }
    },
)
```

That is the whole minimum: `editable = true` on the column, and `onCellEdit` to apply the result.

The `CellEdit<T>` it hands you carries five things:

`item`

:   The row that was edited. Matching on `rowKey` as above keeps the example self-contained, but
    the row itself is right here when your update needs more than its key.

`rowKey` / `columnKey`

:   Which cell it was, named by the key `itemKey` produced and the column's `key` — so an edit
    reported while the table is sorted, filtered, or paged still points at the right row.

`oldText` / `newText`

:   The value before and after, both `String`. Parsing `newText` back into your model's type is
    your job; see [Validation](#validation) for the guard that makes it safe.

!!! note "Declaring an editable column turns on cell navigation"
    Editing needs a cell cursor — an editor you cannot reach with the keyboard is no use. So a
    single `editable` column implies `cellNavigation = true`, described below. Tables with no
    editable columns are untouched and keep navigating by row.

## Opening and closing an editor

| Key | Action |
|-----|--------|
| ++enter++ or ++f2++ | Open the editor on the focused cell |
| any printable character | Open the editor, replacing the value with what you typed |
| double-click | Open the editor on that cell |
| ++enter++ | Commit and move down one row — the data-entry move |
| ++tab++ / ++shift+tab++ | Commit and move to the next / previous cell |
| ++escape++ | Cancel, leaving the row untouched |

Committing a value you did not change reports nothing, so opening and closing a cell never marks a
row dirty.

++space++ is *not* a shortcut into the editor — it stays bound to row selection. Press ++f2++
first if you need to type a value that begins with a space.

!!! tip "Double-click is yours if you want it"
    Supplying `onRowDoubleClick` claims the gesture: the table stops opening editors on
    double-click and calls you instead. ++enter++, ++f2++, and type-to-edit still work, so a
    detail dialog and in-place editing can coexist in one table.

## Formatting versus editing

`value` is what the cell *displays*. When that is formatted — a currency symbol, thousands
separators, a rendered date — it is the wrong thing to seed an editor with, because the user has
to clean it up before typing. `editValue` supplies the raw, editable form:

```kotlin
DataTableHeader<Invoice>(
    key = "amount",
    title = "Amount",
    width = 140.dp,
    align = TextAlign.End,
    value = { "$${"%,.2f".format(it.amount)}" },   // $1,250.00
    editable = true,
    editValue = { it.amount.toString() },          // 1250.0
)
```

`CellEdit.oldText` is reported from `editValue` too, so an unchanged commit is correctly
recognised as a non-edit.

## Validation

`validateEdit` returns an error message to reject a value, or `null` to accept it. A rejected
commit keeps the editor open, puts the message on `state.editError`, and outlines the cell in
`colors.invalidCellBorder`. Nothing reaches `onCellEdit`.

```kotlin
DataTableHeader<OrderLine>(
    key = "quantity",
    title = "Quantity",
    value = { it.quantity },
    width = 120.dp,
    editable = true,
    validateEdit = { line, text ->
        val quantity = text.toIntOrNull()
        when {
            quantity == null -> "Enter a whole number"
            quantity < 0 -> "Quantity cannot be negative"
            quantity > line.available -> "Only ${line.available} in stock"
            else -> null
        }
    },
)
```

`onCellEdit` hands you a `String`, so parsing it back into your model's type is your job. Pair
every typed column with a `validateEdit` that proves the parse succeeds, and the `toInt()` in your
`onCellEdit` can never throw.

The message is on the state, so you choose where it appears — a banner, a status bar, a tooltip:

```kotlin
tableState.editError?.let { message ->
    Text(message, color = MaterialTheme.colorScheme.error)
}
```

## Custom editors

A column whose values come from a fixed set deserves a picker, not free text. `editorContent`
replaces the built-in text field with any composable, handed the row and a `CellEditController`:

```kotlin
DataTableHeader<Employee>(
    key = "department",
    title = "Department",
    value = { it.department },
    width = 170.dp,
    editable = true,
    editorContent = { employee, controller ->
        var expanded by remember { mutableStateOf(true) }
        Box {
            Text(employee.department)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false; controller.cancel() },
            ) {
                departments.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { expanded = false; controller.commit(option) },
                    )
                }
            }
        }
    },
)
```

`controller.commit(text)` runs the column's `validateEdit` and reports through `onCellEdit` just
as the built-in editor does — validation cannot be bypassed by supplying your own editor.
`controller.initialText` is what the cell holds (or what the user typed to open it), and
`controller.error` carries a rejection message so the editor can show it.

A custom editor is expected to take keyboard focus itself. It commits in place; unlike ++enter++
and ++tab++ in the built-in field, it does not move the cursor afterwards.

## Programmatic editing

```kotlin
tableState.startEditing(rowKey = person.id, columnKey = "role")
tableState.cancelEditing()

val editing: CellPosition? = tableState.editingCell
val isEditing: Boolean = tableState.isEditing
```

`startEditing` moves cell focus and opens the editor, exactly as ++f2++ does. Use it to drop the
user straight into the first cell of a row they have just added.

## Cell navigation

`cellNavigation = true` moves keyboard focus down from the row to the cell, without any column
being editable. The focused cell draws a cursor, clicking a cell focuses it, and:

| Key | Action |
|-----|--------|
| ++arrow-left++ / ++arrow-right++ | Move a column |
| ++arrow-up++ / ++arrow-down++ | Move a row, staying in the same column |
| ++home++ / ++end++ | First / last column of the row |
| ++ctrl+home++ / ++ctrl+end++ | First / last cell of the table |
| ++page-up++ / ++page-down++ | Move a screenful of rows |
| ++tab++ / ++shift+tab++ | Next / previous cell, wrapping across rows |

Moving onto a column that is scrolled out of view scrolls it in, so ++arrow-right++ keeps working
across a sixty-column grid.

++tab++ deliberately falls through at the very first and very last cell rather than wrapping
around, so it still gets the user out of the table.

Holding ++shift++ with any of these keys extends a selected block of cells instead of moving the
cursor alone, and ++ctrl+c++ copies it — see
[Range selection](selection.md#cell-range-selection).

Focus is held as a row key and a column key, not as coordinates — `state.focusedCell` is a
`CellPosition(rowKey, columnKey)`. It stays on the same cell across a re-sort, and a cell that
scrolls off a page simply draws nothing rather than the cursor drifting onto whichever row has
taken its place.

```kotlin
val cell = tableState.focusedCell          // CellPosition?, null when only a row is focused

tableState.focusCell(person.id, "role")    // move it
tableState.focusCell(null, null)           // clear it
```

Like `focusRow`, `focusCell` does not scroll. `state.revealFocusedCell()` is a `suspend` function
that scrolls both axes to bring it into view.

## Limitations

- **Editing is one cell at a time.** A range can be selected and copied — see
  [Selection](selection.md#cell-range-selection) — but not edited or pasted into as a block.
- **No row-level commit.** Each cell commits on its own; there is no "editing row" that validates
  across columns or rolls back as a unit.
- **No undo.** You hold the data, so undo is a stack of `CellEdit`s you keep yourself — every
  commit is reported with both `oldText` and `newText` for exactly that.
- **The editor is single-line.** A multi-line editor means `editorContent`.
