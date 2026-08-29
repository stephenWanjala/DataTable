package io.github.stephenwanjala.datatable

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp

/**
 * State holder for [DataTable].
 *
 * Provides programmatic access to scroll position, focused row and cell, cell editing, and
 * column widths. Create via [rememberDataTableState].
 */
@Stable
class DataTableState(
    internal val lazyListState: LazyListState,
    internal val horizontalScrollState: ScrollState,
) {
    /**
     * Index of the first visible item in the vertical list.
     */
    val firstVisibleItemIndex: Int get() = lazyListState.firstVisibleItemIndex

    /**
     * Key of the row that currently has keyboard focus, or `null` when no row is focused.
     *
     * Keys are produced by `DataTable`'s `itemKey`, so focus follows its row across sorting,
     * filtering, and paging rather than staying at a fixed position.
     */
    var focusedKey: Any? by mutableStateOf(null)
        internal set

    /**
     * [DataTableHeader.key] of the column that currently has keyboard focus, or `null` when
     * focus is on a whole row rather than a cell.
     *
     * Only meaningful once `DataTable`'s `cellNavigation` is on, or a column is editable.
     */
    var focusedColumnKey: String? by mutableStateOf(null)
        internal set

    /**
     * The focused cell, or `null` while focus is on a whole row or nowhere at all.
     *
     * This is [focusedKey] and [focusedColumnKey] read together — it never disagrees with them.
     */
    val focusedCell: CellPosition?
        get() {
            val row = focusedKey ?: return null
            val column = focusedColumnKey ?: return null
            return CellPosition(row, column)
        }

    /**
     * The cell a range selection is anchored to — the corner that stays put while the cursor
     * moves — or `null` when only a single cell is selected.
     */
    var selectionAnchor: CellPosition? by mutableStateOf(null)
        internal set

    /**
     * The selected block of cells, or `null` when nothing is anchored.
     *
     * The rectangle runs between [selectionAnchor] and [focusedCell]; which of the two is the
     * top-left corner depends on which way the user dragged, so read it through
     * [isCellInRange] rather than comparing corners yourself.
     */
    val selectedRange: CellRange?
        get() {
            val anchor = selectionAnchor ?: return null
            val focus = focusedCell ?: return null
            return CellRange(anchor, focus)
        }

    /**
     * The rows and columns the range covers, held as keys.
     *
     * Storing the resolved keys rather than a pair of corner positions is what makes
     * [isCellInRange] two hash lookups instead of two list scans — at forty columns and a
     * screenful of rows, the difference is a scan per cell on every frame. It also means a
     * re-sort keeps the same cells highlighted rather than sweeping the rectangle across
     * whichever rows moved into those positions.
     */
    internal var rangeRowKeys: Set<Any> by mutableStateOf(emptySet())
        private set
    internal var rangeColumnKeys: Set<String> by mutableStateOf(emptySet())
        private set

    /**
     * Whether one cell falls inside the selected range.
     */
    fun isCellInRange(rowKey: Any, columnKey: String): Boolean =
        rowKey in rangeRowKeys && columnKey in rangeColumnKeys

    /**
     * Whether an editor is currently open. Editing always happens at [focusedCell].
     */
    var isEditing: Boolean by mutableStateOf(false)
        internal set

    /**
     * The cell being edited, or `null` when no editor is open.
     */
    val editingCell: CellPosition?
        get() = if (isEditing) focusedCell else null

    /**
     * Message from [DataTableHeader.validateEdit] rejecting the last attempted commit, or `null`.
     *
     * Set while an editor is open and the user tried to commit an invalid value; cleared when the
     * edit ends or a later commit is accepted.
     */
    var editError: String? by mutableStateOf(null)
        internal set

    /**
     * Character the user typed to start an edit, or `null` when the edit began from Enter, F2,
     * a double-click, or [startEditing]. Seeds the editor in place of the cell's current value.
     */
    internal var editSeedText: String? by mutableStateOf(null)

    /**
     * Whether keyboard focus is inside the header's filter row.
     *
     * The table's key handling sits on the container as a *preview* handler, so it sees keys
     * before any field inside it does. While a filter has focus it stands down entirely — the
     * alternative is that typing `a` into a filter opens a cell editor and the arrow keys move
     * the row cursor instead of the caret. Set from one focus observer over the whole header, so
     * a custom `filterContent` is covered without having to do anything.
     */
    internal var filterFocused: Boolean by mutableStateOf(false)

    /**
     * Column widths overridden by user resizing. Keyed by column [DataTableHeader.key].
     */
    internal val columnWidths = mutableStateMapOf<String, Dp>()

    /**
     * Columns the user has hidden, by [DataTableHeader.key].
     *
     * This *adds* to `DataTableHeader.visible`: hiding here takes a column away, and showing it
     * again cannot bring back one whose header declares `visible = false`. The two say different
     * things — "the user does not want to see this" against "this column is not available" — and
     * a restored layout has no business overruling the second.
     */
    var hiddenColumns: Set<String> by mutableStateOf(emptySet())

    /**
     * Column keys in the order the user put them, or empty while they are in declared order.
     *
     * Columns the list does not name follow the ones it does, in declaration order — which is
     * where a column added since a layout was saved appears. Assigning an empty list puts every
     * column back where its header declares it.
     *
     * With grouped headers the order applies to every level of the tree at once, and a group
     * moves as a block — a column cannot be taken out of the group it was declared in.
     */
    var columnOrder: List<String> by mutableStateOf(emptyList())

    /**
     * Sorting and filtering the table owns, used whenever the caller has not taken control of
     * them with `onSortChange`, `onMultiSortChange`, or `onFiltersChange`. `DataTable` seeds
     * these from its parameters once and owns them from then on.
     */
    internal var internalSort: SortState by mutableStateOf(SortState())
    internal var internalMultiSort: List<SortState> by mutableStateOf(emptyList())
    internal var internalFilters: Map<String, String> by mutableStateOf(emptyMap())

    /**
     * Sorting and filtering as currently *displayed*, published by `DataTable` every composition.
     *
     * These are what [captureLayout] reads, which is why they are kept apart from the internal
     * copies above: a snapshot has to describe what the user is looking at whether the table owns
     * the sort or the caller does.
     */
    internal var activeSort: SortState by mutableStateOf(SortState())
    internal var activeMultiSort: List<SortState> by mutableStateOf(emptyList())
    internal var activeFilters: Map<String, String> by mutableStateOf(emptyMap())

    /**
     * Whether the table-owned view state has been set, either by seeding it from `DataTable`'s
     * parameters or by restoring a layout into it.
     *
     * A restore usually happens *before* the table first composes — an application reading a
     * preference on startup — so the seed has to stand down once something else has spoken, or it
     * would wipe the sort and filters that were just put back.
     */
    private var internalStateSeeded = false

    /** Seeds the table-owned view state from `DataTable`'s parameters, once. */
    internal fun seedInternalState(
        sortBy: SortState,
        multiSortBy: List<SortState>,
        filters: Map<String, String>,
    ) {
        if (internalStateSeeded) return
        internalStateSeeded = true
        internalSort = sortBy
        internalMultiSort = multiSortBy
        internalFilters = filters
    }

    /**
     * Focus target for the table container, so an editor can hand keyboard focus back when it
     * closes. Held here rather than in `DataTable` because the editor is composed several
     * layers down, inside a `LazyColumn` item.
     */
    internal val containerFocusRequester = FocusRequester()

    /**
     * Row keys in display order, and the `LazyColumn` item index of each. Published by
     * `DataTable` every composition so navigation — from a key press or from an editor closing —
     * can resolve a focused key back to a position.
     */
    internal var rowKeys: List<Any> by mutableStateOf(emptyList())
    internal var rowScrollIndices: List<Int> by mutableStateOf(emptyList())

    /**
     * Visible leaf column keys in display order: frozen columns first, then scrollable ones.
     */
    internal var columnKeys: List<String> by mutableStateOf(emptyList())

    /**
     * Left and right edge, in pixels, of every *scrollable* column within the horizontally
     * scrolled content. Frozen columns are absent — they are always on screen.
     */
    internal var scrollableColumnBounds: Map<String, IntRange> by mutableStateOf(emptyMap())

    /**
     * Scrolls the table so that the given item index is visible.
     */
    suspend fun scrollToItem(index: Int, scrollOffset: Int = 0) {
        lazyListState.scrollToItem(index, scrollOffset)
    }

    /**
     * Animates scrolling so that the given item index is visible.
     */
    suspend fun animateScrollToItem(index: Int, scrollOffset: Int = 0) {
        lazyListState.animateScrollToItem(index, scrollOffset)
    }

    /**
     * Moves keyboard focus to the row with the given key, or clears focus when [key] is `null`.
     *
     * The key must be one produced by `DataTable`'s `itemKey`. Focusing a key that is not
     * currently displayed — filtered out, or on another page — is harmless: no row draws the
     * focus indicator, and the next arrow key starts again from the first row.
     *
     * This only moves focus; it does not scroll. To bring the row into view as well, pair it
     * with [animateScrollToItem] using the row's position in the list you passed to `DataTable`:
     *
     * ```
     * val position = people.indexOfFirst { it.id == personId }
     * state.focusRow(personId)
     * if (position >= 0) state.animateScrollToItem(position)
     * ```
     */
    fun focusRow(key: Any?) {
        focusedKey = key
    }

    /**
     * Moves keyboard focus to one cell, or clears it when either key is `null`.
     *
     * Cell focus only draws and only responds to the arrow keys once `DataTable`'s
     * `cellNavigation` is on, which declaring any editable column also turns on.
     *
     * Like [focusRow] this does not scroll; use [revealFocusedCell] to bring the cell into view.
     */
    fun focusCell(rowKey: Any?, columnKey: String?) {
        focusedKey = rowKey
        focusedColumnKey = if (rowKey == null) null else columnKey
        if (focusedCell == null) cancelEditing()
        clearRange()
    }

    /**
     * Selects the block of cells between two corners, leaving the cursor on [focus].
     *
     * Either corner may be the top-left one; the rectangle is normalised against the rows and
     * columns currently on display.
     */
    fun selectRange(anchor: CellPosition, focus: CellPosition) {
        focusedKey = focus.rowKey
        focusedColumnKey = focus.columnKey
        selectionAnchor = anchor
        refreshRange()
    }

    /**
     * Extends the selection to one cell, the way Shift+click does.
     *
     * The first extension anchors on wherever the cursor already was, so a plain click followed
     * by a shift-click selects the block between them. Shift-clicking with nothing focused yet
     * anchors on the clicked cell instead, selecting just it.
     */
    fun extendRangeTo(rowKey: Any, columnKey: String) {
        val anchor = selectionAnchor ?: focusedCell
        focusedKey = rowKey
        focusedColumnKey = columnKey
        selectionAnchor = anchor ?: focusedCell
        refreshRange()
    }

    /**
     * Collapses the selection back to the single focused cell.
     */
    fun clearRange() {
        selectionAnchor = null
        rangeRowKeys = emptySet()
        rangeColumnKeys = emptySet()
    }

    /**
     * Opens the editor on one cell, as Enter or F2 would.
     *
     * Does nothing when the column is not editable — the table checks that before composing an
     * editor — but the focus move still happens.
     */
    fun startEditing(rowKey: Any, columnKey: String) {
        focusCell(rowKey, columnKey)
        editSeedText = null
        editError = null
        isEditing = true
    }

    /**
     * Closes any open editor, discarding what was typed.
     */
    fun cancelEditing() {
        endEdit()
    }

    /**
     * Clears all user-resized column widths, reverting to header-defined or weighted widths.
     */
    fun resetColumnWidths() {
        columnWidths.clear()
    }

    /**
     * Hides or shows one column, by [DataTableHeader.key].
     *
     * Showing a column whose header declares `visible = false` does nothing — see
     * [hiddenColumns]. Hiding the column that has keyboard focus is safe: focus is held by key,
     * so it simply stops drawing until the column comes back.
     */
    fun setColumnHidden(key: String, hidden: Boolean) {
        hiddenColumns = if (hidden) hiddenColumns + key else hiddenColumns - key
    }

    /**
     * Whether the user has hidden this column. Says nothing about `DataTableHeader.visible`, so a
     * column can be shown by this and still not be rendered.
     */
    fun isColumnHidden(key: String): Boolean = key in hiddenColumns

    /**
     * Moves one column to a position among the others, the way dragging its header would.
     *
     * Positions are counted over the visible leaf columns in display order, frozen ones first.
     * A key that is not on display, or an index outside the table, is clamped or ignored rather
     * than throwing.
     */
    fun moveColumn(key: String, toIndex: Int) {
        // Whatever order is in force now: the stored one, extended with any column it has not
        // heard of, or the visual order when nothing has been reordered yet.
        val base = if (columnOrder.isEmpty()) {
            columnKeys
        } else {
            columnOrder + columnKeys.filter { it !in columnOrder }
        }
        val reordered = base.toMutableList()
        val from = reordered.indexOf(key)
        if (from < 0) return
        reordered.removeAt(from)
        reordered.add(toIndex.coerceIn(0, reordered.size), key)
        columnOrder = reordered
    }

    /**
     * Captures how the table is arranged right now: widths, hidden columns, order, sort, and
     * filters.
     *
     * Reads what is *displayed*, so it captures a sort or a filter the caller controls just as
     * well as one the table owns. Save the result with [DataTableLayout.encodeToString], or map
     * it onto a type of your own.
     */
    fun captureLayout(): DataTableLayout = DataTableLayout(
        columnWidths = columnWidths.toMap(),
        hiddenColumns = hiddenColumns,
        columnOrder = columnOrder,
        sortBy = activeSort,
        multiSortBy = activeMultiSort,
        filters = activeFilters,
    )

    /**
     * Puts a captured layout back.
     *
     * Widths, hidden columns, and order always apply — the table owns those. The sort and the
     * filters apply only where the table owns them too: a table given `onSortChange` or
     * `onFiltersChange` renders what its caller passes, so restore those from
     * [DataTableLayout.sortBy], [DataTableLayout.multiSortBy], and [DataTableLayout.filters] into
     * your own state instead.
     *
     * Safe to call with a layout saved against an older version of the table: entries naming
     * columns that no longer exist are ignored.
     */
    fun applyLayout(layout: DataTableLayout) {
        // Claims the table-owned sort and filters, so composing the table afterwards does not
        // seed them back from its parameters.
        internalStateSeeded = true
        columnWidths.clear()
        columnWidths.putAll(layout.columnWidths)
        hiddenColumns = layout.hiddenColumns.toSet()
        columnOrder = layout.columnOrder.toList()
        internalSort = layout.sortBy
        internalMultiSort = layout.multiSortBy
        internalFilters = layout.filters
    }

    /**
     * Drops every column override — widths, hidden columns, and order — leaving the table as its
     * headers declare it.
     *
     * Sorting and filtering are left alone: they are what the table is *showing*, not how it is
     * arranged, and a "reset the layout" button that silently cleared the user's filters would
     * be a surprise.
     */
    fun resetLayout() {
        columnWidths.clear()
        hiddenColumns = emptySet()
        columnOrder = emptyList()
    }

    /**
     * Scrolls both axes so the focused cell is on screen. No-op when nothing is focused.
     */
    suspend fun revealFocusedCell() {
        revealFocusedRow()
        revealFocusedColumn()
    }

    /**
     * Returns the current width for a column, considering any user resize override.
     */
    internal fun resolvedColumnWidth(key: String, headerWidth: Dp?): Dp? {
        return columnWidths[key] ?: headerWidth
    }

    /** The cell and timestamp of the last press, for spotting a double-click on one cell. */
    private var lastPressedCell: CellPosition? = null
    private var lastPressedAt: Long = 0

    /**
     * Records a press on one cell and reports whether it completed a double-click.
     *
     * The table detects this itself, from the press stream it already listens to, rather than
     * asking `detectTapGestures` for an `onDoubleTap`: supplying one makes every *single* click
     * wait out the double-tap timeout before it selects anything, and a data-entry grid cannot
     * afford to have selection lag behind the pointer.
     */
    internal fun registerCellPress(
        position: CellPosition,
        timeMillis: Long,
        timeoutMillis: Long,
    ): Boolean {
        val isDoubleClick =
            position == lastPressedCell && timeMillis - lastPressedAt <= timeoutMillis
        lastPressedCell = position
        // Consumed, so a third click does not read as a second double-click.
        lastPressedAt = if (isDoubleClick) 0 else timeMillis
        return isDoubleClick
    }

    /** Closes any open editor. Used for both a cancel and a successful commit. */
    internal fun endEdit() {
        isEditing = false
        editError = null
        editSeedText = null
    }

    /** Position of the focused row among the displayed rows, or -1. */
    internal fun focusedRowPosition(): Int = rowKeys.indexOf(focusedKey)

    /** Position of the focused column among the visible leaf columns, or -1. */
    internal fun focusedColumnPosition(): Int = columnKeys.indexOf(focusedColumnKey)

    /**
     * Moves focus to the row and column at the given positions, clamping both to what exists.
     *
     * Passing a null column position leaves the focused column alone, which is what the plain
     * up/down arrows do — they walk rows without dragging the cursor back to column one.
     */
    internal fun focusPosition(
        rowPosition: Int,
        columnPosition: Int?,
        extendRange: Boolean = false,
    ) {
        if (rowKeys.isEmpty()) return
        // Where the cursor was, which is where an unanchored range anchors itself.
        val previous = focusedCell
        focusedKey = rowKeys[rowPosition.coerceIn(0, rowKeys.lastIndex)]
        if (columnPosition != null && columnKeys.isNotEmpty()) {
            focusedColumnKey = columnKeys[columnPosition.coerceIn(0, columnKeys.lastIndex)]
        }
        if (extendRange) {
            if (selectionAnchor == null) selectionAnchor = previous ?: focusedCell
            refreshRange()
        } else {
            clearRange()
        }
    }

    /**
     * Recomputes the range's key sets from the current corners. Called on every extension, and
     * never on a re-sort — the highlighted cells are the ones that were chosen, not whatever now
     * sits between those coordinates.
     */
    private fun refreshRange() {
        val anchor = selectionAnchor
        val focus = focusedCell
        if (anchor == null || focus == null) {
            rangeRowKeys = emptySet()
            rangeColumnKeys = emptySet()
            return
        }
        val anchorRow = rowKeys.indexOf(anchor.rowKey)
        val focusRow = rowKeys.indexOf(focus.rowKey)
        val anchorColumn = columnKeys.indexOf(anchor.columnKey)
        val focusColumn = columnKeys.indexOf(focus.columnKey)
        // A corner that is no longer displayed — filtered out, or on another page — leaves no
        // rectangle to draw. Clearing beats guessing at where it went.
        if (anchorRow < 0 || focusRow < 0 || anchorColumn < 0 || focusColumn < 0) {
            clearRange()
            return
        }
        rangeRowKeys = rowKeys
            .subList(minOf(anchorRow, focusRow), maxOf(anchorRow, focusRow) + 1)
            .toSet()
        rangeColumnKeys = columnKeys
            .subList(minOf(anchorColumn, focusColumn), maxOf(anchorColumn, focusColumn) + 1)
            .toSet()
    }

    /**
     * Moves focus one cell along the reading order, wrapping onto the neighbouring row at the
     * ends of a row. Returns false at the very first and very last cell of the table, where Tab
     * should fall through and let focus leave the table entirely.
     */
    internal fun advanceCellFocus(backwards: Boolean): Boolean {
        if (rowKeys.isEmpty() || columnKeys.isEmpty()) return false
        val row = focusedRowPosition().coerceAtLeast(0)
        val column = focusedColumnPosition().coerceAtLeast(0)
        val next = column + if (backwards) -1 else 1
        return when {
            next in columnKeys.indices -> { focusPosition(row, next); true }
            !backwards && row < rowKeys.lastIndex -> { focusPosition(row + 1, 0); true }
            backwards && row > 0 -> { focusPosition(row - 1, columnKeys.lastIndex); true }
            else -> false
        }
    }

    /**
     * Scrolls vertically so the focused row is on screen.
     */
    internal suspend fun revealFocusedRow() {
        val position = focusedRowPosition()
        if (position < 0) return
        lazyListState.animateScrollToItem(rowScrollIndices.getOrElse(position) { position })
    }

    /**
     * Scrolls horizontally so the focused column is fully on screen, by the shortest move that
     * gets it there. Frozen columns need no scrolling and are skipped.
     */
    internal suspend fun revealFocusedColumn() {
        val bounds = scrollableColumnBounds[focusedColumnKey ?: return] ?: return
        val viewport = horizontalScrollState.viewportSize
        if (viewport <= 0) return
        val offset = horizontalScrollState.value
        val target = when {
            bounds.first < offset -> bounds.first
            bounds.last > offset + viewport -> bounds.last - viewport
            else -> return
        }
        horizontalScrollState.animateScrollTo(target.coerceIn(0, horizontalScrollState.maxValue))
    }
}

/**
 * Creates and remembers a [DataTableState].
 */
@Composable
fun rememberDataTableState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): DataTableState {
    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset
    )
    val horizontalScrollState = rememberScrollState()
    return remember(lazyListState, horizontalScrollState) {
        DataTableState(lazyListState, horizontalScrollState)
    }
}
