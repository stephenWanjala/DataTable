package io.github.stephenwanjala.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

/**
 * Default footer shown when pagination is disabled.
 */
@Composable
internal fun DefaultFooter(
    itemCount: Int,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.header),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            BasicText(
                text = "$itemCount items",
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.header)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val startItem = if (totalItems > 0) currentPage * itemsPerPage + 1 else 0
        val endItem = minOf((currentPage + 1) * itemsPerPage, totalItems)

        BasicText(
            text = "$startItem-$endItem of $totalItems",
            style = textStyles.footer,
        )

        // Items-per-page selector
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val enabledPrev = currentPage > 0
            val enabledNext = currentPage < totalPages - 1

            SimpleIconButton(
                onClick = { onPageChange(0) },
                enabled = enabledPrev,
            ) {
                VectorIcon(
                    KeyboardDoubleArrowLeft, "First page",
                    tint = if (enabledPrev) colors.iconTint else colors.disabledContent,
                )
            }

            SimpleIconButton(
                onClick = { onPageChange(currentPage - 1) },
                enabled = enabledPrev,
            ) {
                VectorIcon(
                    KeyboardArrowLeft, "Previous page",
                    tint = if (enabledPrev) colors.iconTint else colors.disabledContent,
                )
            }

            BasicText(
                text = "Page ${currentPage + 1} of $totalPages",
                style = textStyles.pagination,
            )

            SimpleIconButton(
                onClick = { onPageChange(currentPage + 1) },
                enabled = enabledNext,
            ) {
                VectorIcon(
                    KeyboardArrowRight, "Next page",
                    tint = if (enabledNext) colors.iconTint else colors.disabledContent,
                )
            }

            SimpleIconButton(
                onClick = { onPageChange(totalPages - 1) },
                enabled = enabledNext,
            ) {
                VectorIcon(
                    KeyboardDoubleArrowRight, "Last page",
                    tint = if (enabledNext) colors.iconTint else colors.disabledContent,
                )
            }
        }
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
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false },
            ) {
                Column(
                    modifier = Modifier
                        .background(colors.container, RoundedCornerShape(4.dp))
                        .border(1.dp, colors.divider, RoundedCornerShape(4.dp))
                        .widthIn(min = 60.dp)
                ) {
                    options.forEach { option ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(option)
                                    expanded = false
                                }
                                .background(
                                    if (option == selectedValue) colors.selectedRow else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
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
