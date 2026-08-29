package io.github.stephenwanjala.datatable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import kotlinx.coroutines.launch

/**
 * Renders a single data row for an item with optional selection and expansion controls.
 * Supports hover highlight, alternating row colors, right-click context menu, and keyboard focus.
 *
 * @param rowKey The row's key, as produced by `DataTable`'s `itemKey`. Cells need it to say which
 *               of them is focused or being edited.
 * @param gridNavigation Whether cell-level focus and editing are active.
 * @param editOnDoubleClick Whether double-clicking an editable cell opens its editor.
 * @param onCellEdit Where committed edits are reported.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun <T> DataTableRow(
    item: T,
    rowKey: Any,
    headers: List<DataTableHeader<T>>,
    modifier: Modifier = Modifier,
    showSelect: Boolean,
    selected: Boolean,
    onSelectChange: (Boolean) -> Unit,
    showExpand: Boolean,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    density: DataTableDensity,
    onClick: (() -> Unit)?,
    onDoubleClick: (() -> Unit)?,
    @Suppress("UNUSED_PARAMETER") horizontalScrollState: ScrollState,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    rowIndex: Int = 0,
    selectionMode: SelectionMode = SelectionMode.MULTI,
    onContextMenu: ((Offset) -> Unit)? = null,
    isFocused: Boolean = false,
    state: DataTableState? = null,
    gridNavigation: Boolean = false,
    editOnDoubleClick: Boolean = false,
    onCellEdit: ((CellEdit<T>) -> Unit)? = null,
) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor = when {
        selected -> colors.selectedRow
        isHovered -> colors.hoveredRow
        rowIndex % 2 == 1 && colors.rowAlternate != Color.Transparent -> colors.rowAlternate
        else -> Color.Transparent
    }

    val focusBorderColor = colors.focusedRowBorder

    // Use rememberUpdatedState so the gesture detector captures the latest values
    // without restarting (which would break double-tap detection).
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelectChange by rememberUpdatedState(onSelectChange)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)
    val currentOnContextMenu by rememberUpdatedState(onContextMenu)

    Row(
        modifier = modifier
            .background(backgroundColor)
            .then(
                if (isFocused) {
                    Modifier.drawBehind {
                        drawRect(
                            color = focusBorderColor,
                            topLeft = Offset.Zero,
                            size = Size(2.dp.toPx(), size.height)
                        )
                    }
                } else Modifier
            )
            // Hover + right-click: use Initial pass so it doesn't block detectTapGestures
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                            PointerEventType.Press -> {
                                val ctxMenu = currentOnContextMenu
                                if (ctxMenu != null) {
                                    val change = event.changes.firstOrNull()
                                    if (change != null && event.buttons.isSecondaryPressed) {
                                        change.consume()
                                        ctxMenu(change.position)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Primary click/double-click — key on selectionMode only, not `selected`
            .pointerInput(selectionMode) {
                detectTapGestures(
                    onTap = {
                        when (selectionMode) {
                            SelectionMode.SINGLE,
                            SelectionMode.MULTI -> currentOnSelectChange(!currentSelected)
                            SelectionMode.NONE -> {}
                        }
                        currentOnClick?.invoke()
                    },
                    // Only provide onDoubleTap when a handler exists — otherwise
                    // detectTapGestures delays every single-tap waiting for a second tap.
                    // Double-tap fires onDoubleClick without toggling selection.
                    onDoubleTap = if (currentOnDoubleClick != null) {
                        { currentOnDoubleClick?.invoke() }
                    } else null,
                )
            }
            .padding(vertical = density.verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showSelect && selectionMode != SelectionMode.NONE) {
            SimpleCheckbox(
                checked = selected,
                onCheckedChange = { checked ->
                    when (selectionMode) {
                        SelectionMode.SINGLE -> onSelectChange(checked)
                        SelectionMode.MULTI -> onSelectChange(checked)
                        SelectionMode.NONE -> {}
                    }
                },
                modifier = Modifier.padding(horizontal = density.horizontalPadding),
                colors = colors,
                // One tab stop per row would make Tab unusable; arrow keys reach rows instead.
                focusable = false,
            )
        }

        if (showExpand) {
            SimpleIconButton(
                onClick = { onExpandChange(!expanded) },
                modifier = Modifier.size(48.dp),
                focusable = false,
            ) {
                VectorIcon(
                    imageVector = if (expanded) KeyboardArrowUp else ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = colors.iconTint,
                )
            }
        }

        headers.forEach { header ->
            DataTableCell(
                item = item,
                rowKey = rowKey,
                header = header,
                density = density,
                colors = colors,
                textStyles = textStyles,
                state = state,
                gridNavigation = gridNavigation,
                editOnDoubleClick = editOnDoubleClick,
                onCellEdit = onCellEdit,
            )
        }
    }
}

/**
 * Body cell for a single item/column intersection.
 *
 * Once [gridNavigation] is on the cell also draws the cell cursor, takes cell focus on a press,
 * and hosts the editor when it is the cell being edited.
 */
