package io.github.stephenwanjala.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor

/** Thickness of the line marking where a dragged column will land. */
private val DropIndicatorWidth = 2.dp

/**
 * Shown over a header that can be dragged, since a column that moves has no other tell — the
 * header looks exactly like one that only sorts.
 */
private val ColumnMoveCursor = PointerIcon(Cursor(Cursor.MOVE_CURSOR))

/**
 * Renders the header, including optional select-all checkbox and expand spacer.
 *
 * [headers] is the header *tree*, not a flattened list. A header with children renders as a
 * group: its title sits in a band above its children, spanning them. A header without children
 * renders as a column and stretches over the full header height, so a leaf sitting beside a
 * deeply nested group is vertically centred rather than leaving blank rows above it.
 *
 * Each rendered header row owns its own [ColumnDragState], which is what keeps a reordering drag
 * inside its own section: the frozen headers and the scrolling ones are two rows.
 */
@Composable
internal fun <T> DataTableHeaderRow(
    headers: List<DataTableHeader<T>>,
    modifier: Modifier = Modifier,
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
    reorderableColumns: Boolean = false,
    state: DataTableState? = null,
) {
    val hasGroups = headers.any { visibleChildren(it) != null }

    // Lets leaves stretch alongside groups, and bounds what the resize handles fill. Without it
    // a flat, resizable header measures against the whole table and `ColumnResizeHandle`'s
    // `fillMaxHeight` stretches it over the rows, leaving the body with nothing.
    val sizeToContent = hasGroups || resizableColumns

    // Reordering writes through the state, so without one there is nowhere to record the result.
    val dragState = remember { ColumnDragState() }
    val drag = if (reorderableColumns && state != null) dragState else null

    val visibleHeaders = headers.filter { it.visible }

    Row(
        modifier = modifier
            .background(backgroundColor)
            .then(if (sizeToContent) Modifier.height(IntrinsicSize.Min) else Modifier)
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

        visibleHeaders.forEach { header ->
            HeaderNode(
                header = header,
                siblings = visibleHeaders,
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
                drag = drag,
                state = state,
            )
        }
    }
}

/**
 * Renders one node of the header tree — a column, or a group spanning its children.
 *
 * @param siblings The visible headers at this level, in display order. A reordering drag is
 *                 hit-tested against these and lands among them, which is what stops a column
 *                 being dragged out of the group it was declared in.
 * @param stretch Whether to fill the header's full height. Set once groups are present, so that
 *                leaves span every level instead of leaving the space above them empty.
 * @param drag The row's drag tracker, or `null` when columns are not reorderable.
 */
