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
 * @param T The item type for each row.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun <T> DataTable(
    items: List<T>,
    headers: List<DataTableHeader<T>>,
    modifier: Modifier = Modifier,
    state: DataTableState = rememberDataTableState(),
    itemKey: (T) -> Any = { it.hashCode() },
    // Selection
    showSelect: Boolean = false,
    selectionMode: SelectionMode = if (showSelect) SelectionMode.MULTI else SelectionMode.NONE,
    selectedItems: Set<T> = emptySet(),
    onSelectionChange: ((Set<T>) -> Unit)? = null,
    // Expansion
    showExpand: Boolean = false,
    expandedItems: Set<T> = emptySet(),
    onExpandChange: ((Set<T>) -> Unit)? = null,
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

    // Focus for keyboard navigation
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .background(colors.container)
            .focusRequester(focusRequester)
            .focusable()
            .dataTableKeyboardNavigation(
                state = state,
                itemCount = processedItems.size,
                scope = scope,
                onRowClick = { index ->
                    if (index in processedItems.indices) {
                        onRowClick?.invoke(processedItems[index])
                    }
                },
                onToggleSelection = { index ->
                    if (index in processedItems.indices) {
                        val item = processedItems[index]
                        when (selectionMode) {
                            SelectionMode.SINGLE -> onSelectionChange?.invoke(setOf(item))
                            SelectionMode.MULTI -> {
                                val newSelection = if (selectedItems.contains(item))
                                    selectedItems - item else selectedItems + item
                                onSelectionChange?.invoke(newSelection)
                            }
                            SelectionMode.NONE -> {}
                        }
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
                                    allSelected = items.isNotEmpty() && selectedItems.containsAll(items),
                                    onSelectAll = {
                                        onSelectionChange?.invoke(
                                            if (selectedItems.containsAll(items)) emptySet() else items.toSet()
                                        )
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
                                allSelected = items.isNotEmpty() && selectedItems.containsAll(items),
                                onSelectAll = {
                                    onSelectionChange?.invoke(
                                        if (selectedItems.containsAll(items)) emptySet() else items.toSet()
                                    )
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
                            var globalRowIndex = 0

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                groupedItems.forEach { (group, groupItems) ->
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
                                        val rowIndex = globalRowIndex + localIndex
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
                                                            selected = selectedItems.contains(item),
                                                            onSelectChange = { checked ->
                                                                handleSelection(selectionMode, checked, item, selectedItems, onSelectionChange)
                                                            },
                                                            showExpand = showExpand,
                                                            expanded = expandedItems.contains(item),
                                                            onExpandChange = { exp ->
                                                                val newExpanded = if (exp) expandedItems + item else expandedItems - item
                                                                onExpandChange?.invoke(newExpanded)
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
                                                            isFocused = state.focusedRowIndex == rowIndex,
                                                            state = state,
                                                        )
                                                    },
                                                    scrollableContent = {
                                                        DataTableRow(
                                                            item = item,
                                                            headers = scrollableHeaders,
                                                            showSelect = false,
                                                            selected = selectedItems.contains(item),
                                                            onSelectChange = { checked ->
                                                                handleSelection(selectionMode, checked, item, selectedItems, onSelectionChange)
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
                                                            isFocused = state.focusedRowIndex == rowIndex,
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
                                                            selected = selectedItems.contains(item),
                                                            onSelectChange = { checked ->
                                                                handleSelection(selectionMode, checked, item, selectedItems, onSelectionChange)
                                                            },
                                                            showExpand = showExpand,
                                                            expanded = expandedItems.contains(item),
                                                            onExpandChange = { exp ->
                                                                val newExpanded = if (exp) expandedItems + item else expandedItems - item
                                                                onExpandChange?.invoke(newExpanded)
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
                                                            isFocused = state.focusedRowIndex == rowIndex,
                                                            state = state,
                                                        )
                                                    }
                                                }
                                            }

                                            // Expanded content
                                            if (showExpand && expandedItems.contains(item) && expandContent != null) {
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

                                    globalRowIndex += groupItems.size
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
 */
private fun <T> handleSelection(
    mode: SelectionMode,
    checked: Boolean,
    item: T,
    currentSelection: Set<T>,
    onSelectionChange: ((Set<T>) -> Unit)?,
) {
    when (mode) {
        SelectionMode.SINGLE -> onSelectionChange?.invoke(if (checked) setOf(item) else emptySet())
        SelectionMode.MULTI -> {
            val newSelection = if (checked) currentSelection + item else currentSelection - item
            onSelectionChange?.invoke(newSelection)
        }
        SelectionMode.NONE -> {}
    }
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
