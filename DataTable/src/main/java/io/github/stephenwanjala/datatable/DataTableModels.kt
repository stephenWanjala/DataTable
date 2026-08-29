package io.github.stephenwanjala.datatable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Describes a column in the `DataTable`.
 *
 * @param key Stable unique identifier for this column. Also used for sorting.
 * @param title Text label shown in the default header when `headerContent` is not provided.
 * @param value Extractor invoked for each row item to produce a displayable value.
 * @param sortable Whether this column supports sorting via the built-in header interactions.
 * @param width Fixed width for the column. If `null`, the column is laid out with weight to fill space.
 * @param align Horizontal alignment for header text and default cell text.
 * @param fixed When true, the column is frozen (pinned) to the left and does not scroll horizontally.
 *              Frozen columns must have an explicit [width]; `DataTable` throws if one does not.
 * @param visible When false, the column is excluded from rendering. Defaults to true.
 * @param maxLines Maximum lines for default text rendering. Defaults to [Int.MAX_VALUE].
 * @param overflow Text overflow strategy for default text rendering.
 * @param comparator Optional custom comparator for sorting. When provided, takes precedence over
 *                   the default [Comparable]-based sort.
 * @param children Optional nested headers. When provided, this header becomes a group: it is
 *                 drawn as a band above its children, spanning them, and only the leaf headers
 *                 render as actual columns. A group's own [value], [width], and sorting settings
 *                 are ignored — it takes its width from the leaves beneath it, which must be
 *                 either all fixed-width or all weighted.
 * @param headerContent Optional composable to fully customize the header cell for this column.
 * @param cellContent Optional composable to fully customize each body cell for this column.
 * @param editable Whether cells in this column can be edited in place. Declaring any editable
 *                 column turns on `DataTable`'s cell-level focus and grid keyboard navigation,
 *                 and requires an `onCellEdit` callback for the edit to go anywhere — the table
 *                 reports edits, it never mutates your items.
 * @param editValue Text the editor opens with. Defaults to [value]'s result rendered with
 *                  `toString()` — the raw value, never the [format]ted one, since formatted text
 *                  is rarely text you can type back. Set this where even the raw form is not what
 *                  you want typed: `92000.0` for a salary held as a `Double`, or an enum's
 *                  `name`.
 * @param validateEdit Checks a value the user is trying to commit, returning an error message to
 *                     reject it or `null` to accept. A rejected commit keeps the editor open with
 *                     the message on `DataTableState.editError`. This is the guard that stops
 *                     unparseable text ever reaching `onCellEdit`.
 * @param editorContent Optional composable replacing the built-in text field — a dropdown, a date
 *                      picker, a lookup. It is handed the row and a [CellEditController] to
 *                      commit or cancel with, and is expected to take keyboard focus itself.
 * @param format Turns the value [value] extracted into the text the cell shows. Formatting here
 *               rather than inside [value] is what keeps a column's raw value raw: `{ it.salary }`
 *               reads `$92,000` and still sorts numerically, with no [comparator] to make up for
 *               a value that had become a string. Clipboard copy takes the formatted text — what
 *               you see is what you copy — while an editor still opens on the raw value, or on
 *               [editValue]. Ignored when [cellContent] draws the cell itself, though copy still
 *               uses it. [DataTableFormatters] has ready-made ones for money, numbers,
 *               percentages, dates, and booleans.
 */
@Immutable
data class DataTableHeader<T>(
    val key: String,
    val title: String = "",
    val value: ((T) -> Any?)? = null,
    val sortable: Boolean = true,
    val width: Dp? = null,
    val align: TextAlign = TextAlign.Start,
    val fixed: Boolean = false,
    val visible: Boolean = true,
    val maxLines: Int = Int.MAX_VALUE,
    val overflow: TextOverflow = TextOverflow.Clip,
    val comparator: Comparator<T>? = null,
    val children: List<DataTableHeader<T>>? = null,
    val headerContent: (@Composable () -> Unit)? = null,
    val cellContent: (@Composable (T) -> Unit)? = null,
    val editable: Boolean = false,
    val editValue: ((T) -> String)? = null,
    val validateEdit: ((T, String) -> String?)? = null,
    val editorContent: (@Composable (T, CellEditController) -> Unit)? = null,
    val format: ((Any?) -> String)? = null,
)

