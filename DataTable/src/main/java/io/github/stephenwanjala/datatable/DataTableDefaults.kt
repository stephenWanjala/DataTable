package io.github.stephenwanjala.datatable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Colors used throughout the [DataTable].
 *
 * All colors have sensible defaults that work without any theming framework.
 * Pass a custom instance to [DataTable] to match your application's palette.
 *
 * Text color is not here — it lives on the styles in [DataTableTextStyles] — so a dark palette
 * means overriding both.
 *
 * @property container Background behind the whole table.
 * @property header Background of the header row and of the footer.
 * @property divider Row separators, the freeze boundary, and grouped-header underlines.
 * @property selectedRow Background of a selected row. Usually translucent, so it reads on top of
 *                       [rowAlternate].
 * @property expandedRow Background behind the content of an expanded row.
 * @property onSurface Primary content color. Paints the caret in an open cell editor, and is
 *                     otherwise there for callers building custom cells.
 * @property onSurfaceSecondary Muted content color, available for callers building custom cells.
 * @property checkboxChecked Fill and border of a checked selection checkbox.
 * @property checkboxUnchecked Border of an unchecked selection checkbox.
 * @property checkboxCheckmark The checkmark drawn inside a checked checkbox.
 * @property iconTint Sort arrows, expand chevrons, and pagination arrows.
 * @property disabledContent Pagination arrows that cannot be used from the current page.
 * @property rowAlternate Background of every other row. `Color.Transparent`, the default, turns
 *                        striping off.
 * @property hoveredRow Background of the row under the pointer, also used for icon-button hover
 *                      highlights in the footer.
 * @property focusedRowBorder Marker drawn down the leading edge of the keyboard-focused row.
 * @property focusedCellBorder Outline drawn around the keyboard-focused cell, once cell-level
 *                             navigation is on. Drawn on top of the row's own focus marker.
 * @property editingCell Background behind an open cell editor, so the field reads as a field
 *                       rather than as text that happens to have a caret in it.
 * @property invalidCellBorder Outline replacing [focusedCellBorder] while the value in an open
 *                             editor has been rejected by the column's `validateEdit`.
 * @property draggedColumn Wash over the header being dragged to a new position, so it is clear
 *                        which column is on the move. Translucent, so the title still reads.
 * @property columnDropIndicator The line drawn down the edge a dragged column would land against.
 * @property filterRow Background of the filter row, under the header. Set a shade off
 *                     [header] so the two read as separate rows rather than one tall band.
 * @property filterField Background of a filter field itself, which sits on [filterRow].
 * @property selectedCell Wash over cells inside a selected range. Translucent, so row striping
 *                        and row selection still read through it. The cell the cursor is on is
 *                        left unwashed, the way a spreadsheet leaves its active cell.
 */
@Immutable
data class DataTableColors(
    val container: Color = Color(0xFFFAFAFA),
    val header: Color = Color(0xFFF0F0F0),
    val divider: Color = Color(0xFFDDDDDD),
    val selectedRow: Color = Color(0x4D1976D2),
    val expandedRow: Color = Color(0x80F0F0F0),
    val onSurface: Color = Color(0xFF1C1C1C),
    val onSurfaceSecondary: Color = Color(0xFF757575),
    val checkboxChecked: Color = Color(0xFF1976D2),
    val checkboxUnchecked: Color = Color(0xFF757575),
    val checkboxCheckmark: Color = Color.White,
    val iconTint: Color = Color(0xFF616161),
    val disabledContent: Color = Color(0xFFBDBDBD),
    val rowAlternate: Color = Color.Transparent,
    val hoveredRow: Color = Color(0x1A000000),
    val focusedRowBorder: Color = Color(0xFF1976D2),
    val focusedCellBorder: Color = Color(0xFF1976D2),
    val editingCell: Color = Color(0xFFFFFFFF),
    val invalidCellBorder: Color = Color(0xFFD32F2F),
    val selectedCell: Color = Color(0x331976D2),
    val draggedColumn: Color = Color(0x1F1976D2),
    val columnDropIndicator: Color = Color(0xFF1976D2),
    val filterRow: Color = Color(0xFFF7F7F7),
    val filterField: Color = Color(0xFFFFFFFF),
)

/**
 * Text styles used throughout the [DataTable].
 *
 * Defaults are plain styles at typical sizes - no Material typography required.
 *
 * Each style carries its own color, which is what makes these the other half of a custom
 * palette alongside [DataTableColors].
 *
 * @property headerCell Column titles, including grouped-header labels.
 * @property bodyCell Cell text rendered from a column's `value`. Columns with a `cellContent`
 *                    composable style themselves and ignore this.
 * @property footer The row-count and range readouts in the footer.
 * @property loading The built-in loading indicator, when no `loadingContent` is supplied.
 * @property noData The built-in empty state, when no `noDataContent` is supplied.
 * @property pagination Page controls: the page indicator, the rows-per-page label, and the
 *                      options in its menu.
 * @property cellEditor Text inside an open cell editor. Matches [bodyCell] by default so a cell
 *                      does not jump as it goes into edit mode.
 * @property filterField Text typed into a column filter, and its placeholder. A size down from
 *                       [bodyCell], so the filter row reads as a control strip rather than as
 *                       another row of data.
 */
