package io.github.stephenwanjala.datatable

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Adds keyboard navigation to the DataTable container.
 *
 * Row navigation, always on:
 * - Arrow Up/Down: move focused row
 * - Enter: trigger row click on the focused row
 * - Space: toggle selection on the focused row
 * - Home/End: focus first/last row
 *
 * Grid navigation, once [gridNavigation] is on, layers cell-level movement over that:
 * - Arrow Left/Right: move focused column
 * - Home/End: first/last column of the focused row; with Ctrl, first/last cell of the table
 * - Page Up/Down: move a screenful of rows
 * - Tab/Shift+Tab: next/previous cell, wrapping across rows. At the very first and very last
 *   cell it falls through instead, so Tab still gets the user out of the table.
 * - Shift with any of the movement keys above: extend the selected block of cells rather than
 *   moving the cursor alone. Moving without Shift collapses it again.
 * - Ctrl+A: select every cell.
 * - Ctrl+C: copy the selection. Also bound during plain row navigation, where it copies the
 *   selected rows — but only ever consumed when there is something to copy, so an application
 *   with its own Ctrl+C keeps it on an empty table.
 * - Enter or F2 on an editable column: open the editor. Enter on any other column keeps
 *   triggering the row click.
 * - Any printable character on an editable column: open the editor seeded with that character,
 *   replacing the cell's value the way a spreadsheet does. Space is excluded — it stays bound to
 *   selection, which is worth more than saving one keystroke on a value that starts with a space.
 *
 * While an editor is open this handler stands down entirely and lets the editor own the keyboard.
 *
 * Focus is stored on [DataTableState] as a row key and a column key, not as positions, so it
 * stays on the same cell when the table is re-sorted. Navigation resolves those keys back to
 * positions against `state.rowKeys` and `state.columnKeys`, which `DataTable` republishes on
 * every composition.
 *
 * @param gridNavigation Whether cell-level movement and editing shortcuts are active.
 * @param isColumnEditable Whether the column with the given key accepts edits.
 * @param onCopy Copies the current selection, returning false when there was nothing to copy.
 * @param onRowClick Invoked with the focused row's position in `state.rowKeys`.
 * @param onToggleSelection Invoked with the focused row's position in `state.rowKeys`.
 */
