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
     * Key of the row that currently has keyboard focus, or `null` when no row is focused.
     *
     * Keys are produced by `DataTable`'s `itemKey`, so focus follows its row across sorting,
     * filtering, and paging rather than staying at a fixed position.
     */
    var focusedKey: Any? by mutableStateOf(null)
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
     * Moves keyboard focus to the row with the given key, or clears focus when [key] is `null`.
     *
     * The key must be one produced by `DataTable`'s `itemKey`. Focusing a key that is not
     * currently displayed — filtered out, or on another page — is harmless: no row draws the
     * focus indicator, and the next arrow key starts again from the first row.
     *
     * This only moves focus; it does not scroll. To bring the row into view as well, pair it
     * with [animateScrollToItem] using the row's position in the list you passed to `DataTable`:
     *
     * ```
     * val position = people.indexOfFirst { it.id == personId }
     * state.focusRow(personId)
     * if (position >= 0) state.animateScrollToItem(position)
     * ```
     */
    fun focusRow(key: Any?) {
        focusedKey = key
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