@Immutable
data class DataTableTextStyles(
    val headerCell: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1C1C1C),
    ),
    val bodyCell: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
    val footer: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
    val loading: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
    val noData: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0x991C1C1C),
    ),
    val pagination: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
    val cellEditor: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
    val filterField: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
)

/**
 * Factory functions for [DataTableColors] and [DataTableTextStyles].
 */
object DataTableDefaults {
    /**
     * The height `DataTable` gives a row when it is not told one: enough for a single line of
     * body text at [density]'s padding.
     *
     * Rows being a uniform, known height is what lets the table cull the columns that scroll out
     * of sight — a row that sizes to its content changes height when the tall cell is the culled
     * one, so `DataTable` will not cull for a table whose `rowHeight` is `null`.
     *
     * This assumes the default `bodyCell` style. A larger font, two lines of text, or a
     * `cellContent` holding a chip or an avatar needs a height of its own — pass one to
     * `DataTable` rather than deriving it from here.
     */
    fun rowHeight(density: DataTableDensity): Dp = density.verticalPadding * 2 + SINGLE_LINE

    /** Room for one line of the default 14sp `bodyCell` style, with a little slack. */
    private val SINGLE_LINE = 20.dp

    /**
     * Creates a [DataTableColors], remembered against its arguments.
     *
     * Every parameter defaults to the value the table uses when given no palette at all, so
     * overriding one color leaves the rest alone:
     *
     * ```
     * colors = DataTableDefaults.colors(rowAlternate = Color(0xFFF5F5F5))
     * ```
     *
     * See [DataTableColors] for what each color applies to.
     */
    @Composable
    fun colors(
        container: Color = Color(0xFFFAFAFA),
        header: Color = Color(0xFFF0F0F0),
        divider: Color = Color(0xFFDDDDDD),
        selectedRow: Color = Color(0x4D1976D2),
        expandedRow: Color = Color(0x80F0F0F0),
        onSurface: Color = Color(0xFF1C1C1C),
        onSurfaceSecondary: Color = Color(0xFF757575),
        checkboxChecked: Color = Color(0xFF1976D2),
        checkboxUnchecked: Color = Color(0xFF757575),
        checkboxCheckmark: Color = Color.White,
        iconTint: Color = Color(0xFF616161),
        disabledContent: Color = Color(0xFFBDBDBD),
        rowAlternate: Color = Color.Transparent,
        hoveredRow: Color = Color(0x1A000000),
        focusedRowBorder: Color = Color(0xFF1976D2),
        focusedCellBorder: Color = Color(0xFF1976D2),
        editingCell: Color = Color(0xFFFFFFFF),
        invalidCellBorder: Color = Color(0xFFD32F2F),
        selectedCell: Color = Color(0x331976D2),
        draggedColumn: Color = Color(0x1F1976D2),
        columnDropIndicator: Color = Color(0xFF1976D2),
        filterRow: Color = Color(0xFFF7F7F7),
        filterField: Color = Color(0xFFFFFFFF),
    ): DataTableColors = remember(
        container, header, divider, selectedRow, expandedRow,
        onSurface, onSurfaceSecondary, checkboxChecked, checkboxUnchecked,
        checkboxCheckmark, iconTint, disabledContent, rowAlternate,
        hoveredRow, focusedRowBorder, focusedCellBorder, editingCell,
        invalidCellBorder, selectedCell, draggedColumn, columnDropIndicator,
        filterRow, filterField
    ) {
        DataTableColors(
            container = container,
            header = header,
            divider = divider,
            selectedRow = selectedRow,
            expandedRow = expandedRow,
            onSurface = onSurface,
            onSurfaceSecondary = onSurfaceSecondary,
            checkboxChecked = checkboxChecked,
            checkboxUnchecked = checkboxUnchecked,
            checkboxCheckmark = checkboxCheckmark,
            iconTint = iconTint,
            disabledContent = disabledContent,
            rowAlternate = rowAlternate,
            hoveredRow = hoveredRow,
            focusedRowBorder = focusedRowBorder,
            focusedCellBorder = focusedCellBorder,
            editingCell = editingCell,
            invalidCellBorder = invalidCellBorder,
            selectedCell = selectedCell,
            draggedColumn = draggedColumn,
            columnDropIndicator = columnDropIndicator,
            filterRow = filterRow,
            filterField = filterField,
        )
    }

    /**
     * Creates a [DataTableTextStyles], remembered against its arguments.
     *
     * Every parameter defaults to the plain style the table uses when given none, so overriding
     * one leaves the rest alone. Text color lives on these styles rather than in
     * [DataTableColors]:
     *
     * ```
     * textStyles = DataTableDefaults.textStyles(
     *     bodyCell = MaterialTheme.typography.bodyMedium,
     * )
     * ```
     */
    @Composable
    fun textStyles(
        headerCell: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1C)),
        bodyCell: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
        footer: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
        loading: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
        noData: TextStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Color(0x991C1C1C)),
        pagination: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
        cellEditor: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
        filterField: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
    ): DataTableTextStyles = remember(
        headerCell, bodyCell, footer, loading, noData, pagination, cellEditor, filterField,
    ) {
        DataTableTextStyles(
            headerCell = headerCell,
            bodyCell = bodyCell,
            footer = footer,
            loading = loading,
            noData = noData,
            pagination = pagination,
            cellEditor = cellEditor,
            filterField = filterField,
        )
    }
}