internal fun Modifier.dataTableKeyboardNavigation(
    state: DataTableState,
    scope: CoroutineScope,
    gridNavigation: Boolean = false,
    isColumnEditable: (String) -> Boolean = { false },
    onCopy: (() -> Boolean)? = null,
    onRowClick: ((Int) -> Unit)? = null,
    onToggleSelection: ((Int) -> Unit)? = null,
): Modifier = this.onPreviewKeyEvent { keyEvent ->
    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

    // An open editor owns every key. Without this the container would swallow the arrow keys
    // before the text field ever saw them, and the caret could not move.
    if (state.isEditing) return@onPreviewKeyEvent false

    val rowKeys = state.rowKeys
    if (rowKeys.isEmpty()) return@onPreviewKeyEvent false

    val columnKeys = state.columnKeys

    // -1 when nothing is focused, or when the focused row is no longer displayed
    // (filtered out, or on another page). Same for the column when it has been hidden.
    val row = state.focusedRowPosition()
    val column = state.focusedColumnPosition()

    fun moveTo(rowPosition: Int, columnPosition: Int?, extendRange: Boolean = false): Boolean {
        state.focusPosition(rowPosition, columnPosition, extendRange)
        scope.launch { state.revealFocusedCell() }
        return true
    }

    fun beginEdit(seed: String?): Boolean {
        val columnKey = columnKeys.getOrNull(if (column < 0) 0 else column) ?: return false
        if (!isColumnEditable(columnKey)) return false
        val rowKey = rowKeys.getOrNull(if (row < 0) 0 else row) ?: return false
        state.focusCell(rowKey, columnKey)
        state.editSeedText = seed
        state.editError = null
        state.isEditing = true
        return true
    }

    // Copy is bound in both modes: a table navigated by row still has selected rows worth
    // putting on the clipboard.
    if (keyEvent.isCtrlPressed && keyEvent.key == Key.C) {
        return@onPreviewKeyEvent onCopy?.invoke() ?: false
    }

    if (!gridNavigation) {
        return@onPreviewKeyEvent when (keyEvent.key) {
            Key.DirectionDown -> moveTo(if (row < 0) 0 else row + 1, null)
            Key.DirectionUp -> moveTo(if (row < 0) 0 else row - 1, null)
            Key.MoveHome -> moveTo(0, null)
            Key.MoveEnd -> moveTo(rowKeys.lastIndex, null)
            Key.Enter -> {
                if (row >= 0) onRowClick?.invoke(row)
                true
            }

            Key.Spacebar -> {
                if (row >= 0) onToggleSelection?.invoke(row)
                true
            }

            else -> false
        }
    }

    // A screenful, floored at one so a table shorter than the viewport still moves.
    val pageSize = state.lazyListState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val safeRow = if (row < 0) 0 else row
    val safeColumn = if (column < 0) 0 else column

    // Shift turns every movement key into an extension of the selected block. Tab is the one
    // exception — Shift already means "backwards" there.
    val extend = keyEvent.isShiftPressed

    when (keyEvent.key) {
        Key.DirectionDown -> moveTo(if (row < 0) 0 else row + 1, safeColumn, extend)

        Key.DirectionUp -> moveTo(if (row < 0) 0 else row - 1, safeColumn, extend)

        Key.DirectionRight -> moveTo(safeRow, if (column < 0) 0 else column + 1, extend)

        Key.DirectionLeft -> moveTo(safeRow, if (column < 0) 0 else column - 1, extend)

        Key.PageDown -> moveTo(safeRow + pageSize, safeColumn, extend)

        Key.PageUp -> moveTo(safeRow - pageSize, safeColumn, extend)

        Key.MoveHome ->
            if (keyEvent.isCtrlPressed) moveTo(0, 0, extend) else moveTo(safeRow, 0, extend)

        Key.MoveEnd ->
            if (keyEvent.isCtrlPressed) moveTo(rowKeys.lastIndex, columnKeys.lastIndex, extend)
            else moveTo(safeRow, columnKeys.lastIndex, extend)

        Key.A -> if (keyEvent.isCtrlPressed && columnKeys.isNotEmpty()) {
            state.selectRange(
                anchor = CellPosition(rowKeys.first(), columnKeys.first()),
                focus = CellPosition(rowKeys.last(), columnKeys.last()),
            )
            true
        } else {
            val typed = keyEvent.printableCharacter()
            if (typed != null) beginEdit(seed = typed) else false
        }

        // Falls through at the very first and very last cell, so Tab can still leave the table.
        Key.Tab -> if (state.advanceCellFocus(backwards = keyEvent.isShiftPressed)) {
            scope.launch { state.revealFocusedCell() }
            true
        } else {
            false
        }

        Key.F2 -> beginEdit(seed = null)

        Key.Enter, Key.NumPadEnter -> {
            if (!beginEdit(seed = null) && row >= 0) onRowClick?.invoke(row)
            true
        }

        Key.Spacebar -> {
            if (row >= 0) onToggleSelection?.invoke(row)
            true
        }

        else -> {
            val typed = keyEvent.printableCharacter()
            if (typed != null) beginEdit(seed = typed) else false
        }
    }
}

/**
 * The character this key event would type, or `null` when it is not a plain character press.
 *
 * Modifier chords are excluded — the table handles the few it binds itself, and the rest belong
 * to the host application — and so is Space, which the table binds to selection.
 */
private fun KeyEvent.printableCharacter(): String? {
    if (isCtrlPressed || isAltPressed || isMetaPressed) return null
    if (key == Key.Spacebar) return null
    val codePoint = utf16CodePoint
    // Keys that type nothing — the function keys, Insert, Delete — report AWT's CHAR_UNDEFINED,
    // which is an unassigned code point rather than a control character. Without the isDefined
    // check F5 would open an editor seeded with U+FFFF.
    if (!Character.isDefined(codePoint) || Character.isISOControl(codePoint)) return null
    return String(Character.toChars(codePoint))
}
