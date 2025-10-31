package io.github.stephenwanjala.datatable

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Describes a column in the `DataTable`.
 *
 * Use this to define how each column appears (title, width, alignment),
 * how values are derived from your row item (`value`), whether it is sortable,
 * and optionally provide fully custom composable content for the header and cells.
 *
 * Columns can be nested via `children` to model complex header hierarchies.
 * Only leaf columns (those without `children`) are rendered as actual cells in the body.
 *
 * @param key Stable unique identifier for this column. Also used for sorting.
 * @param title Text label shown in the default header when `headerContent` is not provided.
 * @param value Extractor invoked for each row item to produce a displayable value when
 *              `cellContent` is not provided. The returned value is converted to text via `toString()`.
 * @param sortable Whether this column supports sorting via the built-in header interactions.
 * @param width Fixed width for the column. If `null`, the column is laid out with weight to fill space.
 * @param align Horizontal alignment for header text and default cell text.
 * @param fixed Reserved for future use to indicate a fixed (frozen) column. Currently not applied.
 * @param children Optional nested headers. When provided, only the leaf headers are displayed as cells.
 * @param headerContent Optional composable to fully customize the header cell for this column.
 * @param cellContent Optional composable to fully customize each body cell for this column.
 */
data class DataTableHeader<T>(
    val key: String,
    val title: String = "",
    val value: ((T) -> Any?)? = null,
    val sortable: Boolean = true,
    val width: Dp? = null,
    val align: TextAlign = TextAlign.Start,
    val fixed: Boolean = false,
    val children: List<DataTableHeader<T>>? = null,
    val headerContent: (@Composable () -> Unit)? = null,
    val cellContent: (@Composable (T) -> Unit)? = null
)

/**
 * Represents the sort order applied to a column.
 *
 * - `ASCENDING` sorts values from low to high (A→Z, 0→9).
 * - `DESCENDING` sorts values from high to low (Z→A, 9→0).
 * - `NONE` means no active sort on the column.
 */
enum class SortOrder {
    ASCENDING, DESCENDING, NONE
}

/**
 * Current sort configuration for the table.
 *
 * @param key The column `key` to which sorting is applied. Empty means no column selected.
 * @param order The direction of sorting. Use `SortOrder.NONE` for no sorting.
 */
data class SortState(
    val key: String = "",
    val order: SortOrder = SortOrder.NONE
)

/**
 * Density presets that control vertical and horizontal padding for rows and headers.
 *
 * Use a denser preset to fit more content on screen, or the default for readability.
 *
 * @property verticalPadding Vertical padding applied to rows and header cells.
 * @property horizontalPadding Horizontal padding applied to cells.
 */
public enum class DataTableDensity(val verticalPadding: Dp, val horizontalPadding: Dp) {
    DEFAULT(16.dp, 16.dp),
    COMFORTABLE(12.dp, 16.dp),
    COMPACT(8.dp, 12.dp)
}

