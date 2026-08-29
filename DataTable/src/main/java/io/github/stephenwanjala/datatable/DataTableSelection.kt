package io.github.stephenwanjala.datatable

import androidx.compose.runtime.Immutable
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * A selected block of cells, from the anchor corner to the cursor.
 *
 * Either corner may be the top-left one — [anchor] is simply where the selection started and
 * [focus] is where the cursor is now, so dragging up and left is as ordinary as dragging down
 * and right. Use `DataTableState.isCellInRange` to test membership rather than comparing corners.
 *
 * @property anchor The corner that stays put while the selection is extended.
 * @property focus The corner the cursor is on, which is also `DataTableState.focusedCell`.
 */
@Immutable
data class CellRange(
    val anchor: CellPosition,
    val focus: CellPosition,
)

/**
 * What a copy covers: the rows, the columns, and the text of every cell where they cross.
 *
 * Handed to `DataTable`'s `onCopy`. Both [rows] and [columns] are in display order, so a caller
 * writing CSV, XLSX, or anything else gets the grid exactly as the user sees it — sorted, paged,
 * and with hidden columns already gone.
 *
 * Supplying `onCopy` *replaces* the table's own clipboard write rather than running alongside it,
 * so a handler that only logs or displays the selection leaves the clipboard untouched. Call
 * [copyToSystemClipboard] from inside it to do both.
 *
 * @property rows The selected row items, in display order.
 * @property columns The selected leaf columns, in display order.
 * @property cells Cell text, row-major: `cells[row][column]` lines up with [rows] and [columns].
 *                 Text is what the cell shows — each column's `value` put through its `format` —
 *                 so a copy carries the same money symbols and dates the user was looking at. A
 *                 column that renders only through `cellContent` copies as empty; give it a
 *                 `value` and it becomes sortable and copyable at once. The raw values are still
 *                 within reach for a handler that wants them: [columns] carries every column's
 *                 own `value` extractor, and [rows] the items to run it against.
 */
@Immutable
data class ClipboardSelection<T>(
    val rows: List<T>,
    val columns: List<DataTableHeader<T>>,
    val cells: List<List<String>>,
) {
    /**
     * Renders the selection as tab-separated text, which is what spreadsheets paste natively.
     *
     * Values containing a tab, a newline, or a double quote are quoted and their quotes doubled,
     * so a cell holding free-text notes cannot silently split one row into several on paste.
     *
     * @param includeHeader Whether to lead with a row of column titles. Off by default, matching
     *                      what a spreadsheet puts on the clipboard when you copy a range.
     */
    fun toTabSeparated(includeHeader: Boolean = false): String = buildString {
        if (includeHeader) {
            columns.joinTo(this, separator = "\t") { escapeForClipboard(it.title) }
            append('\n')
        }
        cells.forEachIndexed { index, row ->
            if (index > 0) append('\n')
            row.joinTo(this, separator = "\t") { escapeForClipboard(it) }
        }
    }
}

/**
 * Puts this selection on the system clipboard as tab-separated text, reporting whether it got
 * there.
 *
 * This is what `DataTable` does for you when no `onCopy` is supplied. Call it from inside an
 * `onCopy` handler that wants to observe or log a copy *and* still perform it:
 *
 * ```
 * onCopy = { selection ->
 *     auditLog.record(selection.rows.size)
 *     selection.copyToSystemClipboard()
 * }
 * ```
 *
 * @param includeHeader Whether to lead with a row of column titles, as in [toTabSeparated].
 */
fun ClipboardSelection<*>.copyToSystemClipboard(includeHeader: Boolean = false): Boolean =
    copyToSystemClipboard(toTabSeparated(includeHeader))

/**
 * Quotes a value that would otherwise break the row-and-column structure of the clipboard text.
 */
private fun escapeForClipboard(value: String): String =
    if (value.any { it == '\t' || it == '\n' || it == '\r' || it == '"' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

/**
 * Puts [text] on the system clipboard, reporting whether it got there.
 *
 * Exposed for `onCopy` handlers that build their own format — CSV, or a single column joined by
 * commas — and still want it on the clipboard.
 *
 * Returns false rather than throwing when there is no clipboard to write to: a headless JVM, or a
 * desktop session where another application is holding it. A failed copy should leave the table
 * working, not take the application down.
 */
fun copyToSystemClipboard(text: String): Boolean =
    try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        true
    } catch (_: HeadlessException) {
        false
    } catch (_: IllegalStateException) {
        false
    }
