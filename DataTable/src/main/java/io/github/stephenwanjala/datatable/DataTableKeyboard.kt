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
 */
internal fun Modifier.dataTableKeyboardNavigation(
    state: DataTableState,
    itemCount: Int,
    scope: CoroutineScope,
    onRowClick: ((Int) -> Unit)? = null,
    onToggleSelection: ((Int) -> Unit)? = null,
): Modifier = this.onPreviewKeyEvent { keyEvent ->
    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    if (itemCount == 0) return@onPreviewKeyEvent false

    when (keyEvent.key) {
        Key.DirectionDown -> {
            state.focusedRowIndex = (state.focusedRowIndex + 1).coerceAtMost(itemCount - 1)
            scope.launch { state.lazyListState.animateScrollToItem(state.focusedRowIndex) }
            true
        }

        Key.DirectionUp -> {
            state.focusedRowIndex = (state.focusedRowIndex - 1).coerceAtLeast(0)
            scope.launch { state.lazyListState.animateScrollToItem(state.focusedRowIndex) }
            true
        }

        Key.Enter -> {
            if (state.focusedRowIndex in 0 until itemCount) {
                onRowClick?.invoke(state.focusedRowIndex)
            }
            true
        }

        Key.Spacebar -> {
            if (state.focusedRowIndex in 0 until itemCount) {
                onToggleSelection?.invoke(state.focusedRowIndex)
            }
            true
        }

        Key.MoveHome -> {
            state.focusedRowIndex = 0
            scope.launch { state.lazyListState.animateScrollToItem(0) }
            true
        }

        Key.MoveEnd -> {
            state.focusedRowIndex = itemCount - 1
            scope.launch { state.lazyListState.animateScrollToItem(itemCount - 1) }
            true
        }

        else -> false
    }
}