/**
 * A flexible, Compose Multiplatform data table with horizontal and vertical scrolling.
 *
 * Features:
 * - Header with optional built‑in sorting per column
 * - Row selection and row expansion
 * - Optional grouping with custom group header and summary rows
 * - Pagination footer (optional)
 * - Customizable density and colors
 * - Pluggable header/footer/empty/loading content
 * - Optional scrollbars and trackpad/mouse horizontal scrolling
 *
 * Type parameter `T` represents the item type for each row.
 * Only leaf headers (see `DataTableHeader.children`) render table body cells.
 *
 * @param items The list of items to render as rows.
 * @param headers Column definitions. Nested structures are flattened and only leaf columns are rendered.
 * @param modifier Optional `Modifier` for the outer `Surface`.
 * @param itemKey Provides a stable, unique key for each item for list diffing.
 * @param showSelect When true, shows a leading checkbox for row selection. Also adds a header checkbox for select‑all.
 * @param selectedItems The current set of selected items when `showSelect` is true.
 * @param onSelectionChange Callback invoked with the updated selection set.
 * @param showExpand When true, shows a leading expand/collapse control per row.
 * @param expandedItems The current set of expanded items when `showExpand` is true.
 * @param onExpandChange Callback invoked with the updated expanded set.
 * @param expandContent Composable providing the expanded row content when a row is expanded.
 * @param density Density preset controlling cell paddings.
 * @param sortBy Current sort state. Sorting is applied when `order != NONE` and a matching header key exists.
 * @param onSortChange Callback invoked when the user changes sorting via header interaction.
 * @param hideDefaultHeader When true, omits the built‑in header. Use `headerContent` for custom headers.
 * @param hideDefaultFooter When true, omits the built‑in footer. Use `footerContent` for custom footers.
 * @param loading When true, displays loading UI instead of the table body.
 * @param loadingContent Optional custom loading UI.
 * @param headerContent Optional custom header content. Rendered instead of the default header when provided.
 * @param footerContent Optional custom footer content. Rendered instead of the default footer when provided.
 * @param noDataContent Optional UI shown when there are no `items`.
 * @param groupBy Optional function to group items. When provided, the list is grouped by the returned key.
 * @param groupHeaderContent Optional UI at the start of each group. Requires `groupBy`.
 * @param groupSummaryContent Optional UI at the end of each group. Requires `groupBy`.
 * @param onRowClick Optional single‑click handler for rows.
 * @param onRowDoubleClick Optional double‑click handler for rows.
 * @param containerColor Color of the outer `Surface` containing the table.
 * @param headerColor Background color of the default header row.
 * @param showPagination When true, enables paginated view with `itemsPerPage` and `currentPage`.
 * @param itemsPerPage Number of items per page when pagination is enabled.
 * @param currentPage Zero‑based page index when pagination is enabled.
 * @param onPageChange Callback invoked with the new page index when pagination controls are used.
 * @param showScrollbars When true, shows scrollbars for both header and body scroll containers.
 * @param scrollbarThickness Thickness for the scrollbars (currently decorative; adapter controls size).
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun <T> DataTable(
    items: List<T>,
    headers: List<DataTableHeader<T>>,
    modifier: Modifier = Modifier,
    itemKey: (T) -> Any = { it.hashCode() },
    showSelect: Boolean = false,
    selectedItems: Set<T> = emptySet(),
    onSelectionChange: ((Set<T>) -> Unit)? = null,
    showExpand: Boolean = false,
    expandedItems: Set<T> = emptySet(),
    onExpandChange: ((Set<T>) -> Unit)? = null,
    expandContent: (@Composable (T) -> Unit)? = null,
    density: DataTableDensity = DataTableDensity.DEFAULT,
    sortBy: SortState = SortState(),
    onSortChange: ((SortState) -> Unit)? = null,
    hideDefaultHeader: Boolean = false,
    hideDefaultFooter: Boolean = false,
    loading: Boolean = false,
    loadingContent: (@Composable () -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    noDataContent: (@Composable () -> Unit)? = null,
    groupBy: ((T) -> String)? = null,
    groupHeaderContent: (@Composable (String, List<T>) -> Unit)? = null,
    groupSummaryContent: (@Composable (String, List<T>) -> Unit)? = null,
    onRowClick: ((T) -> Unit)? = null,
    onRowDoubleClick: ((T) -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    headerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showPagination: Boolean = false,
    itemsPerPage: Int = 10,
    currentPage: Int = 0,
    onPageChange: ((Int) -> Unit)? = null,
    showScrollbars: Boolean = true,
    scrollbarThickness: Dp = 8.dp,
) {
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val flatHeaders = remember(headers) { flattenHeaders(headers) }

    var currentSortState by remember(sortBy) { mutableStateOf(sortBy) }
    var currentPageState by remember(currentPage) { mutableStateOf(currentPage) }

    val processedItems = remember(items, groupBy, currentSortState, currentPageState, showPagination, itemsPerPage) {
        var result = items

        // Apply sorting
        if (currentSortState.order != SortOrder.NONE && currentSortState.key.isNotEmpty()) {
            val header = flatHeaders.find { it.key == currentSortState.key }
            if (header != null) {
                result = result.sortedWith { a, b ->
                    val valueA = header.value?.invoke(a) ?: ""
                    val valueB = header.value?.invoke(b) ?: ""
                    val comparison = compareValues(valueA as? Comparable<Any>, valueB as? Comparable<Any>)
                    if (currentSortState.order == SortOrder.ASCENDING) comparison else -comparison
                }
            }
        }

        // Apply pagination
        if (showPagination) {
            val start = currentPageState * itemsPerPage
            val end = minOf(start + itemsPerPage, result.size)
            if (start < result.size) {
                result.subList(start, end)
            } else {
                emptyList()
            }
        } else {
            result
        }
    }

    val totalPages = remember(items.size, itemsPerPage, showPagination) {
        if (showPagination && itemsPerPage > 0) {
            (items.size + itemsPerPage - 1) / itemsPerPage
        } else {
            1
        }
    }

    val groupedItems = remember(processedItems, groupBy) {
        if (groupBy != null) {
            processedItems.groupBy(groupBy)
        } else {
            mapOf("" to processedItems)
        }
    }

    Surface(
        modifier = modifier,
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Box {
            Column {
                // Header
                if (!hideDefaultHeader && headerContent == null) {
                    Box {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState)
                                .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                                .fillMaxWidth()
                        ) {
                            DataTableHeaderRow(
                                headers = flatHeaders,
                                showSelect = showSelect,
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
                                backgroundColor = headerColor
                            )
                        }

                        // Horizontal scrollbar for header
                        if (showScrollbars) {
                            HorizontalScrollbar(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(),
                                adapter = rememberScrollbarAdapter(horizontalScrollState)
                            )
                        }
                    }
                } else if (headerContent != null) {
                    headerContent()
                }

                HorizontalDivider()

                // Body
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        loading -> {
                            loadingContent?.invoke() ?: DefaultLoadingContent()
                        }

                        processedItems.isEmpty() -> {
                            noDataContent?.invoke() ?: DefaultNoDataContent()
                        }

                        else -> {
                            Box {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    groupedItems.forEach { (group, groupItems) ->
                                        // Group header
                                        if (groupBy != null && groupHeaderContent != null) {
                                            item(key = "group-header-$group") {
                                                Box {
                                                    Row(
                                                        modifier = Modifier
                                                            .horizontalScroll(horizontalScrollState)
                                                            .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                                                            .fillMaxWidth()
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
                                        ) { _, item ->
                                            Column {
                                                Box {
                                                    Row(
                                                        modifier = Modifier
                                                            .horizontalScroll(horizontalScrollState)
                                                            .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                                                            .fillMaxWidth()
                                                    ) {
                                                        DataTableRow(
                                                            item = item,
                                                            headers = flatHeaders,
                                                            showSelect = showSelect,
                                                            selected = selectedItems.contains(item),
                                                            onSelectChange = {
                                                                val newSelection = if (it) {
                                                                    selectedItems + item
                                                                } else {
                                                                    selectedItems - item
                                                                }
                                                                onSelectionChange?.invoke(newSelection)
                                                            },
                                                            showExpand = showExpand,
                                                            expanded = expandedItems.contains(item),
                                                            onExpandChange = {
                                                                val newExpanded = if (it) {
                                                                    expandedItems + item
                                                                } else {
                                                                    expandedItems - item
                                                                }
                                                                onExpandChange?.invoke(newExpanded)
                                                            },
                                                            density = density,
                                                            onClick = onRowClick?.let { { onRowClick(item) } },
                                                            onDoubleClick = onRowDoubleClick?.let {
                                                                {
                                                                    onRowDoubleClick(
                                                                        item
                                                                    )
                                                                }
                                                            },
                                                            horizontalScrollState = horizontalScrollState
                                                        )
                                                    }
                                                }

                                                // Expanded content
                                                if (showExpand && expandedItems.contains(item) && expandContent != null) {
                                                    Surface(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    ) {
                                                        Box(modifier = Modifier.padding(density.verticalPadding)) {
                                                            expandContent(item)
                                                        }
                                                    }
                                                }

                                                HorizontalDivider()
                                            }
                                        }

                                        // Group summary
                                        if (groupBy != null && groupSummaryContent != null) {
                                            item(key = "group-summary-$group") {
                                                Box {
                                                    Row(
                                                        modifier = Modifier
                                                            .horizontalScroll(horizontalScrollState)
                                                            .enableTrackpadHorizontalScroll(scrollState = horizontalScrollState)
                                                            .fillMaxWidth()
                                                    ) {
                                                        groupSummaryContent(group, groupItems)
                                                    }
                                                }
                                                HorizontalDivider()
                                            }
                                        }
                                    }
                                }

                                // Vertical scrollbar for body
                                if (showScrollbars) {
                                    VerticalScrollbar(
                                        modifier = Modifier
                                            .pointerHoverIcon(icon = PointerIcon.Hand)
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight(),
                                        adapter = rememberScrollbarAdapter(listState)
                                    )
                                }

                                // Horizontal scrollbar for body
                                if (showScrollbars) {
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

                // Footer
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
                            }
                        )
                    } else {
                        DefaultFooter(itemCount = items.size)
                    }
                } else if (footerContent != null) {
                    HorizontalDivider()
                    footerContent()
                }
            }
        }
    }
}

/**
 * Renders the default header row, including optional select‑all checkbox and expand spacer.
 *
 * Internal: used by `DataTable` when `hideDefaultHeader` is false and no custom `headerContent` is provided.
 */
