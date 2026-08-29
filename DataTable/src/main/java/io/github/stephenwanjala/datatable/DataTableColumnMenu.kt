package io.github.stephenwanjala.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

/**
 * The table menu button: a control at the trailing edge of the header listing every column with a
 * checkbox, for showing and hiding them.
 *
 * @param headers The header tree as *declared*, so a column the user has hidden is still listed
 *                and can be brought back. Columns whose header says `visible = false` are absent:
 *                those are the caller's decision, not the user's.
 */
@Composable
internal fun <T> ColumnMenuButton(
    headers: List<DataTableHeader<T>>,
    state: DataTableState,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val columns = remember(headers) { flattenHeaders(headers) }

    Box(modifier) {
        SimpleIconButton(
            onClick = { expanded = !expanded },
            size = 28.dp,
            hoverColor = colors.hoveredRow,
            focusable = false,
        ) {
            VectorIcon(
                imageVector = Add,
                contentDescription = "Show or hide columns",
                modifier = Modifier.size(15.dp),
                tint = colors.iconTint,
            )
        }

        if (expanded) {
            Popup(
                popupPositionProvider = remember { BelowTrailingEdge(gap = 4) },
                onDismissRequest = { expanded = false },
            ) {
                Column(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(6.dp))
                        .background(colors.container, RoundedCornerShape(6.dp))
                        .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                        .width(IntrinsicSize.Max)
                        // A wide table has more columns than fit on screen.
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    columns.forEach { column ->
                        ColumnMenuItem(
                            title = column.title.ifEmpty { column.key },
                            checked = !state.isColumnHidden(column.key),
                            colors = colors,
                            textStyles = textStyles,
                            onToggle = { checked -> state.setColumnHidden(column.key, !checked) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnMenuItem(
    title: String,
    checked: Boolean,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    onToggle: (Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 160.dp)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            // Toggleable rather than clickable: the row *is* the checkbox, and saying so gives
            // it a state a screen reader — and a test — can read.
            .toggleable(value = checked, onValueChange = onToggle)
            .background(if (hovered) colors.hoveredRow else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SimpleCheckbox(
            checked = checked,
            // The row carries the click; a checkbox that also took one would fire twice.
            onCheckedChange = {},
            modifier = Modifier.size(16.dp),
            colors = colors,
            focusable = false,
        )
        BasicText(text = title, style = textStyles.bodyCell, maxLines = 1)
    }
}

/** Drops the menu below the button, pulled left so it stays on screen at the table's edge. */
private class BelowTrailingEdge(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.right - popupContentSize.width)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}