/**
 * Represents the sort order applied to a column.
 */
enum class SortOrder {
    /** Smallest first. The order a column takes on its first header click. */
    ASCENDING,

    /** Largest first. Reached by clicking an already-ascending column. */
    DESCENDING,

    /** Unsorted. Clicking a descending column returns it here, restoring the original order. */
    NONE
}

/**
 * Current sort configuration for a single column.
 *
 * @param key The column `key` to which sorting is applied. Empty means no column selected.
 * @param order The direction of sorting.
 */
@Immutable
data class SortState(
    val key: String = "",
    val order: SortOrder = SortOrder.NONE
)

/**
 * Density presets that control vertical and horizontal padding for rows and headers.
 *
 * Density affects padding only. Row height still grows with cell content, so a column that wraps
 * to two lines is tall at any density — cap it with `maxLines` if you want uniform rows.
 *
 * @property verticalPadding Padding above and below the content of every row and header cell.
 * @property horizontalPadding Padding on each side of a cell's content.
 */
public enum class DataTableDensity(val verticalPadding: Dp, val horizontalPadding: Dp) {
    /** Roomiest preset, and the default. */
    DEFAULT(16.dp, 16.dp),

    /** Slightly tighter vertically, for longer lists that should still read comfortably. */
    COMFORTABLE(12.dp, 16.dp),

    /** Tightest preset, fitting the most rows on screen. Suits data-entry style tables. */
    COMPACT(8.dp, 12.dp)
}

/**
 * Controls how row selection behaves.
 *
 * - [NONE]: No selection UI. Taps still fire [DataTable]'s `onRowClick`.
 * - [SINGLE]: At most one row selected at a time. No select-all checkbox.
 * - [MULTI]: Multiple rows selectable with a select-all checkbox in the header.
 */
enum class SelectionMode {
    /** No selection UI and no selection changes. Row taps still fire `onRowClick`. */
    NONE,

    /** At most one row selected. Tapping the selected row clears it. No select-all checkbox. */
    SINGLE,

    /** Any number of rows selected, with a select-all checkbox in the header. */
    MULTI
}

/**
 * Visible child headers, or `null` when this header is a leaf.
 */
internal fun <T> visibleChildren(header: DataTableHeader<T>): List<DataTableHeader<T>>? =
    header.children?.filter { it.visible }?.takeIf { it.isNotEmpty() }

/**
 * Visible leaf headers beneath [header], or the header itself when it is a leaf.
 */
internal fun <T> leavesOf(header: DataTableHeader<T>): List<DataTableHeader<T>> {
    val children = visibleChildren(header) ?: return listOf(header)
    return children.flatMap { leavesOf(it) }
}

/**
 * Splits top-level headers into the frozen and scrollable sections.
 *
 * A group has to sit wholly on one side of the freeze boundary: half of it would scroll out
 * from under the other half.
 */
internal fun <T> partitionHeaderTree(
    headers: List<DataTableHeader<T>>,
): Pair<List<DataTableHeader<T>>, List<DataTableHeader<T>>> =
    headers.filter { it.visible }.partition { node ->
        val leaves = leavesOf(node)
        val frozen = leaves.count { it.fixed }
        require(frozen == 0 || frozen == leaves.size) {
            "Header group '${node.key}' spans both frozen and scrollable columns. Mark every " +
                "column under it `fixed = true`, or none of them."
        }
        frozen == leaves.size
    }

/**
 * Flattens a potentially nested header tree into a list of visible leaf headers.
 */
internal fun <T> flattenHeaders(headers: List<DataTableHeader<T>>): List<DataTableHeader<T>> {
    val result = mutableListOf<DataTableHeader<T>>()

    fun flatten(headerList: List<DataTableHeader<T>>) {
        headerList.forEach { header ->
            if (!header.visible) return@forEach
            if (header.children.isNullOrEmpty()) {
                result.add(header)
            } else {
                flatten(header.children)
            }
        }
    }

    flatten(headers)
    return result
}