@Composable
private fun <T> DataTableHeaderRow(
    headers: List<DataTableHeader<T>>,
    showSelect: Boolean,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    showExpand: Boolean,
    density: DataTableDensity,
    sortState: SortState,
    onSortChange: (SortState) -> Unit,
    backgroundColor: Color
) {
    Surface(
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = density.verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSelect) {
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { onSelectAll() },
                    modifier = Modifier.padding(horizontal = density.horizontalPadding)
                )
            }

            if (showExpand) {
                Spacer(modifier = Modifier.width(48.dp))
            }

            headers.forEach { header ->
                DataTableHeaderCell(
                    header = header,
                    density = density,
                    sortState = sortState,
                    onSortChange = onSortChange
                )
            }
        }
    }
}

/**
 * Header cell for a single column in the default header row.
 *
 * Handles sort toggling when the column is `sortable` and reflects the current `SortState`.
 */
@Composable
private fun <T> RowScope.DataTableHeaderCell(
    header: DataTableHeader<T>,
    density: DataTableDensity,
    sortState: SortState,
    onSortChange: (SortState) -> Unit
) {
    val weight = if (header.width == null) 1f else 0f
    val modifier = if (header.width != null) {
        Modifier.width(header.width)
    } else {
        Modifier.weight(weight)
    }

    Box(
        modifier = modifier
            .then(
                if (header.sortable) {
                    Modifier.clickable {
                        val newOrder = when {
                            sortState.key != header.key -> SortOrder.ASCENDING
                            sortState.order == SortOrder.ASCENDING -> SortOrder.DESCENDING
                            else -> SortOrder.NONE
                        }
                        onSortChange(SortState(header.key, newOrder))
                    }
                } else Modifier
            )
            .padding(horizontal = density.horizontalPadding),
        contentAlignment = when (header.align) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        if (header.headerContent != null) {
            header.headerContent.invoke()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = header.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = header.align
                )

                if (header.sortable && sortState.key == header.key) {
                    Icon(
                        imageVector = if (sortState.order == SortOrder.ASCENDING) {
                            KeyboardArrowUp
                        } else {
                            ArrowDropDown
                        },
                        contentDescription = "Sort",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Renders a single data row for an item with optional selection and expansion controls.
 *
 * Internal: invoked by `DataTable` for each item in the (optionally grouped/paginated) list.
 * Supports click and double‑click handlers and highlights the selected state.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun <T> DataTableRow(
    item: T,
    headers: List<DataTableHeader<T>>,
    showSelect: Boolean,
    selected: Boolean,
    onSelectChange: (Boolean) -> Unit,
    showExpand: Boolean,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    density: DataTableDensity,
    onClick: (() -> Unit)?,
    onDoubleClick: (() -> Unit)?,
    horizontalScrollState: androidx.compose.foundation.ScrollState
) {

    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onSelectChange(!selected)
                            onClick?.invoke()
                        },
                        onDoubleTap = {
                            onSelectChange(!selected)
                            onDoubleClick?.invoke()
                        },
                        onLongPress = {}
                    )
                }
                .padding(vertical = density.verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSelect) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = onSelectChange,
                    modifier = Modifier.padding(horizontal = density.horizontalPadding)
                )
            }

            if (showExpand) {
                IconButton(
                    onClick = { onExpandChange(!expanded) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) KeyboardArrowUp else ArrowDropDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            headers.forEach { header ->
                DataTableCell(
                    item = item,
                    header = header,
                    density = density
                )
            }
        }
    }
}

