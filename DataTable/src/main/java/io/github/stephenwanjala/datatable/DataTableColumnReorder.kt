package io.github.stephenwanjala.datatable

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Where a dragged header will land: beside the header keyed [targetKey], on its trailing side
 * when [after] and on its leading side otherwise.
 */
internal data class ColumnDrop(val targetKey: String, val after: Boolean)

/**
 * Tracks one header-reordering drag.
 *
 * One instance per rendered header row, which is what keeps a drag inside its own section: the
 * frozen headers and the scrolling ones are separate rows, so a column cannot be dragged across
 * the freeze boundary into a section it would have to change its `fixed` flag to join.
 *
 * Hit-testing is done against window coordinates rather than the pointer's offset within the
 * header it started on, because the header being dragged moves under the pointer as the table
 * reorders around it.
 */
@Stable
internal class ColumnDragState {
    /** The header currently being dragged, or `null` when no drag is in progress. */
    var draggingKey: String? by mutableStateOf(null)
        private set

    /** Where the drag would land if released now, or `null` when it would not move anything. */
    var drop: ColumnDrop? by mutableStateOf(null)
        private set

    /**
     * Horizontal extent of every header node in this row, in window coordinates.
     *
     * A plain map, not snapshot state: it is written during layout and read only from the drag
     * handler, so making it observable would invalidate the header on every scroll for nothing.
     */
    private val extents = mutableMapOf<String, ClosedFloatingPointRange<Float>>()

    /** Records where a header node was placed. Called from its `onGloballyPositioned`. */
    fun reportExtent(key: String, left: Float, right: Float) {
        extents[key] = left..right
    }

    fun start(key: String) {
        draggingKey = key
        drop = null
    }

    /** Recomputes the landing spot for a pointer now at [windowX], among [siblingKeys]. */
    fun update(windowX: Float, siblingKeys: List<String>) {
        val dragging = draggingKey ?: return
        drop = resolveDrop(windowX, dragging, siblingKeys)
    }

    /** Ends the drag, returning where it landed — `null` when it did not move anything. */
    fun finish(): ColumnDrop? {
        val landing = drop
        draggingKey = null
        drop = null
        return landing
    }

    private fun resolveDrop(
        windowX: Float,
        dragging: String,
        siblingKeys: List<String>,
    ): ColumnDrop? {
        val from = siblingKeys.indexOf(dragging)
        if (from < 0) return null

        val overIndex = siblingKeys.indexOfFirst { key -> extents[key]?.contains(windowX) == true }
            .takeIf { it >= 0 }
            ?: nearestEnd(windowX, siblingKeys)
            ?: return null

        // Which half of the column under the pointer decides the side, so the indicator settles
        // rather than flicking back and forth as soon as a wide neighbour is grazed.
        val extent = extents[siblingKeys[overIndex]] ?: return null
        val after = windowX > (extent.start + extent.endInclusive) / 2f
        val insertion = if (after) overIndex + 1 else overIndex

        // Landing on either side of where it already sits is not a move.
        if (insertion == from || insertion == from + 1) return null
        return ColumnDrop(siblingKeys[overIndex], after)
    }

    /**
     * The first or last sibling when the pointer has been dragged off the end of the row, or
     * `null` when it is in a gap between siblings and belongs to neither.
     */
    private fun nearestEnd(windowX: Float, siblingKeys: List<String>): Int? {
        val first = siblingKeys.firstOrNull()?.let { extents[it] } ?: return null
        val last = siblingKeys.lastOrNull()?.let { extents[it] } ?: return null
        return when {
            windowX < first.start -> 0
            windowX > last.endInclusive -> siblingKeys.lastIndex
            else -> null
        }
    }
}
