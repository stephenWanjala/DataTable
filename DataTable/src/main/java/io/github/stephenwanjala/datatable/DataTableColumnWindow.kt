package io.github.stephenwanjala.datatable

/**
 * One entry in a row's render plan under horizontal virtualization: either a column to compose,
 * or the combined width of a run of columns that were skipped.
 *
 * A skipped run is replaced by a single spacer rather than left out, so the row still measures
 * exactly as wide as it would have — which is what keeps the body aligned with a header that is
 * always composed in full.
 */
internal sealed interface ColumnSlot {
    /** A column to compose, by its index in the scrollable header list. */
    data class Column(val index: Int) : ColumnSlot

    /** A run of skipped columns, standing in for them at exactly their combined pixel width. */
    data class Gap(val widthPx: Int) : ColumnSlot
}

/**
 * How many columns beyond each edge of the viewport are composed anyway.
 *
 * The window only changes when a column boundary crosses a viewport edge, so overscan is what
 * decides how often that happens: with none, a scroll of a single pixel can pull a column into
 * composition just as it becomes visible, and the row would be recomposed with the cell already
 * on screen. Two columns of slack means the work is done a column or two ahead of the eye.
 */
internal const val COLUMN_OVERSCAN = 2

/**
 * Works out which scrollable columns a row has to compose, given where the horizontal scroll has
 * got to.
 *
 * Positions are computed rather than measured: every column's width is known before layout — a
 * declared width, a width the user dragged it to, or the share a weighted column takes of what
 * the fixed ones leave — so the plan is right on the first frame instead of one frame late.
 *
 * @param widthsPx Width of each scrollable column in pixels, in display order.
 * @param weighted Which of those columns are weighted rather than fixed-width. A weighted column
 *                 is always composed, wherever it sits: it takes its width from `Modifier.weight`
 *                 dividing up what is left after the fixed children, and replacing it with a
 *                 spacer would hand its share to the weighted columns that remain — moving every
 *                 column after it out of line with the header.
 * @param startOffsetPx Where column zero begins inside the scrolled content, which is past the
 *                      select and expand controls when those scroll along with the columns.
 * @param scrollPx Current horizontal scroll offset.
 * @param viewportPx Width of the scrolling viewport. Zero before the first layout pass, where
 *                   every column is composed — a plan drawn against an unknown viewport would
 *                   blank the table for a frame.
 * @param overscan Columns composed beyond each viewport edge. See [COLUMN_OVERSCAN].
 */
internal fun buildColumnWindow(
    widthsPx: List<Int>,
    weighted: List<Boolean>,
    startOffsetPx: Int,
    scrollPx: Int,
    viewportPx: Int,
    overscan: Int = COLUMN_OVERSCAN,
): List<ColumnSlot> {
    val count = widthsPx.size
    if (count == 0) return emptyList()
    if (viewportPx <= 0) return List(count) { ColumnSlot.Column(it) }

    val viewStart = scrollPx
    val viewEnd = scrollPx + viewportPx

    var firstVisible = -1
    var lastVisible = -1
    var left = startOffsetPx
    for (index in 0 until count) {
        val right = left + widthsPx[index]
        // A zero-width column is on screen when its edge is, so the test is right-exclusive on
        // one side only.
        if (right > viewStart && left < viewEnd) {
            if (firstVisible == -1) firstVisible = index
            lastVisible = index
        }
        left = right
    }

    // Nothing visible — every fixed column is off screen and only the weighted ones survive.
    val windowFirst = if (firstVisible == -1) count else (firstVisible - overscan).coerceAtLeast(0)
    val windowLast = if (lastVisible == -1) -1 else (lastVisible + overscan).coerceAtMost(count - 1)

    val slots = ArrayList<ColumnSlot>(windowLast - windowFirst + 3)
    var gap = 0
    for (index in 0 until count) {
        if (index in windowFirst..windowLast || weighted[index]) {
            if (gap > 0) {
                slots += ColumnSlot.Gap(gap)
                gap = 0
            }
            slots += ColumnSlot.Column(index)
        } else {
            gap += widthsPx[index]
        }
    }
    if (gap > 0) slots += ColumnSlot.Gap(gap)
    return slots
}