/**
 * Body cell for a single item/column intersection.
 *
 * Uses `header.cellContent` when provided; otherwise renders the value from `header.value(item)` as text.
 */
@Composable
private fun <T> RowScope.DataTableCell(
    item: T,
    header: DataTableHeader<T>,
    density: DataTableDensity
) {
    val weight = if (header.width == null) 1f else 0f
    val modifier = if (header.width != null) {
        Modifier.width(header.width)
    } else {
        Modifier.weight(weight)
    }

    Box(
        modifier = modifier.padding(horizontal = density.horizontalPadding),
        contentAlignment = when (header.align) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        if (header.cellContent != null) {
            header.cellContent.invoke(item)
        } else {
            val value = header.value?.invoke(item) ?: ""
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = header.align
            )
        }
    }
}

/**
 * Default loading UI shown when `loading` is true and no `loadingContent` is provided.
 */
@Composable
private fun DefaultLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Loading...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Default empty‑state UI shown when there are no `items` and no `noDataContent` is provided.
 */
@Composable
private fun DefaultNoDataContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No data available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Default footer shown when `hideDefaultFooter` is false and `showPagination` is false.
 * Displays a simple count of rendered items.
 */
@Composable
private fun DefaultFooter(itemCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Default pagination footer with first/prev/next/last controls and range indicator.
 *
 * Shown when `showPagination` is true and the default footer is enabled.
 * `onPageChange` receives zero‑based page indexes.
 */
@Composable
private fun PaginationFooter(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    itemsPerPage: Int,
    onPageChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val startItem = currentPage * itemsPerPage + 1
            val endItem = minOf((currentPage + 1) * itemsPerPage, totalItems)

            Text(
                text = "$startItem-$endItem of $totalItems",
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onPageChange(0) },
                    enabled = currentPage > 0
                ) {
                    Icon(KeyboardDoubleArrowLeft, "First page")
                }

                IconButton(
                    onClick = { onPageChange(currentPage - 1) },
                    enabled = currentPage > 0
                ) {
                    Icon(KeyboardArrowLeft, "Previous page")
                }

                Text(
                    text = "Page ${currentPage + 1} of $totalPages",
                    style = MaterialTheme.typography.bodyMedium
                )

                IconButton(
                    onClick = { onPageChange(currentPage + 1) },
                    enabled = currentPage < totalPages - 1
                ) {
                    Icon(imageVector=KeyboardArrowRight, "Next page")
                }

                IconButton(
                    onClick = { onPageChange(totalPages - 1) },
                    enabled = currentPage < totalPages - 1
                ) {
                    Icon(imageVector=KeyboardDoubleArrowRight, "Last page")
                }
            }
        }
    }
}

