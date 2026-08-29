package io.github.stephenwanjala.datatable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The plan behind horizontal virtualization.
 *
 * [buildColumnWindow] is deliberately free of Compose: which columns a row composes is pure
 * arithmetic over widths and a scroll offset, and pinning it down here is cheaper and far more
 * precise than inferring it from a rendered table.
 *
 * The invariant every case shares is that a window loses nothing: the columns it composes plus
 * the gaps standing in for the ones it skips always come to the same total width as composing
 * everything. That is what keeps a virtualized body aligned with a header that is not.
 */
class DataTableColumnWindowTest {

    private val ten = List(10) { 100 }
    private val allFixed = List(10) { false }

    private fun List<ColumnSlot>.composed(): List<Int> =
        filterIsInstance<ColumnSlot.Column>().map { it.index }

    private fun List<ColumnSlot>.totalWidth(widths: List<Int>): Int = sumOf { slot ->
        when (slot) {
            is ColumnSlot.Gap -> slot.widthPx
            is ColumnSlot.Column -> widths[slot.index]
        }
    }

    @Test
    fun `a window covers the viewport plus its overscan`() {
        val window = buildColumnWindow(
            widthsPx = ten,
            weighted = allFixed,
            startOffsetPx = 0,
            scrollPx = 0,
            viewportPx = 400,
            overscan = 2,
        )

        // 0..3 are on screen; two more each side, clamped at the start.
        assertEquals(listOf(0, 1, 2, 3, 4, 5), window.composed())
    }

    @Test
    fun `the columns it skips are replaced at exactly their width`() {
        val window = buildColumnWindow(ten, allFixed, 0, 0, 400, overscan = 2)

        assertEquals(listOf(ColumnSlot.Gap(400)), window.filterIsInstance<ColumnSlot.Gap>())
        assertEquals(ten.sum(), window.totalWidth(ten), "a window must not lose width")
    }

    @Test
    fun `scrolling moves the window`() {
        val window = buildColumnWindow(ten, allFixed, 0, 600, 400, overscan = 1)

        // 600..1000 is columns 6..9, one of slack each side and nothing past the end.
        assertEquals(listOf(5, 6, 7, 8, 9), window.composed())
        assertEquals(ten.sum(), window.totalWidth(ten))
        assertEquals(
            listOf(ColumnSlot.Gap(500), ColumnSlot.Column(5)),
            window.take(2),
            "the skipped run has to come first, or every column after it shifts left",
        )
    }

    @Test
    fun `columns of unequal width are placed by their own widths`() {
        val widths = listOf(50, 300, 40, 40, 40, 500)
        val window = buildColumnWindow(
            widthsPx = widths,
            weighted = List(widths.size) { false },
            startOffsetPx = 0,
            scrollPx = 360,
            viewportPx = 100,
            overscan = 0,
        )

        // Edges at 0, 50, 350, 390, 430, 470, 970: 360..460 covers columns 2, 3 and 4.
        assertEquals(listOf(2, 3, 4), window.composed())
        assertEquals(widths.sum(), window.totalWidth(widths))
    }

    @Test
    fun `the leading controls push the first column along`() {
        // Select and expand controls scroll with the columns when nothing is frozen, so column
        // zero does not start at zero.
        val window = buildColumnWindow(ten, allFixed, startOffsetPx = 250, scrollPx = 250,
            viewportPx = 100, overscan = 0)

        assertEquals(listOf(0), window.composed())
    }

    @Test
    fun `a weighted column is composed wherever it sits`() {
        // Weighted columns take what `Modifier.weight` divides up after the fixed children.
        // Culling one would hand its share to the others and shift everything after it.
        val weighted = List(10) { it == 8 }
        val window = buildColumnWindow(ten, weighted, 0, 0, 200, overscan = 0)

        assertEquals(listOf(0, 1, 8), window.composed())
        assertEquals(
            listOf(ColumnSlot.Gap(600), ColumnSlot.Gap(100)),
            window.filterIsInstance<ColumnSlot.Gap>(),
            "the run either side of a weighted column has to be two gaps, not one",
        )
        assertEquals(ten.sum(), window.totalWidth(ten))
    }

    @Test
    fun `nothing is culled before the first layout pass`() {
        // The viewport is unknown until the table has been measured once. Planning against a
        // zero-width viewport would blank every row for a frame.
        val window = buildColumnWindow(ten, allFixed, 0, 0, viewportPx = 0)

        assertEquals(ten.indices.toList(), window.composed())
    }

    @Test
    fun `a table with no scrollable columns plans nothing`() {
        assertEquals(emptyList(), buildColumnWindow(emptyList(), emptyList(), 0, 0, 400))
    }

    @Test
    fun `every scroll offset keeps the total width`() {
        val widths = listOf(120, 80, 200, 60, 300, 90, 150, 45, 500, 75)
        val weighted = List(widths.size) { it == 3 }
        for (scroll in 0..widths.sum() step 17) {
            val window = buildColumnWindow(widths, weighted, 30, scroll, 400)
            assertEquals(
                widths.sum(),
                window.totalWidth(widths),
                "width went missing at scroll $scroll",
            )
            assertTrue(
                ColumnSlot.Column(3) in window,
                "the weighted column was culled at scroll $scroll",
            )
        }
    }
}
