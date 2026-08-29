package io.github.stephenwanjala.datatable

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A flexible, Compose Multiplatform data table built entirely on Foundation APIs.
 *
 * Features:
 * - Column sorting (single and multi-column with Ctrl+click)
 * - Row selection (none, single, multi)
 * - Row expansion with custom content
 * - Row hover highlight and alternating row colors
 * - Frozen/pinned columns
 * - Nested (grouped) headers, to any depth
 * - Column resizing via drag handles
 * - Grouping with custom group header and summary rows
 * - Pagination with configurable items-per-page
 * - Column visibility toggle
 * - Text overflow / ellipsis per column
 * - Per-column display formatting, leaving values raw for sorting and editing
 * - Custom sort comparators
 * - A filter row under the header, client-side or handed off with `manualFiltering`
 * - Right-click context menu callback
 * - Keyboard navigation (arrow keys, Enter, Space, Home, End)
 * - Cell-level focus and grid keyboard navigation
 * - Cell range selection and clipboard copy
 * - In-place cell editing, with validation and custom editors
 * - Programmatic scroll-to-row via [DataTableState]
 * - Customizable colors and text styles without any theming framework
 *
 * Selection and expansion are tracked by row key rather than by item. Callers hoist a
 * `Set<Any>` of keys produced by [itemKey], so both survive item instances being replaced
 * (a refresh from a repository, for example) and neither requires the item type to implement
 * `equals`/`hashCode`.
 *
 * @param T The item type for each row.
 * @param itemKey Produces a stable, unique key per row — a database id or similar. Used for
 *                `LazyColumn` item identity and as the identity of a row in [selectedKeys]
 *                and [expandedKeys]. Keys must be unique across [items]: two rows sharing a
 *                key select, expand, and recycle as one row.
 * @param selectedKeys Keys of the currently selected rows.
 * @param onSelectionChange Invoked with the new set of selected keys.
 * @param expandedKeys Keys of the currently expanded rows.
 * @param onExpandChange Invoked with the new set of expanded keys.
 * @param sortBy Active single-column sort. Read only when [onSortChange] is supplied.
 * @param onSortChange Invoked when a header is clicked. Supplying it makes sorting *controlled*:
 *                     the table renders [sortBy] and never changes it, so the caller must feed
 *                     the new value back. Leave it `null` to let the table own sort state.
 * @param multiSortBy Active multi-column sort. Controlled by [onMultiSortChange] the same way.
 * @param manualSorting When true the table does not reorder [items] — the caller has already
 *                      sorted them, typically in a database query. Headers still show sort
 *                      indicators and report clicks.
 * @param filters Active column filters, as column key to query text. Read only when
 *                [onFiltersChange] is supplied, which makes filtering *controlled* the same way
 *                sorting is: the table renders these and never changes them, so the caller must
 *                feed the new value back. Leave it `null` to let the table own its filters.
 *                Filtering applies to any visible column with a query, whether or not it draws a
 *                field — which is what lets a filter UI of your own drive the table's matching.
 * @param onFiltersChange Invoked with the whole new filter map when a filter changes, on every
 *                        keystroke. Debounce it before turning it into a server query.
 * @param manualFiltering When true the table does not filter [items] — the caller has already
 *                        done it, typically in the same query that sorted and paged them. The
 *                        filter row still renders and still reports what the user types.
 * @param noResultsContent Shown in place of the rows when filters match nothing, telling that
 *                         apart from a table that has no data at all.
 * @param currentPage Active zero-based page. Read only when [onPageChange] is supplied, which
 *                    makes pagination controlled in the same way as sorting.
 * @param manualPagination When true [items] is already the current page and the table does not
 *                         slice it. Requires [totalItems].
 * @param totalItems Row count across every page. Defaults to `items.size`, which is correct
 *                   unless [manualPagination] is on — then only the caller knows the true total.
 * @param cellNavigation Moves keyboard focus down to the cell: Left/Right walk columns, the
 *                       focused cell draws a cursor, and clicking a cell focuses it. Declaring
 *                       any editable column turns this on by itself, since an editor you cannot
 *                       reach is no use. Off by default, which leaves row navigation exactly as
 *                       it was.
 * @param onCellEdit Invoked when a cell edit is committed, with the old and new text. The table
 *                   never mutates [items] — apply the edit to your own model and pass the
 *                   updated list back. Without this, editing is a no-op that still validates.
 * @param onCopy Invoked on Ctrl+C with the rows, columns, and cell text under the selection.
 *               Leave it `null` and the table writes tab-separated text to the system clipboard
 *               itself; supply it to write a different format, or to copy somewhere else.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun <T> DataTable(
    items: List<T>,
    headers: List<DataTableHeader<T>>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    state: DataTableState = rememberDataTableState(),
    // Selection
    showSelect: Boolean = false,
    selectionMode: SelectionMode = if (showSelect) SelectionMode.MULTI else SelectionMode.NONE,
    selectedKeys: Set<Any> = emptySet(),
    onSelectionChange: ((Set<Any>) -> Unit)? = null,
    // Expansion
    showExpand: Boolean = false,
    expandedKeys: Set<Any> = emptySet(),
    onExpandChange: ((Set<Any>) -> Unit)? = null,
    expandContent: (@Composable (T) -> Unit)? = null,
    // Layout
    density: DataTableDensity = DataTableDensity.DEFAULT,
    colors: DataTableColors = DataTableDefaults.colors(),
    textStyles: DataTableTextStyles = DataTableDefaults.textStyles(),
    // Sorting
    sortBy: SortState = SortState(),
    onSortChange: ((SortState) -> Unit)? = null,
    multiSortBy: List<SortState> = emptyList(),
    onMultiSortChange: ((List<SortState>) -> Unit)? = null,
    manualSorting: Boolean = false,
    // Filtering
    filters: Map<String, String> = emptyMap(),
    onFiltersChange: ((Map<String, String>) -> Unit)? = null,
    manualFiltering: Boolean = false,
    // Column resizing
    resizableColumns: Boolean = false,
    minColumnWidth: Dp = 40.dp,
    // Header / Footer
    hideDefaultHeader: Boolean = false,
    hideDefaultFooter: Boolean = false,
    loading: Boolean = false,
    loadingContent: (@Composable () -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    noDataContent: (@Composable () -> Unit)? = null,
    noResultsContent: (@Composable () -> Unit)? = null,
    // Grouping
    groupBy: ((T) -> String)? = null,
    groupHeaderContent: (@Composable (String, List<T>) -> Unit)? = null,
    groupSummaryContent: (@Composable (String, List<T>) -> Unit)? = null,
    // Row interactions
    onRowClick: ((T) -> Unit)? = null,
    onRowDoubleClick: ((T) -> Unit)? = null,
    onRowContextMenu: ((T, Offset) -> Unit)? = null,
    // Pagination
    showPagination: Boolean = false,
    itemsPerPage: Int = 10,
    currentPage: Int = 0,
    onPageChange: ((Int) -> Unit)? = null,
    itemsPerPageOptions: List<Int> = listOf(10, 25, 50, 100),
    onItemsPerPageChange: ((Int) -> Unit)? = null,
    manualPagination: Boolean = false,
    totalItems: Int? = null,
    // Cell focus and editing
    cellNavigation: Boolean = false,
    onCellEdit: ((CellEdit<T>) -> Unit)? = null,
    onCopy: ((ClipboardSelection<T>) -> Unit)? = null,
    // Scrollbars
    showScrollbars: Boolean = true,
    scrollbarThickness: Dp = 8.dp,
) {
    val scope = rememberCoroutineScope()
    val listState = state.lazyListState
    val horizontalScrollState = state.horizontalScrollState
    // The user's own arrangement — what they hid, and the order they put columns in — folded
    // into the tree once, so everything downstream reads `visible` and list order as declared.
    val arrangedHeaders = remember(headers, state.hiddenColumns, state.columnOrder) {
        applyColumnOverrides(headers, state.hiddenColumns, state.columnOrder)
    }
    val flatHeaders = remember(arrangedHeaders) { flattenHeaders(arrangedHeaders) }

    // Partition into frozen and scrollable. A frozen column has to declare a width — the frozen
    // section sits outside the horizontally scrolling area, so there is nothing for a weighted
    // column to take its share of.
    val (frozenHeaders, scrollableHeaders) = remember(flatHeaders) {
        flatHeaders.forEach { header ->
            require(!header.fixed || header.width != null) {
                "Frozen column '${header.key}' must declare an explicit width. " +
                    "Set `width = ...` on the header, or drop `fixed = true`."
            }
        }
        flatHeaders.partition { it.fixed }
    }
    val hasFrozenColumns = frozenHeaders.isNotEmpty()

    // An editable column implies cell navigation: without it there is no way to reach the cell
    // you want to edit, and Enter/F2 have nothing to open an editor on.
    val hasEditableColumns = remember(flatHeaders) { flatHeaders.any { it.editable } }
    val gridNavigation = cellNavigation || hasEditableColumns

    // Visual order of the leaf columns, which is the order Left/Right and Tab walk.
    val columnKeys = remember(frozenHeaders, scrollableHeaders) {
        (frozenHeaders + scrollableHeaders).map { it.key }
    }

    // The header renders from the tree so groups can span their children, while the body keeps
    // using the flattened leaves.
    val (frozenTree, scrollableTree) = remember(arrangedHeaders) { partitionHeaderTree(arrangedHeaders) }

    require(!manualPagination || totalItems != null) {
        "manualPagination = true requires totalItems: the table only holds the current page, " +
            "so it cannot work out how many pages there are."
    }

    // Sort and page state are controlled when the caller supplies the matching callback, and
    // managed internally when they do not. The internal values are seeded from the parameters
    // once and then owned by the table — they are not kept in sync with later parameter changes,
    // which is what makes the mode unambiguous either way.
    // Sorting and filtering live on the state rather than here, so a captured layout can put
    // them back. Paging does not: which page you are on is not part of how a table is arranged.
    remember(state) { state.seedInternalState(sortBy, multiSortBy, filters) }
    var uncontrolledPage by remember { mutableStateOf(currentPage) }

    val activeSort = if (onSortChange != null) sortBy else state.internalSort
    val activeMultiSort = if (onMultiSortChange != null) multiSortBy else state.internalMultiSort
    val activePage = if (onPageChange != null) currentPage else uncontrolledPage
    val activeFilters = if (onFiltersChange != null) filters else state.internalFilters

    val selectSort: (SortState) -> Unit = { newSort ->
        if (onSortChange != null) onSortChange(newSort) else state.internalSort = newSort
    }
    val selectMultiSort: (List<SortState>) -> Unit = { newMulti ->
        if (onMultiSortChange != null) onMultiSortChange(newMulti) else state.internalMultiSort = newMulti
    }
    val selectPage: (Int) -> Unit = { newPage ->
        if (onPageChange != null) onPageChange(newPage) else uncontrolledPage = newPage
    }
    val selectFilter: (String, String) -> Unit = { key, query ->
        val updated =
            if (query.isEmpty()) activeFilters - key else activeFilters + (key to query)
        // Filtering shrinks the table under whatever page the user was on, which would otherwise
        // leave them looking at a page that no longer exists.
        if (activePage != 0) selectPage(0)
        if (onFiltersChange != null) onFiltersChange(updated) else state.internalFilters = updated
    }

    // Filters resolved against the visible columns, and the rows that survive them. Under
    // `manualFiltering` the caller has filtered already, but the resolved list is still what
    // tells an empty table apart from a filter that matched nothing.
    val resolvedFilters = remember(flatHeaders, activeFilters) {
        resolveFilters(flatHeaders, activeFilters)
    }
    val hasFilterRow = remember(flatHeaders) { flatHeaders.any { it.hasFilterField() } }
    val filteredItems = remember(items, resolvedFilters, manualFiltering) {
        if (manualFiltering) items else applyFilters(items, resolvedFilters)
    }

    val showCheckboxes = selectionMode != SelectionMode.NONE && showSelect

    // Keys for every item, used by select-all and by the header's "all selected" state.
    // Kept in a `remember` so the containsAll scan does not run on every recomposition.
    // Filtered rows are deliberately absent: select-all selects what the user can see.
    val allKeys = remember(filteredItems, itemKey) {
        filteredItems.mapTo(LinkedHashSet<Any>(filteredItems.size), itemKey)
    }
    val allSelected = remember(allKeys, selectedKeys) {
        allKeys.isNotEmpty() && selectedKeys.containsAll(allKeys)
    }

    val processedItems = remember(
        filteredItems, flatHeaders, activeSort, activeMultiSort, activePage,
        showPagination, itemsPerPage, manualSorting, manualPagination,
    ) {
        var result = filteredItems

        // Under manualSorting the caller has already ordered `items`; the header still shows
        // indicators and reports clicks, but the table must not reorder anything itself.
        if (!manualSorting) {
            // Build active sort list: multi-sort takes precedence
            val activeSorts = activeMultiSort.ifEmpty {
                listOfNotNull(activeSort.takeIf { it.order != SortOrder.NONE })
            }

            if (activeSorts.isNotEmpty()) {
                result = result.sortedWith(Comparator { a, b ->
                    for (sort in activeSorts) {
                        val header = flatHeaders.find { it.key == sort.key } ?: continue
                        val comparison = if (header.comparator != null) {
                            header.comparator.compare(a, b)
                        } else {
                            val valueA = header.value?.invoke(a) ?: ""
                            val valueB = header.value?.invoke(b) ?: ""
                            @Suppress("UNCHECKED_CAST")
                            compareValues(valueA as? Comparable<Any>, valueB as? Comparable<Any>)
                        }
                        if (comparison != 0) {
                            return@Comparator if (sort.order == SortOrder.ASCENDING) comparison else -comparison
                        }
                    }
                    0
                })
            }
        }

        // Under manualPagination `items` is already the current page.
        if (showPagination && !manualPagination) {
            val start = activePage * itemsPerPage
            val end = minOf(start + itemsPerPage, result.size)
            if (start < result.size) result.subList(start, end) else emptyList()
        } else {
            result
        }
    }

    // Row count across every page — after filtering, which is what the footer and the page
    // count have to be about. Equal to items.size unless the caller is paging manually, in which
    // case only they know how many rows exist beyond the page they handed us.
    val rowCount = totalItems ?: filteredItems.size

    val totalPages = remember(rowCount, itemsPerPage, showPagination) {
        if (showPagination && itemsPerPage > 0) {
            ((rowCount + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
        } else {
            1
        }
    }

    val groupedItems = remember(processedItems, groupBy) {
        if (groupBy != null) processedItems.groupBy(groupBy)
        else mapOf("" to processedItems)
    }

    // Table-wide row index at which each group starts. This must be computed up front:
    // a counter mutated while the LazyColumn content is being built would already hold
    // its final value by the time an item's content lambda actually composes.
    val groupOffsets = remember(groupedItems) {
        var offset = 0
        groupedItems.mapValues { (_, groupItems) ->
            offset.also { offset += groupItems.size }
        }
    }

    // Rows in display order. Grouping reorders them relative to `processedItems`, so keyboard
    // navigation walks this list rather than the pre-grouping one.
    val orderedItems = remember(groupedItems) { groupedItems.values.flatten() }
    val rowKeys = remember(orderedItems, itemKey) { orderedItems.map(itemKey) }

    // LazyColumn item index for each display row. Group header and summary items sit between
    // rows, so a row's position and its scroll index diverge once grouping is on.
    val hasGroupHeaders = groupBy != null && groupHeaderContent != null
    val hasGroupSummaries = groupBy != null && groupSummaryContent != null
    val rowScrollIndices = remember(groupedItems, hasGroupHeaders, hasGroupSummaries) {
        val indices = ArrayList<Int>(orderedItems.size)
        var lazyIndex = 0
        groupedItems.values.forEach { groupItems ->
            if (hasGroupHeaders) lazyIndex++
            repeat(groupItems.size) { indices.add(lazyIndex++) }
            if (hasGroupSummaries) lazyIndex++
        }
        indices
    }

    // What Ctrl+C puts on the clipboard, in the order the user is looking at. The cascade runs
    // narrowest first: an extended block of cells, then the checked rows, then the one focused
    // cell, then the focused row. Returns false when none of those exist, which leaves Ctrl+C
    // unconsumed so an application with its own binding still gets it.
    val copySelection: () -> Boolean = copy@{
        val visibleColumns = frozenHeaders + scrollableHeaders
        val rangeRows = state.rangeRowKeys
        val rangeColumns = state.rangeColumnKeys
        val focusedCell = state.focusedCell

        val rows: List<T>
        val columns: List<DataTableHeader<T>>
        when {
            rangeRows.isNotEmpty() && rangeColumns.isNotEmpty() -> {
                rows = orderedItems.filter { itemKey(it) in rangeRows }
                columns = visibleColumns.filter { it.key in rangeColumns }
            }

            selectedKeys.isNotEmpty() -> {
                rows = orderedItems.filter { itemKey(it) in selectedKeys }
                columns = visibleColumns
            }

            focusedCell != null -> {
                rows = orderedItems.filter { itemKey(it) == focusedCell.rowKey }
                columns = visibleColumns.filter { it.key == focusedCell.columnKey }
            }

            state.focusedKey != null -> {
                rows = orderedItems.filter { itemKey(it) == state.focusedKey }
                columns = visibleColumns
            }

            else -> return@copy false
        }
        if (rows.isEmpty() || columns.isEmpty()) return@copy false

        val selection = ClipboardSelection(
            rows = rows,
            columns = columns,
            // The formatted text, not the raw value: a copy should carry what the cell reads.
            cells = rows.map { item ->
                columns.map { column -> column.displayText(item) }
            },
        )
        // A caller's handler *replaces* the clipboard write rather than running alongside it, so
        // that a copy can be redirected or suppressed entirely. `ClipboardSelection`'s own
        // `copyToSystemClipboard` is public for handlers that want to do both.
        if (onCopy != null) {
            onCopy(selection)
            true
        } else {
            selection.copyToSystemClipboard()
        }
    }

    // Navigation reads these back out of the state at key-event time — from the container's key
    // handler, and from an editor closing several layers down inside a LazyColumn item.
    SideEffect {
        state.rowKeys = rowKeys
        state.rowScrollIndices = rowScrollIndices
        state.columnKeys = columnKeys
        // What a captured layout describes: the sort and filters on display, whoever owns them.
        state.activeSort = activeSort
        state.activeMultiSort = activeMultiSort
        state.activeFilters = activeFilters
    }

    // Width taken by the select/expand controls, which sit ahead of the first column.
    val leadingWidth =
        (if (showCheckboxes) 20.dp + density.horizontalPadding * 2 else 0.dp) +
            (if (showExpand) 48.dp else 0.dp)

    // Total of the columns that declare a width, and how many are left to share what remains.
    // Frozen columns always declare a width, so only the scrollable side can be weighted.
    val scrollableFixedWidth = scrollableHeaders.fold(0.dp) { total, header ->
        total + (state.resolvedColumnWidth(header.key, header.width) ?: 0.dp)
    }
    val frozenFixedWidth = frozenHeaders.fold(0.dp) { total, header ->
        total + (state.resolvedColumnWidth(header.key, header.width) ?: 0.dp)
    }
    val weightedColumnCount = scrollableHeaders.count {
        state.resolvedColumnWidth(it.key, it.width) == null
    }

    // Double-clicking an editable cell opens its editor — unless the caller claimed the gesture
    // for themselves, in which case theirs wins and Enter, F2, and typing still reach the editor.
    val editOnDoubleClick = onRowDoubleClick == null

    // Focus for keyboard navigation. Lives on the state so a cell editor can hand focus back
    // to the table when it closes.
    val focusRequester = state.containerFocusRequester

    BoxWithConstraints(
        modifier = modifier
            .background(colors.container)
            .focusRequester(focusRequester)
            .focusable()
            // Take focus when the table is clicked, so the keyboard shortcuts below actually
            // receive key events. Rows are driven by raw pointer input rather than `clickable`,
            // which would have requested focus on its own — without this the table can never be
            // focused at all and every shortcut is dead.
            //
            // Initial pass and no consumption: this must not interfere with row clicks,
            // selection, or the resize handles.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }
            .dataTableKeyboardNavigation(
                state = state,
                scope = scope,
                gridNavigation = gridNavigation,
                isColumnEditable = { key ->
                    flatHeaders.firstOrNull { it.key == key }?.editable == true
                },
                onCopy = copySelection,
                onRowClick = { position ->
                    orderedItems.getOrNull(position)?.let { onRowClick?.invoke(it) }
                },
                onToggleSelection = { position ->
                    orderedItems.getOrNull(position)?.let { item ->
                        val key = itemKey(item)
                        handleSelection(
                            mode = selectionMode,
                            checked = !selectedKeys.contains(key),
                            key = key,
                            currentSelection = selectedKeys,
                            onSelectionChange = onSelectionChange,
                        )
                    }
                },
            )
    ) {
        // Weighted columns need a bounded width to divide up. Everything scrollable sits inside
        // `horizontalScroll`, which measures its content with an unbounded width, and
        // `Modifier.weight` resolves to zero against that — so give the scrolled content a
        // definite width instead of letting it run to infinity.
        //
        // It is the viewport minus whatever is pinned outside the scrolling area, floored so a
        // weighted column keeps at least `minColumnWidth` when the fixed columns already overflow.
        val scrollableContentWidth: Dp? = when {
            weightedColumnCount == 0 -> null
            !constraints.hasBoundedWidth -> null
            else -> {
                val pinned = if (hasFrozenColumns) leadingWidth + frozenFixedWidth + 1.dp else 0.dp
                val viewport = maxWidth - pinned
                val floor = scrollableFixedWidth +
                    (if (hasFrozenColumns) 0.dp else leadingWidth) +
                    minColumnWidth * weightedColumnCount
                maxOf(viewport, floor)
            }
        }
        val scrollableWidthModifier =
            if (scrollableContentWidth != null) Modifier.width(scrollableContentWidth) else Modifier

        // Where each scrollable column sits inside the scrolled content, so moving cell focus
        // sideways can bring an off-screen column into view. Weighted columns split whatever the
        // fixed ones leave, which is exactly what `Modifier.weight` does a moment later — so the
        // bounds can be computed rather than measured, and are right on the first frame.
        if (gridNavigation) {
            val pixelDensity = LocalDensity.current
            val weightedWidth =
                if (weightedColumnCount > 0 && scrollableContentWidth != null) {
                    val leading = if (hasFrozenColumns) 0.dp else leadingWidth
                    ((scrollableContentWidth - leading - scrollableFixedWidth) / weightedColumnCount)
                        .coerceAtLeast(0.dp)
                } else {
                    0.dp
                }
            var left = if (hasFrozenColumns) 0.dp else leadingWidth
            val bounds = LinkedHashMap<String, IntRange>(scrollableHeaders.size)
            scrollableHeaders.forEach { header ->
                val width = state.resolvedColumnWidth(header.key, header.width) ?: weightedWidth
                with(pixelDensity) {
                    bounds[header.key] = IntRange(left.roundToPx(), (left + width).roundToPx())
                }
                left += width
            }
            SideEffect { state.scrollableColumnBounds = bounds }
        }

        Column {
            // ---- Header ----
            if (!hideDefaultHeader && headerContent == null) {
                Box {
                    if (hasFrozenColumns) {
                        FrozenRowLayout(
                            frozenHeaders = frozenHeaders,
                            scrollableHeaders = scrollableHeaders,
                            horizontalScrollState = horizontalScrollState,
                            dividerColor = colors.divider,
                            // Sized to content: a nested header is taller than a data row.
                            height = null,
                            frozenContent = {
                                DataTableHeaderRow(
                                    headers = frozenTree,
                                    showSelect = showCheckboxes,
                                    allSelected = allSelected,
                                    onSelectAll = {
                                        onSelectionChange?.invoke(if (allSelected) emptySet() else allKeys)
                                    },
                                    showExpand = showExpand,
                                    density = density,
                                    sortState = activeSort,
                                    onSortChange = selectSort,
                                    backgroundColor = colors.header,
                                    colors = colors,
                                    textStyles = textStyles,
                                    selectionMode = selectionMode,
                                    multiSortBy = activeMultiSort,
                                    onMultiSortChange = selectMultiSort,
                                    resizableColumns = resizableColumns,
                                    minColumnWidth = minColumnWidth,
                                    state = state,
                                )
                            },
                            scrollableContent = {
                                DataTableHeaderRow(
                                    headers = scrollableTree,
                                    modifier = scrollableWidthModifier,
                                    showSelect = false,
                                    allSelected = false,
                                    onSelectAll = {},
                                    showExpand = false,
                                    density = density,
                                    sortState = activeSort,
                                    onSortChange = selectSort,
                                    backgroundColor = colors.header,
                                    colors = colors,
                                    textStyles = textStyles,
                                    selectionMode = selectionMode,
                                    multiSortBy = activeMultiSort,
                                    onMultiSortChange = selectMultiSort,
                                    resizableColumns = resizableColumns,
                                    minColumnWidth = minColumnWidth,
                                    state = state,
                                )
                            },
                        )
                    }
                    else {
                        Column(
                            modifier = Modifier
                                .background(colors.header)
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                                .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                        ) {
                            DataTableHeaderRow(
                                headers = scrollableTree,
                                modifier = scrollableWidthModifier,
                                showSelect = showCheckboxes,
                                allSelected = allSelected,
                                onSelectAll = {
                                    onSelectionChange?.invoke(if (allSelected) emptySet() else allKeys)
                                },
                                showExpand = showExpand,
                                density = density,
                                sortState = activeSort,
                                onSortChange = selectSort,
                                backgroundColor = colors.header,
                                colors = colors,
                                textStyles = textStyles,
                                selectionMode = selectionMode,
                                multiSortBy = activeMultiSort,
                                onMultiSortChange = selectMultiSort,
                                resizableColumns = resizableColumns,
                                minColumnWidth = minColumnWidth,
                                state = state,
                            )
                        }
                    }

                    if (showScrollbars && !hasFrozenColumns) {
                        HorizontalScrollbar(
                            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                            adapter = rememberScrollbarAdapter(horizontalScrollState)
                        )
                    }
                }
            } else if (headerContent != null) {
                headerContent()
            }

            // ---- Filter row ----
            // Its own block rather than part of the header, so it survives `hideDefaultHeader`
            // and a custom `headerContent` — the fields line up with the columns, not with
            // whatever is drawn above them.
            if (hasFilterRow) {
                Box(
                    // One focus observer over the whole row, rather than one per field: focus
                    // moving between two fields would otherwise race, and whichever of the two
                    // reported last would decide whether the table swallows the next keystroke.
                    modifier = Modifier.onFocusChanged { state.filterFocused = it.hasFocus }
                ) {
                    if (hasFrozenColumns) {
                        FrozenRowLayout(
                            frozenHeaders = frozenHeaders,
                            scrollableHeaders = scrollableHeaders,
                            horizontalScrollState = horizontalScrollState,
                            dividerColor = colors.divider,
                            // Sized to content: a filter field is shorter than a data row.
                            height = null,
                            frozenContent = {
                                DataTableFilterRow(
                                    headers = frozenHeaders,
                                    showSelect = showCheckboxes,
                                    showExpand = showExpand,
                                    selectionMode = selectionMode,
                                    density = density,
                                    filters = activeFilters,
                                    onFilterChange = selectFilter,
                                    colors = colors,
                                    textStyles = textStyles,
                                    state = state,
                                )
                            },
                            scrollableContent = {
                                DataTableFilterRow(
                                    headers = scrollableHeaders,
                                    modifier = scrollableWidthModifier,
                                    showSelect = false,
                                    showExpand = false,
                                    selectionMode = selectionMode,
                                    density = density,
                                    filters = activeFilters,
                                    onFilterChange = selectFilter,
                                    colors = colors,
                                    textStyles = textStyles,
                                    state = state,
                                )
                            },
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .background(colors.filterRow)
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                                .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                        ) {
                            DataTableFilterRow(
                                headers = scrollableHeaders,
                                modifier = scrollableWidthModifier,
                                showSelect = showCheckboxes,
                                showExpand = showExpand,
                                selectionMode = selectionMode,
                                density = density,
                                filters = activeFilters,
                                onFilterChange = selectFilter,
                                colors = colors,
                                textStyles = textStyles,
                                state = state,
                            )
                        }
                    }
                }
            }

            TableDivider(colors.divider)

            // ---- Body ----
            Box(modifier = Modifier.weight(1f)) {
                when {
                    loading -> loadingContent?.invoke() ?: DefaultLoadingContent(textStyles)

                    processedItems.isEmpty() -> if (resolvedFilters.isNotEmpty()) {
                        noResultsContent?.invoke() ?: DefaultNoResultsContent(textStyles)
                    } else {
                        noDataContent?.invoke() ?: DefaultNoDataContent(textStyles)
                    }

                    else -> {
                        Box {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                groupedItems.forEach { (group, groupItems) ->
                                    val groupOffset = groupOffsets[group] ?: 0
                                    // Group header
                                    if (groupBy != null && groupHeaderContent != null) {
                                        item(key = "group-header-$group") {
                                            if (hasFrozenColumns) {
                                                FrozenRowLayout(
                                                    frozenHeaders = frozenHeaders,
                                                    scrollableHeaders = scrollableHeaders,
                                                    horizontalScrollState = horizontalScrollState,
                                                    dividerColor = colors.divider,
                                                    frozenContent = { /* group header spans full width */ },
                                                    scrollableContent = { groupHeaderContent(group, groupItems) },
                                                )
                                            } else {
                                                Row(
                                                    modifier = Modifier
                                                        .horizontalScroll(horizontalScrollState)
                                                        .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                                                ) {
                                                    groupHeaderContent(group, groupItems)
                                                }
                                            }
                                        }
                                    }

                                    // Items
                                    itemsIndexed(
                                        items = groupItems,
                                        key = { _, item -> itemKey(item) }
                                    ) { localIndex, item ->
                                        val rowIndex = groupOffset + localIndex
                                        val key = itemKey(item)
                                        val itemSelected = selectedKeys.contains(key)
                                        val itemExpanded = expandedKeys.contains(key)
                                        Column {
                                            if (hasFrozenColumns) {
                                                FrozenRowLayout(
                                                    frozenHeaders = frozenHeaders,
                                                    scrollableHeaders = scrollableHeaders,
                                                    horizontalScrollState = horizontalScrollState,
                                                    dividerColor = colors.divider,
                                                    frozenContent = {
                                                        DataTableRow(
                                                            item = item,
                                                            rowKey = key,
                                                            headers = frozenHeaders,
                                                            showSelect = showCheckboxes,
                                                            selected = itemSelected,
                                                            onSelectChange = { checked ->
                                                                handleSelection(selectionMode, checked, key, selectedKeys, onSelectionChange)
                                                            },
                                                            showExpand = showExpand,
                                                            expanded = itemExpanded,
                                                            onExpandChange = { exp ->
                                                                handleExpansion(exp, key, expandedKeys, onExpandChange)
                                                            },
                                                            density = density,
                                                            onClick = onRowClick?.let { { it(item) } },
                                                            onDoubleClick = onRowDoubleClick?.let { { it(item) } },
                                                            horizontalScrollState = horizontalScrollState,
                                                            colors = colors,
                                                            textStyles = textStyles,
                                                            rowIndex = rowIndex,
                                                            selectionMode = selectionMode,
                                                            onContextMenu = onRowContextMenu?.let { cb -> { offset -> cb(item, offset) } },
                                                            isFocused = state.focusedKey == key,
                                                            state = state,
                                                            gridNavigation = gridNavigation,
                                                            editOnDoubleClick = editOnDoubleClick,
                                                            onCellEdit = onCellEdit,
                                                        )
                                                    },
                                                    scrollableContent = {
                                                        DataTableRow(
                                                            item = item,
                                                            rowKey = key,
                                                            modifier = scrollableWidthModifier,
                                                            headers = scrollableHeaders,
                                                            showSelect = false,
                                                            selected = itemSelected,
                                                            onSelectChange = { checked ->
                                                                handleSelection(selectionMode, checked, key, selectedKeys, onSelectionChange)
                                                            },
                                                            showExpand = false,
                                                            expanded = false,
                                                            onExpandChange = {},
                                                            density = density,
                                                            onClick = onRowClick?.let { { it(item) } },
                                                            onDoubleClick = onRowDoubleClick?.let { { it(item) } },
                                                            horizontalScrollState = horizontalScrollState,
                                                            colors = colors,
                                                            textStyles = textStyles,
                                                            rowIndex = rowIndex,
                                                            selectionMode = selectionMode,
                                                            onContextMenu = onRowContextMenu?.let { cb -> { offset -> cb(item, offset) } },
                                                            isFocused = state.focusedKey == key,
                                                            state = state,
                                                            gridNavigation = gridNavigation,
                                                            editOnDoubleClick = editOnDoubleClick,
                                                            onCellEdit = onCellEdit,
                                                        )
                                                    },
                                                )
                                            } else {
                                                Box {
                                                    Row(
                                                        modifier = Modifier
                                                            .horizontalScroll(horizontalScrollState)
                                                            .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                                                    ) {
                                                        DataTableRow(
                                                            item = item,
                                                            rowKey = key,
                                                            modifier = scrollableWidthModifier,
                                                            headers = flatHeaders,
                                                            showSelect = showCheckboxes,
                                                            selected = itemSelected,
                                                            onSelectChange = { checked ->
                                                                handleSelection(selectionMode, checked, key, selectedKeys, onSelectionChange)
                                                            },
                                                            showExpand = showExpand,
                                                            expanded = itemExpanded,
                                                            onExpandChange = { exp ->
                                                                handleExpansion(exp, key, expandedKeys, onExpandChange)
                                                            },
                                                            density = density,
                                                            onClick = onRowClick?.let { { it(item) } },
                                                            onDoubleClick = onRowDoubleClick?.let { { it(item) } },
                                                            horizontalScrollState = horizontalScrollState,
                                                            colors = colors,
                                                            textStyles = textStyles,
                                                            rowIndex = rowIndex,
                                                            selectionMode = selectionMode,
                                                            onContextMenu = onRowContextMenu?.let { cb -> { offset -> cb(item, offset) } },
                                                            isFocused = state.focusedKey == key,
                                                            state = state,
                                                            gridNavigation = gridNavigation,
                                                            editOnDoubleClick = editOnDoubleClick,
                                                            onCellEdit = onCellEdit,
                                                        )
                                                    }
                                                }
                                            }

                                            // Expanded content
                                            if (showExpand && itemExpanded && expandContent != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(colors.expandedRow)
                                                        .padding(density.verticalPadding)
                                                ) {
                                                    expandContent(item)
                                                }
                                            }

                                            TableDivider(colors.divider)
                                        }
                                    }

                                    // Group summary
                                    if (groupBy != null && groupSummaryContent != null) {
                                        item(key = "group-summary-$group") {
                                            if (hasFrozenColumns) {
                                                FrozenRowLayout(
                                                    frozenHeaders = frozenHeaders,
                                                    scrollableHeaders = scrollableHeaders,
                                                    horizontalScrollState = horizontalScrollState,
                                                    dividerColor = colors.divider,
                                                    frozenContent = {},
                                                    scrollableContent = { groupSummaryContent(group, groupItems) },
                                                )
                                            } else {
                                                Row(
                                                    modifier = Modifier
                                                        .horizontalScroll(horizontalScrollState)
                                                        .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                                                ) {
                                                    groupSummaryContent(group, groupItems)
                                                }
                                            }
                                            TableDivider(colors.divider)
                                        }
                                    }
                                }
                            }

                            // Scrollbars
                            if (showScrollbars) {
                                VerticalScrollbar(
                                    modifier = Modifier
                                        .pointerHoverIcon(icon = PointerIcon.Hand)
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight(),
                                    adapter = rememberScrollbarAdapter(listState)
                                )
                                HorizontalScrollbar(
                                    modifier = Modifier
                                        .pointerHoverIcon(icon = PointerIcon.Hand)
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth(),
                                    adapter = rememberScrollbarAdapter(horizontalScrollState)
                                )
                            }
                        }
                    }
                }
            }

            // ---- Footer ----
            if (!hideDefaultFooter && footerContent == null) {
                if (showPagination) {
                    PaginationFooter(
                        currentPage = activePage,
                        totalPages = totalPages,
                        totalItems = rowCount,
                        itemsPerPage = itemsPerPage,
                        onPageChange = selectPage,
                        colors = colors,
                        textStyles = textStyles,
                        itemsPerPageOptions = itemsPerPageOptions,
                        onItemsPerPageChange = onItemsPerPageChange?.let { callback ->
                            { newSize ->
                                // A larger page size can put the current page past the end,
                                // so go back to the first page whenever it changes.
                                selectPage(0)
                                callback(newSize)
                            }
                        },
                    )
                } else {
                    DefaultFooter(itemCount = rowCount, colors = colors, textStyles = textStyles)
                }
            } else if (footerContent != null) {
                TableDivider(colors.divider)
                footerContent()
            }
        }
    }
}

// ---- Internal helpers ----

/**
 * Handles row selection based on the active [SelectionMode].
 *
 * Operates on the row's key (as produced by `DataTable`'s `itemKey`) rather than the item
 * itself, so selection survives item instances being replaced and does not depend on the
 * item type implementing `equals`/`hashCode`.
 */
private fun handleSelection(
    mode: SelectionMode,
    checked: Boolean,
    key: Any,
    currentSelection: Set<Any>,
    onSelectionChange: ((Set<Any>) -> Unit)?,
) {
    when (mode) {
        SelectionMode.SINGLE -> onSelectionChange?.invoke(if (checked) setOf(key) else emptySet())
        SelectionMode.MULTI -> {
            val newSelection = if (checked) currentSelection + key else currentSelection - key
            onSelectionChange?.invoke(newSelection)
        }
        SelectionMode.NONE -> {}
    }
}

/**
 * Adds or removes a row's key from the expanded set.
 *
 * Like [handleSelection], this is keyed on the row's `itemKey` rather than the item itself.
 */
private fun handleExpansion(
    expanded: Boolean,
    key: Any,
    currentExpanded: Set<Any>,
    onExpandChange: ((Set<Any>) -> Unit)?,
) {
    onExpandChange?.invoke(if (expanded) currentExpanded + key else currentExpanded - key)
}

/**
 * A thin horizontal line used as a visual separator.
 */
@Composable
internal fun TableDivider(color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

/**
 * Enables natural horizontal scrolling with trackpads and mice.
 *
 * Handles wheel and trackpad scroll events only: a horizontal wheel/two-finger swipe, or
 * Shift + vertical wheel. It deliberately does not treat a press-and-drag as a scroll — that
 * would make dragging anywhere on a row pan the table, and would fight the column resize
 * handles, text selection, and any future drag-select.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Modifier.enableTrackpadHorizontalScroll(scrollState: ScrollState): Modifier {
    val scope = rememberCoroutineScope()
    return this
        .pointerInput(scrollState) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.first().scrollDelta
                        val hScroll = when {
                            delta.x != 0f -> delta.x
                            event.keyboardModifiers.isShiftPressed -> delta.y
                            else -> 0f
                        }
                        if (hScroll != 0f) {
                            scope.launch {
                                scrollState.scrollBy(-hScroll * 30f)
                            }
                        }
                    }
                }
            }
        }
}