/**
 * Flattens a potentially nested header tree into a list of leaf headers.
 *
 * Only leaf headers (those without `children`) are rendered as table body columns.
 */
private fun <T> flattenHeaders(headers: List<DataTableHeader<T>>): List<DataTableHeader<T>> {
    val result = mutableListOf<DataTableHeader<T>>()

    fun flatten(headerList: List<DataTableHeader<T>>) {
        headerList.forEach { header ->
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

/**
 * Enables natural horizontal scrolling with trackpads and mice for horizontally scrollable content.
 *
 * This modifier:
 * - Handles horizontal drag gestures (primary method) and scrolls the provided `scrollState`.
 * - Converts vertical wheel/trackpad scroll into horizontal scroll when the Shift key is pressed.
 * - Also reacts to native horizontal scroll deltas where available.
 *
 * Apply this together with `Modifier.horizontalScroll(scrollState)` on the same container.
 *
 * @param scrollState The horizontal `ScrollState` to control.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.enableTrackpadHorizontalScroll(scrollState: ScrollState): Modifier {
    val scope = rememberCoroutineScope()
    return this
        .pointerInput(scrollState) {
            // Handle horizontal drag gestures (primary scrolling method)
            detectHorizontalDragGestures { _, dragAmount ->
                scope.launch {
                    scrollState.scrollBy(-dragAmount)
                }
            }
        }
        .pointerInput(scrollState) {
            // Keep your existing trackpad/mouse wheel handling
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.first().scrollDelta
                        val hScroll = when {
                            // Native horizontal gesture
                            delta.x != 0f -> delta.x
                            // Convert vertical to horizontal when Shift is pressed
                            event.keyboardModifiers.isShiftPressed -> delta.y
                            else -> 0f
                        }
                        if (hScroll != 0f) {
                            scope.launch {
                                // smaller multiplier feels more natural on touchpads
                                scrollState.scrollBy(-hScroll * 30f)
                            }
                        }
                    }
                }
            }
        }
}

