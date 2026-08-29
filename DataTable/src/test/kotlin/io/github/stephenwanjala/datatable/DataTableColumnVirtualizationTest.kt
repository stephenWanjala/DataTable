package io.github.stephenwanjala.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Horizontal virtualization, seen from the rendered table.
 *
 * Rows are virtualized vertically by `LazyColumn`; this is the other axis. A wide grid used to
 * compose every leaf column for every visible row, so forty columns meant forty cells a row
 * whatever the viewport showed.
 *
 * The two things worth holding onto are that off-screen cells really are gone — otherwise none
 * of this saves anything — and that the columns still on screen have not moved a pixel, since
 * the header is composed in full either way and a body drifting under it would be worse than
 * the cost this avoids.
 *
 * The test density is 1dp = 1px, and cell text sits `density.horizontalPadding` (16dp) in from
 * the leading edge of its column.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableColumnVirtualizationTest {

    data class Row(val id: Int, val cells: List<String>)

    private val columnCount = 20
    private val columnWidth = 100.dp

    private val rows = List(3) { row ->
        Row(row, List(columnCount) { column -> "r${row}c$column" })
    }

    private val headers = List(columnCount) { column ->
        DataTableHeader<Row>(
            key = "c$column",
            title = "C$column",
            value = { it.cells[column] },
            width = columnWidth,
        )
    }

    private fun SemanticsNodeInteraction.x() = fetchSemanticsNode().positionInRoot.x.toInt()

    /** Column 10, holding text long enough to wrap a 100dp column onto several lines. */
    private fun wrappingText(row: Int) =
        "r${row}c10 with rather more text than fits in a hundred points of width"

    private val wrapping = headers.mapIndexed { index, header ->
        if (index == 10) header.copy(value = { wrappingText(it.id) }) else header
    }

    @Test
    fun `columns past the viewport are not composed`() = runComposeUiTest {
        val state = rememberedState()
        setContent {
            // 400dp of viewport onto 2000dp of columns.
            Box(Modifier.size(400.dp, 300.dp)) {
                DataTable(
                    items = rows,
                    headers = headers,
                    itemKey = { it.id },
                    state = state,
                )
            }
        }

        onNodeWithText("r0c0").assertIsDisplayed()
        // Columns 0..3 are on screen and COLUMN_OVERSCAN keeps 4 and 5; 6 onwards are gone.
        onNodeWithText("r0c5").assertExists()
        onNodeWithText("r0c8").assertDoesNotExist()
        onNodeWithText("r0c19").assertDoesNotExist()

        // Every row is culled, not just the first.
        onNodeWithText("r2c19").assertDoesNotExist()
    }

    @Test
    fun `a table whose rows size to content composes every column`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                // No row height, so a row can grow to fit its tallest cell — and a cell that
                // might be the tallest cannot be left out of the measurement.
                DataTable(items = rows, headers = headers, itemKey = { it.id }, rowHeight = null)
            }
        }

        onNodeWithText("r0c19").assertExists()
    }

    @Test
    fun `the header keeps every column whatever the body drops`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                DataTable(
                    items = rows,
                    headers = headers,
                    itemKey = { it.id },
                )
            }
        }

        // One row of header cells is not what makes a wide grid slow, and culling it would take
        // the filter fields' focus with it.
        onNodeWithText("C19").assertExists()
    }

    @Test
    fun `the columns still on screen have not moved`() {
        var virtualized = 0
        var plain = 0

        runComposeUiTest {
            setContent {
                Box(Modifier.size(400.dp, 300.dp)) {
                    DataTable(
                        items = rows,
                        headers = headers,
                        itemKey = { it.id },
                        )
                }
            }
            virtualized = onNodeWithText("r0c3").x()
        }

        runComposeUiTest {
            setContent {
                Box(Modifier.size(400.dp, 300.dp)) {
                    DataTable(items = rows, headers = headers, itemKey = { it.id }, rowHeight = null)
                }
            }
            plain = onNodeWithText("r0c3").x()
        }

        assertEquals(plain, virtualized, "virtualization moved a column that was on screen")
        assertEquals(300 + 16, plain, "three 100dp columns then 16dp of cell padding")
    }

    @Test
    fun `the body stays under its header after scrolling sideways`() = runComposeUiTest {
        val state = rememberedState()
        setContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                DataTable(
                    items = rows,
                    headers = headers,
                    itemKey = { it.id },
                    state = state,
                )
            }
        }

        runBlocking { state.horizontalScrollState.scrollTo(1000) }
        waitForIdle()

        // 1000..1400 is columns 10..13, with overscan either side.
        onNodeWithText("r0c10").assertIsDisplayed()
        onNodeWithText("r0c0").assertDoesNotExist()

        // The gap standing in for columns 0..7 has to measure exactly what they did, or the
        // cell lands somewhere other than under its own header.
        assertEquals(
            onNodeWithText("C10").x(),
            onNodeWithText("r0c10").x(),
            "a cell drifted out from under its header column",
        )
    }

    @Test
    fun `a fixed row height holds steady while a wrapping column is culled`() = runComposeUiTest {
        // The reason `rowHeight` and column culling are one decision rather than two.
        //
        // A row that sizes to its tallest cell grows for a column of wrapping text even while
        // that column is off screen. Cull it and the rows snap short; scroll it back and they
        // grow again, shoving everything below down the page. A fixed height is immune, which
        // is what makes culling safe to do without being asked.
        val state = rememberedState()

        setContent {
            Box(Modifier.size(400.dp, 600.dp)) {
                DataTable(items = rows, headers = wrapping, itemKey = { it.id }, state = state)
            }
        }

        // Column 5 stays composed at both offsets; only the wrapping column 10 comes and goes.
        runBlocking { state.horizontalScrollState.scrollTo(300) }
        waitForIdle()
        val near = onNodeWithText("r2c5").fetchSemanticsNode().positionInRoot.y
        onNodeWithText(wrappingText(2)).assertDoesNotExist()   // culled, so the test is not vacuous

        runBlocking { state.horizontalScrollState.scrollTo(700) }
        waitForIdle()
        val far = onNodeWithText("r2c5").fetchSemanticsNode().positionInRoot.y
        onNodeWithText(wrappingText(2)).assertExists()         // and back again

        assertEquals(near, far, "a fixed row height moved when a wrapping column was culled")
    }

    @Test
    fun `rows that size to content are never culled, so they cannot jump`() = runComposeUiTest {
        // `rowHeight = null` is the case culling has to stand down for. It does so by composing
        // every column at every offset — which is the whole cost, and the reason it is not the
        // default.
        val state = rememberedState()

        setContent {
            Box(Modifier.size(400.dp, 600.dp)) {
                DataTable(
                    items = rows,
                    headers = wrapping,
                    itemKey = { it.id },
                    state = state,
                    rowHeight = null,
                )
            }
        }

        runBlocking { state.horizontalScrollState.scrollTo(300) }
        waitForIdle()
        val near = onNodeWithText("r2c5").fetchSemanticsNode().positionInRoot.y
        // Composed while far off screen: that is what keeps the height honest, and what costs.
        onNodeWithText(wrappingText(2)).assertExists()

        runBlocking { state.horizontalScrollState.scrollTo(700) }
        waitForIdle()
        val far = onNodeWithText("r2c5").fetchSemanticsNode().positionInRoot.y

        assertEquals(near, far, "a content-sized row moved, so something was culled after all")
    }

    @Test
    fun `a weighted column survives being scrolled past`() = runComposeUiTest {
        val state = rememberedState()
        // One weighted column early on: it takes what `Modifier.weight` leaves after the fixed
        // ones, so replacing it with a spacer would hand its share to nobody and pull every
        // column after it out of line.
        val mixed = headers.mapIndexed { index, header ->
            if (index == 1) header.copy(width = null) else header
        }

        setContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                DataTable(
                    items = rows,
                    headers = mixed,
                    itemKey = { it.id },
                    state = state,
                )
            }
        }

        val restingX = onNodeWithText("r0c5").x()

        runBlocking { state.horizontalScrollState.scrollTo(600) }
        waitForIdle()

        onNodeWithText("r0c1").assertExists()
        assertEquals(
            onNodeWithText("C5").x(),
            onNodeWithText("r0c5").x(),
            "the weighted column was culled and the fixed ones after it shifted",
        )
        assertEquals(
            restingX - 600,
            onNodeWithText("r0c5").x(),
            "a column moved by something other than the scroll",
        )
    }
}

/**
 * A state built outside composition, so a test can drive its scroll after `setContent` returns.
 */
private fun rememberedState(): DataTableState = DataTableState(
    androidx.compose.foundation.lazy.LazyListState(),
    androidx.compose.foundation.ScrollState(0),
)
