package io.github.stephenwanjala.datatable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Colors used throughout the [DataTable].
 *
 * All colors have sensible defaults that work without any theming framework.
 * Pass a custom instance to [DataTable] to match your application's palette.
 */
@Immutable
data class DataTableColors(
    val container: Color = Color(0xFFFAFAFA),
    val header: Color = Color(0xFFF0F0F0),
    val divider: Color = Color(0xFFDDDDDD),
    val selectedRow: Color = Color(0x4D1976D2),
    val expandedRow: Color = Color(0x80F0F0F0),
    val onSurface: Color = Color(0xFF1C1C1C),
    val onSurfaceSecondary: Color = Color(0xFF757575),
    val checkboxChecked: Color = Color(0xFF1976D2),
    val checkboxUnchecked: Color = Color(0xFF757575),
    val checkboxCheckmark: Color = Color.White,
    val iconTint: Color = Color(0xFF616161),
    val disabledContent: Color = Color(0xFFBDBDBD),
    val rowAlternate: Color = Color.Transparent,
    val hoveredRow: Color = Color(0x1A000000),
    val focusedRowBorder: Color = Color(0xFF1976D2),
)

/**
 * Text styles used throughout the [DataTable].
 *
 * Defaults are plain styles at typical sizes - no Material typography required.
 */
@Immutable
data class DataTableTextStyles(
    val headerCell: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1C1C1C),
    ),
    val bodyCell: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
    val footer: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
    val loading: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
    val noData: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0x991C1C1C),
    ),
    val pagination: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF1C1C1C),
    ),
)

/**
 * Factory functions for [DataTableColors] and [DataTableTextStyles].
 */
object DataTableDefaults {
    @Composable
    fun colors(
        container: Color = Color(0xFFFAFAFA),
        header: Color = Color(0xFFF0F0F0),
        divider: Color = Color(0xFFDDDDDD),
        selectedRow: Color = Color(0x4D1976D2),
        expandedRow: Color = Color(0x80F0F0F0),
        onSurface: Color = Color(0xFF1C1C1C),
        onSurfaceSecondary: Color = Color(0xFF757575),
        checkboxChecked: Color = Color(0xFF1976D2),
        checkboxUnchecked: Color = Color(0xFF757575),
        checkboxCheckmark: Color = Color.White,
        iconTint: Color = Color(0xFF616161),
        disabledContent: Color = Color(0xFFBDBDBD),
        rowAlternate: Color = Color.Transparent,
        hoveredRow: Color = Color(0x1A000000),
        focusedRowBorder: Color = Color(0xFF1976D2),
    ): DataTableColors = remember(
        container, header, divider, selectedRow, expandedRow,
        onSurface, onSurfaceSecondary, checkboxChecked, checkboxUnchecked,
        checkboxCheckmark, iconTint, disabledContent, rowAlternate,
        hoveredRow, focusedRowBorder
    ) {
        DataTableColors(
            container = container,
            header = header,
            divider = divider,
            selectedRow = selectedRow,
            expandedRow = expandedRow,
            onSurface = onSurface,
            onSurfaceSecondary = onSurfaceSecondary,
            checkboxChecked = checkboxChecked,
            checkboxUnchecked = checkboxUnchecked,
            checkboxCheckmark = checkboxCheckmark,
            iconTint = iconTint,
            disabledContent = disabledContent,
            rowAlternate = rowAlternate,
            hoveredRow = hoveredRow,
            focusedRowBorder = focusedRowBorder,
        )
    }

    @Composable
    fun textStyles(
        headerCell: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1C)),
        bodyCell: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
        footer: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
        loading: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
        noData: TextStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Color(0x991C1C1C)),
        pagination: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color(0xFF1C1C1C)),
    ): DataTableTextStyles = remember(headerCell, bodyCell, footer, loading, noData, pagination) {
        DataTableTextStyles(
            headerCell = headerCell,
            bodyCell = bodyCell,
            footer = footer,
            loading = loading,
            noData = noData,
            pagination = pagination,
        )
    }
}
