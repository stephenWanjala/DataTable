package io.github.stephenwanjala.datatable

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp

/**
 * State holder for [DataTable].
 *
 * Provides programmatic access to scroll position, focused row, and column widths.
 * Create via [rememberDataTableState].
 */
@Stable
class DataTableState(
    internal val lazyListState: LazyListState,
    internal val horizontalScrollState: ScrollState,
) {
    /**
     * Index of the first visible item in the vertical list.
     */
    val firstVisibleItemIndex: Int get() = lazyListState.firstVisibleItemIndex

    /**
     * The index of the row that currently has keyboard focus. -1 means no focus.
     */
    var focusedRowIndex: Int by mutableIntStateOf(-1)
        internal set

    /**
     * Column widths overridden by user resizing. Keyed by column [DataTableHeader.key].
     */
    internal val columnWidths = mutableStateMapOf<String, Dp>()

    /**
     * Scrolls the table so that the given item index is visible.
     */
    suspend fun scrollToItem(index: Int, scrollOffset: Int = 0) {
        lazyListState.scrollToItem(index, scrollOffset)
    }

    /**
     * Animates scrolling so that the given item index is visible.
     */
    suspend fun animateScrollToItem(index: Int, scrollOffset: Int = 0) {
        lazyListState.animateScrollToItem(index, scrollOffset)
    }

    /**
     * Clears all user-resized column widths, reverting to header-defined or weighted widths.
     */
    fun resetColumnWidths() {
        columnWidths.clear()
    }

    /**
     * Returns the current width for a column, considering any user resize override.
     */
    internal fun resolvedColumnWidth(key: String, headerWidth: Dp?): Dp? {
        return columnWidths[key] ?: headerWidth
    }
}

/**
 * Creates and remembers a [DataTableState].
 */
@Composable
fun rememberDataTableState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): DataTableState {
    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset
    )
    val horizontalScrollState = rememberScrollState()
    return remember(lazyListState, horizontalScrollState) {
        DataTableState(lazyListState, horizontalScrollState)
    }
}