@Composable
private fun <T> RowScope.HeaderNode(
    header: DataTableHeader<T>,
    siblings: List<DataTableHeader<T>>,
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
    drag: ColumnDragState?,
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
    val cellModifier = (if (stretch) sizeModifier.fillMaxHeight() else sizeModifier)
        .columnDropDecoration(header.key, drag, colors)

    // A drop is expressed against the target's outermost leaf, because that is the column the
    // order list can name — the group itself is never in it.
    val onDrop: (ColumnDrop) -> Unit = drop@{ landing ->
        val table = state ?: return@drop
        val target = siblings.firstOrNull { it.key == landing.targetKey } ?: return@drop
        val targetLeaves = leavesOf(target)
        val anchor = (if (landing.after) targetLeaves.lastOrNull() else targetLeaves.firstOrNull())
            ?: return@drop
        table.moveColumnsBeside(leaves.map { it.key }, anchor.key, landing.after)
    }
    val siblingKeys = siblings.map { it.key }

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
            drag = drag,
            siblingKeys = siblingKeys,
            onDrop = onDrop,
            state = state,
        )
    } else {
        Column(modifier = cellModifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // The band is the group's drag handle: grabbing it moves every column under
                    // it as one block.
                    .headerPointerInput(
                        columnKey = header.key,
                        siblingKeys = siblingKeys,
                        drag = drag,
                        onDrop = onDrop,
                        onClick = null,
                    )
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
                        siblings = children,
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
                        drag = drag,
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
    drag: ColumnDragState? = null,
    siblingKeys: List<String> = emptyList(),
    onDrop: (ColumnDrop) -> Unit = {},
    state: DataTableState? = null,
) {
    // `pointerInput` restarts only when its keys change, so its lambda would otherwise keep
    // reading the values captured on first composition — leaving the sort cycle stuck on
    // ASCENDING because it always saw the initial, empty sort state. Same reason the body row
    // does this for its tap handler.
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

    val onSortClick: ((Boolean) -> Unit)? = if (!header.sortable) null else { isCtrl ->
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

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .headerPointerInput(
                    columnKey = header.key,
                    siblingKeys = siblingKeys,
                    drag = drag,
                    onDrop = onDrop,
                    onClick = onSortClick,
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
 * Reports where a header node was placed, washes it while it is the one being dragged, and draws
 * the line marking a landing spot on its leading or trailing edge.
 *
 * Applied to the node's outer modifier rather than to its title, so the extent it reports — and
 * the line it draws — covers the whole column including its resize handle, leaving no dead gap
 * between one column's hit area and the next.
 */
private fun Modifier.columnDropDecoration(
    columnKey: String,
    drag: ColumnDragState?,
    colors: DataTableColors,
): Modifier {
    if (drag == null) return this
    return this
        .onGloballyPositioned { coordinates ->
            // Position plus size rather than `boundsInWindow`, which clips to the scroll
            // viewport: a column half off the edge would otherwise report half its width, and
            // one scrolled fully out would collapse onto the edge alongside its neighbours.
            val left = coordinates.positionInWindow().x
            drag.reportExtent(columnKey, left, left + coordinates.size.width)
        }
        .drawWithContent {
            drawContent()
            if (drag.draggingKey == columnKey) {
                drawRect(colors.draggedColumn)
            }
            val landing = drag.drop
            if (landing?.targetKey == columnKey) {
                val width = DropIndicatorWidth.toPx()
                drawRect(
                    color = colors.columnDropIndicator,
                    topLeft = Offset(if (landing.after) size.width - width else 0f, 0f),
                    size = Size(width, size.height),
                )
            }
        }
}

/**
 * The header's press handling: one gesture that resolves into either a sort or a reordering drag,
 * decided by whether the pointer travels far enough sideways to cross the touch slop.
 *
 * They have to share a gesture. Sorting on press — which is what the header used to do — would
 * re-sort the table under the user every time they reached for a column to drag it.
 *
 * @param onClick Invoked with whether Ctrl was held when the press began, for a press that never
 *                became a drag. `null` on a column that cannot be sorted.
 */
@Composable
private fun Modifier.headerPointerInput(
    columnKey: String,
    siblingKeys: List<String>,
    drag: ColumnDragState?,
    onDrop: (ColumnDrop) -> Unit,
    onClick: ((ctrlPressed: Boolean) -> Unit)?,
): Modifier {
    if (drag == null && onClick == null) return this

    // Held across the gesture so the pointer's offset can be turned into a window coordinate:
    // the header being dragged slides out from under the pointer as the table reorders, so its
    // own local coordinates are not a fixed frame to measure against.
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentSiblings by rememberUpdatedState(siblingKeys)

    return this
        .then(if (drag == null) Modifier else Modifier.pointerHoverIcon(ColumnMoveCursor))
        .onGloballyPositioned { coordinates = it }
        .pointerInput(columnKey, drag) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                // Read at the press: by the time the gesture ends the key may have been released.
                val ctrlPressed = currentEvent.keyboardModifiers.isCtrlPressed

                if (drag != null) {
                    val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                        change.consume()
                    }
                    if (start != null) {
                        drag.start(columnKey)

                        fun track(change: PointerInputChange) {
                            val origin = coordinates ?: return
                            drag.update(origin.localToWindow(change.position).x, currentSiblings)
                        }

                        track(start)
                        val released = horizontalDrag(start.id) { change ->
                            track(change)
                            change.consume()
                        }
                        // Always ends the drag, but only a release commits it: a gesture taken
                        // over by something else is an abandoned drag, not a drop.
                        val landing = drag.finish()
                        if (released && landing != null) currentOnDrop(landing)
                        return@awaitEachGesture
                    }
                    // Slop was never crossed, so the pointer came up where it went down.
                    currentOnClick?.invoke(ctrlPressed)
                    return@awaitEachGesture
                }

                if (waitForUpOrCancellation() != null) currentOnClick?.invoke(ctrlPressed)
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
