package io.github.stephenwanjala.datatable

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Adds keyboard navigation to the DataTable container.
 *
 * - Arrow Up/Down: move focused row
 * - Enter: trigger row click on focused row
 * - Space: toggle selection on focused row
 * - Home: focus first row
 * - End: focus last row
 *
 * Focus is stored on [DataTableState] as a row key, not a position, so it stays on the same
 * row when the table is re-sorted. Navigation resolves that key back to a position against
 * [rowKeys], which must be in display order.
 *
 * @param rowKeys Keys of the currently displayed rows, in display order.
 * @param scrollIndices `LazyColumn` item index for each entry in [rowKeys]. These differ when
 *                      grouping inserts header or summary items between rows.
 * @param onRowClick Invoked with the focused row's position in [rowKeys].
 * @param onToggleSelection Invoked with the focused row's position in [rowKeys].
 */
internal fun Modifier.dataTableKeyboardNavigation(
    state: DataTableState,
    rowKeys: List<Any>,
    scrollIndices: List<Int>,
    scope: CoroutineScope,
    onRowClick: ((Int) -> Unit)? = null,
    onToggleSelection: ((Int) -> Unit)? = null,
): Modifier = this.onPreviewKeyEvent { keyEvent ->
    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    if (rowKeys.isEmpty()) return@onPreviewKeyEvent false

    // -1 when nothing is focused, or when the focused row is no longer displayed
    // (filtered out, or on another page).
    val current = rowKeys.indexOf(state.focusedKey)

    fun moveFocusTo(position: Int): Boolean {
        val target = position.coerceIn(0, rowKeys.lastIndex)
        state.focusedKey = rowKeys[target]
        scope.launch {
            state.lazyListState.animateScrollToItem(scrollIndices.getOrElse(target) { target })
        }
        return true
    }

    when (keyEvent.key) {
        Key.DirectionDown -> moveFocusTo(if (current < 0) 0 else current + 1)

        Key.DirectionUp -> moveFocusTo(if (current < 0) 0 else current - 1)

        Key.MoveHome -> moveFocusTo(0)

        Key.MoveEnd -> moveFocusTo(rowKeys.lastIndex)

        Key.Enter -> {
            if (current >= 0) onRowClick?.invoke(current)
            true
        }

        Key.Spacebar -> {
            if (current >= 0) onToggleSelection?.invoke(current)
            true
        }

        else -> false
    }
}