@Composable
internal fun <T> RowScope.DataTableCell(
    item: T,
    rowKey: Any,
    header: DataTableHeader<T>,
    density: DataTableDensity,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    state: DataTableState? = null,
    gridNavigation: Boolean = false,
    editOnDoubleClick: Boolean = false,
    onCellEdit: ((CellEdit<T>) -> Unit)? = null,
) {
    val resolvedWidth = state?.resolvedColumnWidth(header.key, header.width) ?: header.width
    val weight = if (resolvedWidth == null) 1f else 0f
    val sizeModifier = if (resolvedWidth != null) {
        Modifier.width(resolvedWidth)
    } else {
        Modifier.weight(weight)
    }

    val cellNavigation = gridNavigation && state != null
    val position = if (cellNavigation) CellPosition(rowKey, header.key) else null
    val isFocusedCell = position != null && state!!.focusedCell == position
    val isEditingCell = isFocusedCell && state!!.isEditing

    // The cursor's own cell is left unwashed, the way a spreadsheet leaves its active cell.
    val isInRange = cellNavigation && !isFocusedCell &&
        state!!.isCellInRange(rowKey, header.key)

    val cursorModifier = if (isFocusedCell || isInRange) {
        val borderColor =
            if (isEditingCell && state!!.editError != null) colors.invalidCellBorder
            else colors.focusedCellBorder
        val rangeColor = colors.selectedCell
        // The row's vertical padding sits outside every cell, so both the range wash and the
        // cursor are drawn overflowing their bounds by that much to span the whole row. Nothing
        // clips them. A row made tall by a wrapping neighbour is the one case where they come up
        // short of the divider.
        Modifier.drawBehind {
            val overflow = density.verticalPadding.toPx()
            if (isInRange) {
                drawRect(
                    color = rangeColor,
                    topLeft = Offset(0f, -overflow),
                    size = Size(size.width, size.height + overflow * 2),
                )
            }
            if (isFocusedCell) {
                val stroke = 2.dp.toPx()
                drawRect(
                    color = borderColor,
                    topLeft = Offset(stroke / 2, -overflow + stroke / 2),
                    size = Size(size.width - stroke, size.height + overflow * 2 - stroke),
                    style = Stroke(width = stroke),
                )
            }
        }
    } else Modifier

    // Initial pass and no consumption, so this records which cell was hit without taking the
    // press away from the row's tap detector — row clicks and selection keep working.
    val doubleClickTimeout = LocalViewConfiguration.current.doubleTapTimeoutMillis
    val pressModifier = if (cellNavigation) {
        Modifier.pointerInput(rowKey, header.key, editOnDoubleClick) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type != PointerEventType.Press) continue
                    val change = event.changes.firstOrNull() ?: continue
                    // A press anywhere but inside the open editor closes it.
                    if (state!!.editingCell != position) state.cancelEditing()
                    if (event.keyboardModifiers.isShiftPressed) {
                        // Extending a selection is not a click on a cell: it must not re-anchor,
                        // and two shift-clicks in a row are not a double-click into the editor.
                        state.extendRangeTo(rowKey, header.key)
                    } else {
                        val doubleClicked = state.registerCellPress(
                            position!!, change.uptimeMillis, doubleClickTimeout,
                        )
                        state.focusCell(rowKey, header.key)
                        if (doubleClicked && editOnDoubleClick && header.editable) {
                            state.startEditing(rowKey, header.key)
                        }
                    }
                }
            }
        }
    } else Modifier

    Box(
        modifier = sizeModifier
            .then(cursorModifier)
            .then(pressModifier)
            .padding(horizontal = density.horizontalPadding),
        contentAlignment = when (header.align) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        when {
            isEditingCell -> CellEditorHost(
                item = item,
                rowKey = rowKey,
                header = header,
                state = state!!,
                colors = colors,
                textStyles = textStyles,
                onCellEdit = onCellEdit,
            )

            header.cellContent != null -> header.cellContent.invoke(item)

            else -> {
                BasicText(
                    text = header.displayText(item),
                    style = textStyles.bodyCell.copy(textAlign = header.align),
                    maxLines = header.maxLines,
                    overflow = header.overflow,
                )
            }
        }
    }
}

