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
 * Renders the header, including optional select-all checkbox and expand spacer.
 *
 * [headers] is the header *tree*, not a flattened list. A header with children renders as a
 * group: its title sits in a band above its children, spanning them. A header without children
 * renders as a column and stretches over the full header height, so a leaf sitting beside a
 * deeply nested group is vertically centred rather than leaving blank rows above it.
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
    // Intrinsic height is only needed to let leaves stretch alongside groups. Flat headers skip
    // it, so the common case measures exactly as it did before nesting existed.
    val hasGroups = headers.any { visibleChildren(it) != null }

    Row(
        modifier = Modifier
            .background(backgroundColor)
            .then(if (hasGroups) Modifier.height(IntrinsicSize.Min) else Modifier)
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
            if (header.visible) {
                HeaderNode(
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
                    stretch = hasGroups,
                    state = state,
                )
            }
        }
    }
}

/**
 * Renders one node of the header tree — a column, or a group spanning its children.
 *
 * @param stretch Whether to fill the header's full height. Set once groups are present, so that
 *                leaves span every level instead of leaving the space above them empty.
 */
@Composable
private fun <T> RowScope.HeaderNode(
    header: DataTableHeader<T>,
    density: DataTableDensity,
    sortState: SortState,
    onSortChange: (SortState) -> Unit,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    multiSortBy: List<SortState>,
    onMultiSortChange: ((List<SortState>) -> Unit)?,
    resizableColumns: Boolean,
    minColumnWidth: Dp,
    stretch: Boolean,
    state: DataTableState?,
) {
    val children = visibleChildren(header)
    val leaves = leavesOf(header)
    val widths = leaves.map { leaf -> state?.resolvedColumnWidth(leaf.key, leaf.width) ?: leaf.width }
    val allFixed = widths.all { it != null }

    require(allFixed || widths.all { it == null }) {
        "Header group '${header.key}' spans a mix of fixed-width and weighted columns, so its " +
            "width cannot be resolved. Give every column under it an explicit width, or leave " +
            "them all weighted."
    }

    // A group is exactly as wide as the columns beneath it, resize overrides included.
    val sizeModifier = if (allFixed) {
        Modifier.width(widths.fold(0.dp) { total, width -> total + width!! })
    } else {
        Modifier.weight(leaves.size.toFloat())
    }
    val cellModifier = if (stretch) sizeModifier.fillMaxHeight() else sizeModifier

    if (children == null) {
        DataTableHeaderLeaf(
            header = header,
            modifier = cellModifier,
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
    } else {
        Column(modifier = cellModifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (header.headerContent != null) {
                    header.headerContent.invoke()
                } else {
                    BasicText(
                        text = header.title,
                        style = textStyles.headerCell.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Underline spanning the group — what reads as "these columns belong together".
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(1.dp)
                    .background(colors.divider)
            )

            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                children.forEach { child ->
                    HeaderNode(
                        header = child,
                        density = density,
                        sortState = sortState,
                        onSortChange = onSortChange,
                        colors = colors,
                        textStyles = textStyles,
                        multiSortBy = multiSortBy,
                        onMultiSortChange = onMultiSortChange,
                        resizableColumns = resizableColumns,
                        minColumnWidth = minColumnWidth,
                        stretch = true,
                        state = state,
                    )
                }
            }
        }
    }
}

/**
 * Header cell for a single leaf column: title, sort indicator, and resize handle.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun <T> DataTableHeaderLeaf(
    header: DataTableHeader<T>,
    modifier: Modifier,
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
    // `pointerInput(Unit)` never restarts, so its lambda would otherwise keep reading the values
    // captured on first composition — leaving the sort cycle stuck on ASCENDING because it always
    // saw the initial, empty sort state. Same reason the body row does this for its tap handler.
    val currentSortState by rememberUpdatedState(sortState)
    val currentMultiSortBy by rememberUpdatedState(multiSortBy)
    val currentOnSortChange by rememberUpdatedState(onSortChange)
    val currentOnMultiSortChange by rememberUpdatedState(onMultiSortChange)

    // Determine sort indicator for this column
    val multiSortIndex = multiSortBy.indexOfFirst { it.key == header.key }
    val isMultiSorted = multiSortIndex >= 0
    val activeSortState = when {
        isMultiSorted -> multiSortBy[multiSortIndex]
        sortState.key == header.key -> sortState
        else -> null
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
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

                                            val multiSortHandler = currentOnMultiSortChange
                                            if (isCtrl && multiSortHandler != null) {
                                                // Multi-sort: Ctrl+click
                                                val existing = currentMultiSortBy.toMutableList()
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
                                                multiSortHandler(existing)
                                            } else {
                                                // Single sort
                                                val active = currentSortState
                                                val newOrder = when {
                                                    active.key != header.key -> SortOrder.ASCENDING
                                                    active.order == SortOrder.ASCENDING -> SortOrder.DESCENDING
                                                    else -> SortOrder.NONE
                                                }
                                                currentOnSortChange(SortState(header.key, newOrder))
                                                // Clear multi-sort when doing single sort
                                                multiSortHandler?.invoke(emptyList())
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
