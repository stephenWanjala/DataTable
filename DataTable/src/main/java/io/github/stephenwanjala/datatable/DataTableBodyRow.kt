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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.isSecondaryPressed

/**
 * Renders a single data row for an item with optional selection and expansion controls.
 * Supports hover highlight, alternating row colors, right-click context menu, and keyboard focus.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun <T> DataTableRow(
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
    @Suppress("UNUSED_PARAMETER") horizontalScrollState: ScrollState,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    rowIndex: Int = 0,
    selectionMode: SelectionMode = SelectionMode.MULTI,
    onContextMenu: ((Offset) -> Unit)? = null,
    isFocused: Boolean = false,
    state: DataTableState? = null,
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
        modifier = Modifier
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
            )
        }

        if (showExpand) {
            SimpleIconButton(
                onClick = { onExpandChange(!expanded) },
                modifier = Modifier.size(48.dp)
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
                header = header,
                density = density,
                textStyles = textStyles,
                state = state,
            )
        }
    }
}

/**
 * Body cell for a single item/column intersection.
 */
@Composable
internal fun <T> RowScope.DataTableCell(
    item: T,
    header: DataTableHeader<T>,
    density: DataTableDensity,
    textStyles: DataTableTextStyles,
    state: DataTableState? = null,
) {
    val resolvedWidth = state?.resolvedColumnWidth(header.key, header.width) ?: header.width
    val weight = if (resolvedWidth == null) 1f else 0f
    val modifier = if (resolvedWidth != null) {
        Modifier.width(resolvedWidth)
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
            BasicText(
                text = value.toString(),
                style = textStyles.bodyCell.copy(textAlign = header.align),
                maxLines = header.maxLines,
                overflow = header.overflow,
            )
        }
    }
}
