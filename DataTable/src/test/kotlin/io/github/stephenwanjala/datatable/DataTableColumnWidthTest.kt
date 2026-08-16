package io.github.stephenwanjala.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Column sizing.
 *
 * A column with `width = null` is documented as taking a share of the leftover space. That only
 * works if the row it lives in has a bounded width — everything scrollable sits inside
 * `horizontalScroll`, which measures its content with an unbounded width, and `Modifier.weight`
 * resolves to zero against that. These pin down that the table supplies a definite width.
 *
 * Widths are read from where the *next* column's text starts: a cell's own width is not directly
 * observable, but the offset of the column after it gives the same information. Cell text sits
 * `density.horizontalPadding` (16dp by default) in from the cell's leading edge, and the test
 * density is 1dp = 1px.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableColumnWidthTest {

    data class Row(val id: Int, val a: String, val b: String, val c: String)

    private val rows = listOf(Row(1, "AAA", "BBB", "CCC"))

    private val padding = 16

    private fun SemanticsNodeInteraction.x() = fetchSemanticsNode().positionInRoot.x.toInt()
    private fun SemanticsNodeInteraction.width() = fetchSemanticsNode().size.width

    @Composable
    private fun Table(
        headers: List<DataTableHeader<Row>>,
        viewport: Int = 1000,
        showSelect: Boolean = false,
    ) {
        Box(Modifier.size(viewport.dp, 400.dp)) {
            DataTable(
                items = rows,
                headers = headers,
                itemKey = { it.id },
                showSelect = showSelect,
            )
        }
    }

    @Test
    fun `a weighted column takes the space the fixed ones leave`() = runComposeUiTest {
        setContent {
            Table(
                listOf(
                    DataTableHeader(key = "a", title = "A", value = { it.a }),                 // weighted
                    DataTableHeader(key = "b", title = "B", value = { it.b }, width = 200.dp),
                ),
            )
        }

        // 1000 viewport - 200 fixed = 800 for the weighted column, so B starts at 800.
        assertEquals(800 + padding, onNodeWithText("BBB").x())
    }

    @Test
    fun `a weighted column is never zero-width`() = runComposeUiTest {
        // The regression this whole file exists for: it used to measure 0 x 95.
        setContent {
            setContent@ Table(listOf(DataTableHeader(key = "a", title = "A", value = { it.a })))
        }

        assertTrue(onNodeWithText("AAA").width() > 0, "a weighted column must not collapse")
    }

    @Test
    fun `equal weights divide the leftover evenly`() = runComposeUiTest {
        setContent {
            Table(
                listOf(
                    DataTableHeader(key = "a", title = "A", value = { it.a }),
                    DataTableHeader(key = "b", title = "B", value = { it.b }),
                    DataTableHeader(key = "c", title = "C", value = { it.c }, width = 100.dp),
                ),
            )
        }

        // 1000 - 100 fixed = 900, split 450/450.
        assertEquals(450 + padding, onNodeWithText("BBB").x())
        assertEquals(900 + padding, onNodeWithText("CCC").x())
    }

    @Test
    fun `fixed columns are unaffected`() = runComposeUiTest {
        setContent {
            Table(
                listOf(
                    DataTableHeader(key = "a", title = "A", value = { it.a }, width = 200.dp),
                    DataTableHeader(key = "b", title = "B", value = { it.b }, width = 300.dp),
                ),
            )
        }

        assertEquals(padding, onNodeWithText("AAA").x())
        assertEquals(200 + padding, onNodeWithText("BBB").x())
    }

    @Test
    fun `a weighted column fills the space beside a frozen section`() = runComposeUiTest {
        setContent {
            Table(
                listOf(
                    DataTableHeader(key = "a", title = "A", value = { it.a }, width = 200.dp, fixed = true),
                    DataTableHeader(key = "b", title = "B", value = { it.b }),
                ),
            )
        }

        // The frozen section is 200 wide, then a 1dp boundary divider.
        assertEquals(200 + 1 + padding, onNodeWithText("BBB").x())
    }

    @Test
    fun `the selection column is accounted for`() = runComposeUiTest {
        setContent {
            Table(
                listOf(
                    DataTableHeader(key = "a", title = "A", value = { it.a }),
                    DataTableHeader(key = "b", title = "B", value = { it.b }, width = 200.dp),
                ),
                showSelect = true,
            )
        }

        // A 20dp checkbox with 16dp either side leads the row, so the weighted column gets
        // 1000 - 52 - 200 = 748 and B lands after it. Ignoring the checkbox would overflow.
        val leading = 20 + padding * 2
        assertEquals(leading + 748 + padding, onNodeWithText("BBB").x())
    }

    @Test
    fun `a weighted column keeps a minimum when fixed columns overflow`() = runComposeUiTest {
        setContent {
            Table(
                listOf(
                    DataTableHeader(key = "a", title = "A", value = { it.a }, width = 400.dp),
                    DataTableHeader(key = "b", title = "B", value = { it.b }),
                ),
                viewport = 300,
            )
        }

        // 400 already exceeds the 300 viewport, so the weighted column falls back to
        // minColumnWidth (40dp) rather than collapsing to nothing.
        assertEquals(400 + padding, onNodeWithText("BBB").x())
        assertTrue(onNodeWithText("BBB").width() > 0, "it should still be rendered")
    }
}
