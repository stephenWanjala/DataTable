package io.github.stephenwanjala.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Identifies a single cell — the intersection of a row and a column.
 *
 * The row is named by the key `DataTable`'s `itemKey` produced for it and the column by its
 * [DataTableHeader.key], so a position survives sorting, filtering, and paging rather than
 * pointing at whatever now sits at a given coordinate.
 *
 * @property rowKey Key of the row, as produced by `DataTable`'s `itemKey`.
 * @property columnKey [DataTableHeader.key] of the column.
 */
@Immutable
data class CellPosition(
    val rowKey: Any,
    val columnKey: String,
)

/**
 * A committed cell edit, handed to `DataTable`'s `onCellEdit`.
 *
 * The table never mutates your data: it reports the edit and waits for you to apply it and pass
 * the updated `items` back. That keeps a single source of truth in your model, which is what
 * makes undo, dirty tracking, and server round-trips possible at all.
 *
 * Only edits that actually change the text are reported — committing a cell you did not alter
 * fires nothing.
 *
 * @property item The row item that was edited.
 * @property rowKey Key of that row, as produced by `DataTable`'s `itemKey`.
 * @property columnKey [DataTableHeader.key] of the edited column.
 * @property oldText The cell's text before the edit, as the editor was seeded with it.
 * @property newText The text the user committed. Parsing it back into your model's type is the
 *                   caller's job — pair it with [DataTableHeader.validateEdit] so an unparseable
 *                   value never reaches here.
 */
@Immutable
data class CellEdit<T>(
    val item: T,
    val rowKey: Any,
    val columnKey: String,
    val oldText: String,
    val newText: String,
)

/**
 * Handle given to a column's [DataTableHeader.editorContent], for editors that are not plain text.
 *
 * A custom editor renders whatever control it likes — a dropdown, a date picker, a lookup field —
 * and calls [commit] or [cancel] when the user is done. It is composed in place of the cell's
 * normal content and is expected to take keyboard focus itself.
 *
 * @property initialText The text the editor should start with: the cell's current value, or the
 *                       character the user typed if the edit began by typing over the cell.
 * @property error Message from [DataTableHeader.validateEdit] rejecting the last attempted
 *                 commit, or `null` when nothing has been rejected. Show it, and stay open.
 */
@Stable
class CellEditController internal constructor(
    val initialText: String,
    val error: String?,
    private val onCommitRequest: (String) -> Unit,
    private val onCancelRequest: () -> Unit,
) {
    /**
     * Validates [text] and, if it passes, closes the editor and reports the edit through
     * `DataTable`'s `onCellEdit`.
     *
     * A rejected value leaves the editor open with [error] set on the next controller instance.
     */
    fun commit(text: String) {
        onCommitRequest(text)
    }

    /**
     * Abandons the edit, leaving the row untouched and returning keyboard focus to the table.
     */
    fun cancel() {
        onCancelRequest()
    }
}

/**
 * Where focus lands after an editor commits, which is what distinguishes the keys that close it.
 */
internal enum class EditMove {
    /** Stay on the cell that was just edited. What [CellEditController.commit] does. */
    STAY,

    /** The same column on the next row down — Enter, the data-entry-down-a-column move. */
    DOWN,

    /** The next cell in reading order — Tab. */
    NEXT,

    /** The previous cell in reading order — Shift+Tab. */
    PREVIOUS,
}

/**
 * The built-in cell editor: a single-line text field used when a column is editable but supplies
 * no [DataTableHeader.editorContent].
 *
 * The text starts fully selected, so typing replaces the old value the way it does in a
 * spreadsheet — except when the edit began by typing a character, where the caret sits after
 * what was already typed.
 *
 * @param selectAll Whether to select the whole value on open. False when [initialText] is a
 *                  character the user just typed.
 * @param onCommit Invoked with the text and where focus should go afterwards. Returns true when
 *                 the value was accepted; a false return means validation rejected it and the
 *                 editor stays open.
 */
@Composable
internal fun DefaultCellEditor(
    initialText: String,
    selectAll: Boolean,
    textStyle: TextStyle,
    colors: DataTableColors,
    onCommit: (text: String, move: EditMove) -> Boolean,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // Seeded text — the user typed over the cell — leaves the caret after what they typed.
    // Otherwise the whole value is selected so the first keystroke replaces it.
    var value by remember(initialText, selectAll) {
        val selection =
            if (selectAll) TextRange(0, initialText.length) else TextRange(initialText.length)
        mutableStateOf(TextFieldValue(initialText, selection))
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.editingCell)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        onCommit(value.text, EditMove.DOWN); true
                    }

                    Key.Tab -> {
                        val move = if (event.isShiftPressed) EditMove.PREVIOUS else EditMove.NEXT
                        onCommit(value.text, move); true
                    }

                    Key.Escape -> {
                        onCancel(); true
                    }

                    else -> false
                }
            },
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(colors.onSurface),
    )
}
