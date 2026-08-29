package io.github.stephenwanjala.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Default footer shown when pagination is disabled.
 */
@Composable
internal fun DefaultFooter(
    itemCount: Int,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
) {
    Column {
        TableDivider(colors.divider)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.header)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            BasicText(
                text = if (itemCount == 1) "1 item" else "$itemCount items",
                style = textStyles.footer,
            )
        }
    }
}

/**
 * Pagination footer with first/prev/next/last controls, range indicator,
 * and optional items-per-page selector.
 */
@Composable
internal fun PaginationFooter(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    itemsPerPage: Int,
    onPageChange: (Int) -> Unit,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    itemsPerPageOptions: List<Int> = emptyList(),
    onItemsPerPageChange: ((Int) -> Unit)? = null,
) {
    Column {
        TableDivider(colors.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.header)
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val startItem = if (totalItems > 0) currentPage * itemsPerPage + 1 else 0
            val endItem = minOf((currentPage + 1) * itemsPerPage, totalItems)

            BasicText(
                text = if (totalItems > 0) "$startItem–$endItem of $totalItems" else "No rows",
                style = textStyles.footer,
            )

            // Both controls live in one trailing group. With SpaceBetween across three loose
            // children, the middle one drifted and the whole footer re-laid-out whenever the
            // rows-per-page selector was absent.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (itemsPerPageOptions.isNotEmpty() && onItemsPerPageChange != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BasicText(
                            text = "Rows per page:",
                            style = textStyles.pagination,
                        )
                        SimpleDropdown(
                            selectedValue = itemsPerPage,
                            options = itemsPerPageOptions,
                            onValueChange = onItemsPerPageChange,
                            colors = colors,
                            textStyles = textStyles,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val enabledPrev = currentPage > 0
                    val enabledNext = currentPage < totalPages - 1

                    PageButton(KeyboardDoubleArrowLeft, "First page", enabledPrev, colors) {
                        onPageChange(0)
                    }
                    PageButton(KeyboardArrowLeft, "Previous page", enabledPrev, colors) {
                        onPageChange(currentPage - 1)
                    }

                    // Fixed minimum width: the label grows as the page number gains digits,
                    // which would otherwise shove the next/last buttons sideways mid-paging.
                    Box(
                        modifier = Modifier
                            .widthIn(min = 104.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Page ${currentPage + 1} of $totalPages",
                            style = textStyles.pagination,
                        )
                    }

                    PageButton(KeyboardArrowRight, "Next page", enabledNext, colors) {
                        onPageChange(currentPage + 1)
                    }
                    PageButton(KeyboardDoubleArrowRight, "Last page", enabledNext, colors) {
                        onPageChange(totalPages - 1)
                    }
                }
            }
        }
    }
}

/**
 * One page-navigation button. Compact enough to sit in a footer, unlike the 48dp default.
 */
@Composable
private fun PageButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    colors: DataTableColors,
    onClick: () -> Unit,
) {
    SimpleIconButton(
        onClick = onClick,
        enabled = enabled,
        size = 30.dp,
        hoverColor = colors.hoveredRow,
    ) {
        VectorIcon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) colors.iconTint else colors.disabledContent,
        )
    }
}

/**
 * A Foundation-only dropdown selector using [Popup].
 */
@Composable
internal fun SimpleDropdown(
    selectedValue: Int,
    options: List<Int>,
    onValueChange: (Int) -> Unit,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .background(colors.container, RoundedCornerShape(4.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText(text = "$selectedValue", style = textStyles.pagination)
            VectorIcon(
                ArrowDropDown, "Expand",
                modifier = Modifier.size(16.dp),
                tint = colors.iconTint,
            )
        }

        if (expanded) {
            Popup(
                popupPositionProvider = remember { DropUpPositionProvider(gap = 4) },
                onDismissRequest = { expanded = false },
            ) {
                Column(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(6.dp))
                        .background(colors.container, RoundedCornerShape(6.dp))
                        .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                        .width(IntrinsicSize.Max)
                        .padding(vertical = 4.dp)
                ) {
                    options.forEach { option ->
                        val selected = option == selectedValue
                        val optionInteraction = remember { MutableInteractionSource() }
                        val hovered by optionInteraction.collectIsHoveredAsState()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minWidth = 64.dp)
                                .hoverable(optionInteraction)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable {
                                    onValueChange(option)
                                    expanded = false
                                }
                                .background(
                                    when {
                                        selected -> colors.selectedRow
                                        hovered -> colors.hoveredRow
                                        else -> Color.Transparent
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            BasicText(text = "$option", style = textStyles.pagination)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Places a popup above its anchor, falling back to below when there is not enough room.
 *
 * The rows-per-page menu lives in the footer, at the very bottom of the window, so the default
 * downward placement ran it straight off the screen edge.
 */
private class DropUpPositionProvider(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left
            .coerceAtMost(windowSize.width - popupContentSize.width)
            .coerceAtLeast(0)
        val above = anchorBounds.top - popupContentSize.height - gap
        val y = if (above >= 0) above else anchorBounds.bottom + gap
        return IntOffset(x, y)
    }
}

/**
 * Default loading UI shown when `loading` is true and no `loadingContent` is provided.
 */
@Composable
internal fun DefaultLoadingContent(textStyles: DataTableTextStyles) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var dots by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                while (true) {
                    dots = ".".repeat((dots.length % 3) + 1)
                    kotlinx.coroutines.delay(500)
                }
            }
            BasicText(
                text = "Loading$dots",
                style = textStyles.loading,
            )
        }
    }
}

/**
 * Default empty-state UI shown when there are no items.
 */
@Composable
internal fun DefaultNoDataContent(textStyles: DataTableTextStyles) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = "No data available",
            style = textStyles.noData,
        )
    }
}

/**
 * Default empty-state UI shown when a filter, rather than an empty data set, is what left the
 * table with no rows. Worth telling apart: one is a table with nothing in it, the other is a
 * table whose contents are one keystroke away.
 */
@Composable
internal fun DefaultNoResultsContent(textStyles: DataTableTextStyles) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = "No rows match the filter",
            style = textStyles.noData,
        )
    }
}
