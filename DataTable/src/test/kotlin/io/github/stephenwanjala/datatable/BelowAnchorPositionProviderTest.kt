package io.github.stephenwanjala.datatable

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class BelowAnchorPositionProviderTest {

    private val window = IntSize(800, 600)
    private val popup = IntSize(160, 200)

    private fun place(anchor: IntRect) = BelowAnchorPositionProvider.calculatePosition(
        anchor, window, LayoutDirection.Ltr, popup,
    )

    @Test
    fun `opens below the anchor when there is room`() {
        val anchor = IntRect(100, 100, 300, 140)

        assertEquals(IntOffset(100, 140), place(anchor))
    }

    @Test
    fun `flips above the anchor only when below is too short`() {
        val anchor = IntRect(100, 480, 300, 520)

        assertEquals(IntOffset(100, 280), place(anchor))
    }

    @Test
    fun `stays below when neither side fits`() {
        val tall = IntSize(160, 590)
        val anchor = IntRect(100, 300, 300, 340)

        val position = BelowAnchorPositionProvider.calculatePosition(
            anchor, window, LayoutDirection.Ltr, tall,
        )

        assertEquals(IntOffset(100, 340), position)
    }

    @Test
    fun `shifts left to stay inside the window`() {
        val anchor = IntRect(700, 100, 790, 140)

        assertEquals(IntOffset(640, 140), place(anchor))
    }
}
