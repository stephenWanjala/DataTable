package io.github.stephenwanjala.datatable

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
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
 * - Column resizing via drag handles
 * - Grouping with custom group header and summary rows
 * - Pagination with configurable items-per-page
 * - Column visibility toggle
 * - Text overflow / ellipsis per column
 * - Custom sort comparators
 * - Right-click context menu callback
 * - Keyboard navigation (arrow keys, Enter, Space, Home, End)
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
    // Scrollbars
    showScrollbars: Boolean = true,
    scrollbarThickness: Dp = 8.dp,
) {
    val scope = rememberCoroutineScope()
    val listState = state.lazyListState
    val horizontalScrollState = state.horizontalScrollState
    val flatHeaders = remember(headers) { flattenHeaders(headers) }

    // Partition into frozen and scrollable
    val (frozenHeaders, scrollableHeaders) = remember(flatHeaders) {
        flatHeaders.partition { it.fixed && it.width != null }
    }
    val hasFrozenColumns = frozenHeaders.isNotEmpty()

    var currentSortState by remember(sortBy) { mutableStateOf(sortBy) }
    var currentMultiSort by remember(multiSortBy) { mutableStateOf(multiSortBy) }
    var currentPageState by remember(currentPage) { mutableStateOf(currentPage) }

    val showCheckboxes = selectionMode != SelectionMode.NONE && showSelect

    // Keys for every item, used by select-all and by the header's "all selected" state.
    // Kept in a `remember` so the containsAll scan does not run on every recomposition.
    val allKeys = remember(items, itemKey) {
        items.mapTo(LinkedHashSet<Any>(items.size), itemKey)
    }
    val allSelected = remember(allKeys, selectedKeys) {
        allKeys.isNotEmpty() && selectedKeys.containsAll(allKeys)
    }

    val processedItems = remember(items, groupBy, currentSortState, currentMultiSort, currentPageState, showPagination, itemsPerPage) {
        var result = items

        // Build active sort list: multi-sort takes precedence
        val activeSorts = currentMultiSort.ifEmpty {
            listOfNotNull(currentSortState.takeIf { it.order != SortOrder.NONE })
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

        // Apply pagination
        if (showPagination) {
            val start = currentPageState * itemsPerPage
            val end = minOf(start + itemsPerPage, result.size)
            if (start < result.size) result.subList(start, end) else emptyList()
        } else {
            result
        }
    }

    val totalPages = remember(items.size, itemsPerPage, showPagination) {
        if (showPagination && itemsPerPage > 0) (items.size + itemsPerPage - 1) / itemsPerPage else 1
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

    // Focus for keyboard navigation
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .background(colors.container)
            .focusRequester(focusRequester)
            .focusable()
            .dataTableKeyboardNavigation(
                state = state,
                rowKeys = rowKeys,
                scrollIndices = rowScrollIndices,
                scope = scope,
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
                            frozenContent = {
                                DataTableHeaderRow(
                                    headers = frozenHeaders,
                                    showSelect = showCheckboxes,
                                    allSelected = allSelected,
                                    onSelectAll = {
                                        onSelectionChange?.invoke(if (allSelected) emptySet() else allKeys)
                                    },
                                    showExpand = showExpand,
                                    density = density,
                                    sortState = currentSortState,
                                    onSortChange = { newSort ->
                                        currentSortState = newSort
                                        onSortChange?.invoke(newSort)
                                    },
                                    backgroundColor = colors.header,
                                    colors = colors,
                                    textStyles = textStyles,
                                    selectionMode = selectionMode,
                                    multiSortBy = currentMultiSort,
                                    onMultiSortChange = { newMulti ->
                                        currentMultiSort = newMulti
                                        onMultiSortChange?.invoke(newMulti)
                                    },
                                    resizableColumns = resizableColumns,
                                    minColumnWidth = minColumnWidth,
                                    state = state,
                                )
                            },
                            scrollableContent = {
                                DataTableHeaderRow(
                                    headers = scrollableHeaders,
                                    showSelect = false,
                                    allSelected = false,
                                    onSelectAll = {},
                                    showExpand = false,
                                    density = density,
                                    sortState = currentSortState,
                                    onSortChange = { newSort ->
                                        currentSortState = newSort
                                        onSortChange?.invoke(newSort)
                                    },
                                    backgroundColor = colors.header,
                                    colors = colors,
                                    textStyles = textStyles,
                                    selectionMode = selectionMode,
                                    multiSortBy = currentMultiSort,
                                    onMultiSortChange = { newMulti ->
                                        currentMultiSort = newMulti
                                        onMultiSortChange?.invoke(newMulti)
                                    },
                                    resizableColumns = resizableColumns,
                                    minColumnWidth = minColumnWidth,
                                    state = state,
                                )
                            },
                        )
                    }
                    else {
                        Row(
                            modifier = Modifier
                                .background(colors.header)
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                                .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                        ) {
                            DataTableHeaderRow(
                                headers = flatHeaders,
                                showSelect = showCheckboxes,
                                allSelected = allSelected,
                                onSelectAll = {
                                    onSelectionChange?.invoke(if (allSelected) emptySet() else allKeys)
                                },
                                showExpand = showExpand,
                                density = density,
                                sortState = currentSortState,
                                onSortChange = { newSort ->
                                    currentSortState = newSort
                                    onSortChange?.invoke(newSort)
                                },
                                backgroundColor = colors.header,
                                colors = colors,
                                textStyles = textStyles,
                                selectionMode = selectionMode,
                                multiSortBy = currentMultiSort,
                                onMultiSortChange = { newMulti ->
                                    currentMultiSort = newMulti
                                    onMultiSortChange?.invoke(newMulti)
                                },
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

            TableDivider(colors.divider)

            // ---- Body ----
            Box(modifier = Modifier.weight(1f)) {
                when {
                    loading -> loadingContent?.invoke() ?: DefaultLoadingContent(textStyles)

                    processedItems.isEmpty() -> noDataContent?.invoke() ?: DefaultNoDataContent(textStyles)

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
                                                        )
                                                    },
                                                    scrollableContent = {
                                                        DataTableRow(
                                                            item = item,
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
                        currentPage = currentPageState,
                        totalPages = totalPages,
                        totalItems = items.size,
                        itemsPerPage = itemsPerPage,
                        onPageChange = { newPage ->
                            currentPageState = newPage
                            onPageChange?.invoke(newPage)
                        },
                        colors = colors,
                        textStyles = textStyles,
                        itemsPerPageOptions = itemsPerPageOptions,
                        onItemsPerPageChange = onItemsPerPageChange?.let { callback ->
                            { newSize ->
                                currentPageState = 0
                                callback(newSize)
                            }
                        },
                    )
                } else {
                    DefaultFooter(itemCount = items.size, colors = colors, textStyles = textStyles)
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
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Modifier.enableTrackpadHorizontalScroll(scrollState: ScrollState): Modifier {
    val scope = rememberCoroutineScope()
    return this
        .pointerInput(scrollState) {
            detectHorizontalDragGestures { _, dragAmount ->
                scope.launch {
                    scrollState.scrollBy(-dragAmount)
                }
            }
        }
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