/**
 * Runs one open editor: seeds it, validates what comes back, reports the edit, and moves focus.
 *
 * The commit path is the one place that decides whether an edit is accepted, so the built-in
 * text field and a column's own [DataTableHeader.editorContent] cannot drift apart on validation
 * or on what counts as a change.
 */
@Composable
private fun <T> CellEditorHost(
    item: T,
    rowKey: Any,
    header: DataTableHeader<T>,
    state: DataTableState,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    onCellEdit: ((CellEdit<T>) -> Unit)?,
) {
    val scope = rememberCoroutineScope()

    // What the cell holds now, which is both the editor's starting text and the `oldText` an
    // edit is reported against — even when the edit began by typing over the cell.
    val originalText = header.editValue?.invoke(item)
        ?: header.value?.invoke(item)?.toString()
        ?: ""

    val seed = state.editSeedText
    val currentOnCellEdit by rememberUpdatedState(onCellEdit)

    fun commit(text: String, move: EditMove): Boolean {
        val error = header.validateEdit?.invoke(item, text)
        if (error != null) {
            state.editError = error
            return false
        }
        state.endEdit()
        if (text != originalText) {
            currentOnCellEdit?.invoke(CellEdit(item, rowKey, header.key, originalText, text))
        }
        // The editor is going away; the container is the only thing left that can hold focus,
        // and without this the next key press would go nowhere.
        state.containerFocusRequester.requestFocus()
        when (move) {
            EditMove.STAY -> Unit
            EditMove.DOWN -> state.focusPosition(state.focusedRowPosition() + 1, null)
            EditMove.NEXT -> state.advanceCellFocus(backwards = false)
            EditMove.PREVIOUS -> state.advanceCellFocus(backwards = true)
        }
        if (move != EditMove.STAY) scope.launch { state.revealFocusedCell() }
        return true
    }

    fun cancel() {
        state.endEdit()
        state.containerFocusRequester.requestFocus()
    }

    val editorContent = header.editorContent
    if (editorContent != null) {
        val controller = remember(rowKey, header.key, seed, state.editError) {
            CellEditController(
                initialText = seed ?: originalText,
                error = state.editError,
                onCommitRequest = { commit(it, EditMove.STAY) },
                onCancelRequest = { cancel() },
            )
        }
        editorContent(item, controller)
    } else {
        DefaultCellEditor(
            initialText = seed ?: originalText,
            selectAll = seed == null,
            textStyle = textStyles.cellEditor,
            colors = colors,
            onCommit = ::commit,
            onCancel = ::cancel,
        )
    }
}
