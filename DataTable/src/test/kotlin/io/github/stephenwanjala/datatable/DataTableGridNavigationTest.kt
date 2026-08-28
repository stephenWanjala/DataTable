package io.github.stephenwanjala.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cell-level focus and grid keyboard navigation.
 *
 * Cell navigation is opt-in: without it the table keeps walking whole rows, which is what every
 * table written against the previous release expects. These tests pin both halves of that —
 * that it stays off until asked for, and that once on, focus moves in two dimensions and is
 * still held as keys rather than as coordinates.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableGridNavigationTest {

    data class Person(val id: Int, val name: String, val role: String, val age: Int)

    // Unsorted, so a re-sort visibly reorders them.
    private val people = listOf(
        Person(1, "Carol", "Buyer", 41),
        Person(2, "Alice", "Clerk", 30),
        Person(3, "Bob", "Auditor", 35),
    )

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
        DataTableHeader<Person>(key = "role", title = "Role", value = { it.role }, width = 150.dp),
        DataTableHeader<Person>(key = "age", title = "Age", value = { it.age }, width = 150.dp),
    )

    private fun ComposeUiTest.press(key: Key) = onRoot().performKeyInput { pressKey(key) }

    private fun ComposeUiTest.pressWithCtrl(key: Key) =
        onRoot().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(key) } }

    private fun ComposeUiTest.pressWithShift(key: Key) =
        onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(key) } }

    /** Clicks a cell, which both hands the table focus and focuses that cell. */
    private fun ComposeUiTest.clickCell(text: String) = onNodeWithText(text).performClick()

    private fun ComposeUiTest.table(
        cellNavigation: Boolean = true,
        columns: List<DataTableHeader<Person>> = headers,
    ): DataTableState {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(700.dp, 400.dp)) {
                DataTable(
                    items = people,
                    headers = columns,
                    itemKey = { it.id },
                    state = state,
                    cellNavigation = cellNavigation,
                )
            }
        }
        return state
    }

    @Test
    fun `cell navigation stays off until asked for`() = runComposeUiTest {
        val state = table(cellNavigation = false)

        clickCell("Carol")
        press(Key.DirectionRight)

        // Right is not a key row navigation knows, so nothing moved at all.
        assertNull(state.focusedKey)

        // Down still walks rows, exactly as before — but no column is ever focused, so a cell
        // cursor never appears and clicking a cell keeps meaning "click the row".
        press(Key.DirectionDown)
        assertEquals(1, state.focusedKey)
        assertNull(state.focusedColumnKey)
        assertNull(state.focusedCell)
    }

    @Test
    fun `an editable column turns cell navigation on by itself`() = runComposeUiTest {
        val state = table(
            cellNavigation = false,
            columns = headers.map { if (it.key == "age") it.copy(editable = true) else it },
        )

        clickCell("Carol")
        press(Key.DirectionRight)

        assertEquals(CellPosition(1, "role"), state.focusedCell)
    }

    @Test
    fun `clicking a cell focuses it`() = runComposeUiTest {
        val state = table()

        clickCell("Auditor")

        assertEquals(CellPosition(3, "role"), state.focusedCell)
    }

    @Test
    fun `arrow right and left walk columns`() = runComposeUiTest {
        val state = table()

        clickCell("Carol")
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        assertEquals(CellPosition(1, "age"), state.focusedCell)

        press(Key.DirectionLeft)
        assertEquals(CellPosition(1, "role"), state.focusedCell)
    }

    @Test
    fun `arrow right stops at the last column`() = runComposeUiTest {
        val state = table()

        clickCell("Carol")
        repeat(5) { press(Key.DirectionRight) }

        assertEquals(CellPosition(1, "age"), state.focusedCell)
    }

    @Test
    fun `arrow down keeps the column`() = runComposeUiTest {
        val state = table()

        clickCell("Carol")
        press(Key.DirectionRight)
        press(Key.DirectionDown)

        // Walking down a column is the whole point — focus must not snap back to column one.
        assertEquals(CellPosition(2, "role"), state.focusedCell)
    }

    @Test
    fun `Home and End move within the row, with Ctrl to the ends of the table`() =
        runComposeUiTest {
            val state = table()

            clickCell("Carol")
            press(Key.DirectionRight)

            press(Key.MoveEnd)
            assertEquals(CellPosition(1, "age"), state.focusedCell)

            press(Key.MoveHome)
            assertEquals(CellPosition(1, "name"), state.focusedCell)

            pressWithCtrl(Key.MoveEnd)
            assertEquals(CellPosition(3, "age"), state.focusedCell)

            pressWithCtrl(Key.MoveHome)
            assertEquals(CellPosition(1, "name"), state.focusedCell)
        }

    @Test
    fun `Tab walks cells and wraps onto the next row`() = runComposeUiTest {
        val state = table()

        clickCell("Carol")
        press(Key.Tab)
        assertEquals(CellPosition(1, "role"), state.focusedCell)

        press(Key.Tab)
        press(Key.Tab)

        assertEquals(CellPosition(2, "name"), state.focusedCell)
    }

    @Test
    fun `Shift+Tab walks back across the row boundary`() = runComposeUiTest {
        val state = table()

        clickCell("Clerk")   // Alice's role, the middle of the second row
        pressWithShift(Key.Tab)
        assertEquals(CellPosition(2, "name"), state.focusedCell)

        pressWithShift(Key.Tab)
        assertEquals(CellPosition(1, "age"), state.focusedCell)
    }

    @Test
    fun `Tab at the last cell leaves the table rather than trapping focus`() = runComposeUiTest {
        val state = table()

        clickCell("Carol")
        pressWithCtrl(Key.MoveEnd)
        assertEquals(CellPosition(3, "age"), state.focusedCell)

        // Unhandled, so the focus system gets it and moves on. Nothing in the grid changes.
        press(Key.Tab)
        assertEquals(CellPosition(3, "age"), state.focusedCell)
    }

    @Test
    fun `focus stays on its cell across a re-sort`() = runComposeUiTest {
        val state = table()

        clickCell("Buyer")   // Carol's role, sitting first while unsorted
        assertEquals(CellPosition(1, "role"), state.focusedCell)

        onNodeWithText("Name").performClick()   // sort ascending: Alice, Bob, Carol

        // Carol is now last. Coordinates would have drifted; keys do not.
        assertEquals(CellPosition(1, "role"), state.focusedCell)
    }

    @Test
    fun `moving onto an off-screen column scrolls it into view`() = runComposeUiTest {
        lateinit var state: DataTableState
        val wide = (1..8).map { index ->
            DataTableHeader<Person>(
                key = "c$index",
                title = "Column $index",
                value = { it.name },
                width = 200.dp,
            )
        }
        setContent {
            state = rememberDataTableState()
            // Deliberately too narrow for eight 200.dp columns.
            Box(Modifier.size(400.dp, 300.dp)) {
                DataTable(
                    items = people,
                    headers = wide,
                    itemKey = { it.id },
                    state = state,
                    cellNavigation = true,
                )
            }
        }

        onNodeWithText("Column 1").performClick()   // hand the table focus
        state.focusCell(1, "c1")
        repeat(7) { press(Key.DirectionRight) }
        waitForIdle()

        assertEquals("c8", state.focusedColumnKey)
        assertTrue(
            state.horizontalScrollState.value > 0,
            "the eighth column cannot be visible without scrolling, " +
                "but the table never scrolled (offset ${state.horizontalScrollState.value})",
        )
    }

    @Test
    fun `focusCell and its cursor survive a column being hidden`() = runComposeUiTest {
        val state = table()

        state.focusCell(2, "role")
        assertEquals(CellPosition(2, "role"), state.focusedCell)

        // A key that is no longer displayed simply draws nothing; navigation restarts cleanly.
        state.focusCell(2, "nonexistent")
        clickCell("Carol")
        assertEquals(CellPosition(1, "name"), state.focusedCell)
    }
}
