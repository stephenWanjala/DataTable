package io.github.stephenwanjala.datatable

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Splits row content into a frozen (pinned) section and a horizontally scrollable section.
 *
 * When there are no frozen headers, falls back to a single scrollable row.
 * Frozen columns are rendered on the left and do not scroll horizontally.
 * A subtle shadow divider is drawn at the freeze boundary.
 *
 * @param frozenHeaders Headers marked with `fixed = true`.
 * @param scrollableHeaders Headers not marked as fixed.
 * @param horizontalScrollState Shared scroll state for the scrollable section.
 * @param dividerColor Color for the freeze boundary divider.
 * @param frozenContent Composable rendering frozen column cells.
 * @param scrollableContent Composable rendering scrollable column cells.
 */
@Composable
internal fun <T> FrozenRowLayout(
    frozenHeaders: List<DataTableHeader<T>>,
    scrollableHeaders: List<DataTableHeader<T>>,
    horizontalScrollState: ScrollState,
    dividerColor: Color = Color(0x33000000),
    frozenContent: @Composable RowScope.() -> Unit,
    scrollableContent: @Composable RowScope.() -> Unit,
) {
    if (frozenHeaders.isEmpty()) {
        // No frozen columns — standard scrollable row
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .height(50.dp)
        ) {
            scrollableContent()
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth().height(50.dp)) {
            // Frozen section (not scrollable)
            Row {
                frozenContent()
            }

            // Freeze boundary divider with shadow
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(dividerColor)
                    .drawBehind {
                        // Draw a subtle shadow to the right
                        for (i in 1..4) {
                            drawLine(
                                color = Color.Black.copy(alpha = 0.05f / i),
                                start = Offset(i.dp.toPx(), 0f),
                                end = Offset(i.dp.toPx(), size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
            )

            // Scrollable section
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                scrollableContent()
            }
        }
    }
}
