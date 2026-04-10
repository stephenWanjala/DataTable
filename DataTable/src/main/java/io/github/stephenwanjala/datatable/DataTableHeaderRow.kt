package io.github.stephenwanjala.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor

/**
 * Renders the default header row, including optional select-all checkbox and expand spacer.
 */
@Composable
internal fun <T> DataTableHeaderRow(
    headers: List<DataTableHeader<T>>,
    showSelect: Boolean,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    showExpand: Boolean,
    density: DataTableDensity,
    sortState: SortState,
    onSortChange: (SortState) -> Unit,
    backgroundColor: Color,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    selectionMode: SelectionMode = SelectionMode.MULTI,
    multiSortBy: List<SortState> = emptyList(),
    onMultiSortChange: ((List<SortState>) -> Unit)? = null,
    resizableColumns: Boolean = false,
    minColumnWidth: Dp = 40.dp,
    state: DataTableState? = null,
) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .padding(vertical = density.verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Select-all checkbox (only for MULTI mode)
        if (showSelect && selectionMode == SelectionMode.MULTI) {
            SimpleCheckbox(
                checked = allSelected,
                onCheckedChange = { onSelectAll() },
                modifier = Modifier.padding(horizontal = density.horizontalPadding),
                colors = colors,
            )
        } else if (showSelect && selectionMode == SelectionMode.SINGLE) {
            // Reserve space for alignment but no select-all in single mode
            Spacer(modifier = Modifier.width(20.dp + density.horizontalPadding * 2))
        }

        if (showExpand) {
            Spacer(modifier = Modifier.width(48.dp))
        }

        headers.forEach { header ->
            DataTableHeaderCell(
                header = header,
                density = density,
                sortState = sortState,
                onSortChange = onSortChange,
                colors = colors,
                textStyles = textStyles,
                multiSortBy = multiSortBy,
                onMultiSortChange = onMultiSortChange,
                resizableColumns = resizableColumns,
                minColumnWidth = minColumnWidth,
                state = state,
            )
        }
    }
}

/**
 * Header cell for a single column in the default header row.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun <T> RowScope.DataTableHeaderCell(
    header: DataTableHeader<T>,
    density: DataTableDensity,
    sortState: SortState,
    onSortChange: (SortState) -> Unit,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    multiSortBy: List<SortState> = emptyList(),
    onMultiSortChange: ((List<SortState>) -> Unit)? = null,
    resizableColumns: Boolean = false,
    minColumnWidth: Dp = 40.dp,
    state: DataTableState? = null,
) {
    val resolvedWidth = state?.resolvedColumnWidth(header.key, header.width) ?: header.width
    val weight = if (resolvedWidth == null) 1f else 0f
    val cellModifier = if (resolvedWidth != null) {
        Modifier.width(resolvedWidth)
    } else {
        Modifier.weight(weight)
    }

    // Determine sort indicator for this column
    val multiSortIndex = multiSortBy.indexOfFirst { it.key == header.key }
    val isMultiSorted = multiSortIndex >= 0
    val activeSortState = when {
        isMultiSorted -> multiSortBy[multiSortIndex]
        sortState.key == header.key -> sortState
        else -> null
    }

    Row(modifier = cellModifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (header.sortable) {
                        Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Press) {
                                        val change = event.changes.firstOrNull() ?: continue
                                        if (change.pressed) {
                                            change.consume()
                                            val isCtrl = event.keyboardModifiers.isCtrlPressed

                                            if (isCtrl && onMultiSortChange != null) {
                                                // Multi-sort: Ctrl+click
                                                val existing = multiSortBy.toMutableList()
                                                val idx = existing.indexOfFirst { it.key == header.key }
                                                if (idx >= 0) {
                                                    val current = existing[idx]
                                                    when (current.order) {
                                                        SortOrder.ASCENDING -> existing[idx] = current.copy(order = SortOrder.DESCENDING)
                                                        SortOrder.DESCENDING -> existing.removeAt(idx)
                                                        SortOrder.NONE -> existing.removeAt(idx)
                                                    }
                                                } else {
                                                    existing.add(SortState(header.key, SortOrder.ASCENDING))
                                                }
                                                onMultiSortChange(existing)
                                            } else {
                                                // Single sort
                                                val newOrder = when {
                                                    sortState.key != header.key -> SortOrder.ASCENDING
                                                    sortState.order == SortOrder.ASCENDING -> SortOrder.DESCENDING
                                                    else -> SortOrder.NONE
                                                }
                                                onSortChange(SortState(header.key, newOrder))
                                                // Clear multi-sort when doing single sort
                                                onMultiSortChange?.invoke(emptyList())
                                            }
                                        }
                                    }
                                }
                            }
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
                    BasicText(
                        text = header.title,
                        style = textStyles.headerCell.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = header.align
                        ),
                        maxLines = header.maxLines,
                        overflow = header.overflow,
                    )

                    if (activeSortState != null && activeSortState.order != SortOrder.NONE) {
                        VectorIcon(
                            imageVector = if (activeSortState.order == SortOrder.ASCENDING) {
                                KeyboardArrowUp
                            } else {
                                ArrowDropDown
                            },
                            contentDescription = "Sort",
                            modifier = Modifier.size(16.dp),
                            tint = colors.iconTint,
                        )

                        // Show priority number for multi-sort
                        if (isMultiSorted && multiSortBy.size > 1) {
                            BasicText(
                                text = "${multiSortIndex + 1}",
                                style = textStyles.headerCell.copy(
                                    fontSize = textStyles.headerCell.fontSize * 0.7f,
                                    color = colors.iconTint,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // Column resize handle
        if (resizableColumns && state != null) {
            ColumnResizeHandle(
                onResize = { delta ->
                    val currentWidth = state.resolvedColumnWidth(header.key, header.width) ?: 100.dp
                    val newWidth = (currentWidth + delta).coerceAtLeast(minColumnWidth)
                    state.columnWidths[header.key] = newWidth
                },
            )
        }
    }
}

/**
 * Draggable handle on the trailing edge of a header cell for column resizing.
 */
@Composable
internal fun ColumnResizeHandle(
    onResize: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .width(6.dp)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    with(density) {
                        onResize(dragAmount.toDp())
                    }
                }
            }
            .background(Color.Transparent)
    )
}
