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
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cell range selection.
 *
 * A range is an anchor and a cursor, and everything between them. Like the cursor itself it is
 * held as keys rather than coordinates, so these tests check both what gets highlighted and what
 * survives the table being re-sorted underneath it.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableRangeSelectionTest {

    data class Person(val id: Int, val name: String, val role: String, val city: String)

    private val people = listOf(
        Person(1, "Carol", "Buyer", "Nairobi"),
        Person(2, "Alice", "Clerk", "Mombasa"),
        Person(3, "Bob", "Auditor", "Kisumu"),
    )

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
        DataTableHeader<Person>(key = "role", title = "Role", value = { it.role }, width = 150.dp),
        DataTableHeader<Person>(key = "city", title = "City", value = { it.city }, width = 150.dp),
    )

    private fun ComposeUiTest.press(key: Key) = onRoot().performKeyInput { pressKey(key) }

    private fun ComposeUiTest.pressWithShift(key: Key) =
        onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(key) } }

    private fun ComposeUiTest.pressWithCtrl(key: Key) =
        onRoot().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(key) } }

    private fun ComposeUiTest.table(): DataTableState {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(700.dp, 400.dp)) {
                DataTable(
                    items = people,
                    headers = headers,
                    itemKey = { it.id },
                    state = state,
                    cellNavigation = true,
                )
            }
        }
        return state
    }

    /** The cells currently highlighted, as `rowKey/columnKey`, in display order. */
    private fun DataTableState.highlighted(): List<String> =
        people.map { it.id }.filter { it in rangeRowKeys }.flatMap { rowKey ->
            headers.map { it.key }.filter { it in rangeColumnKeys }.map { "$rowKey/$it" }
        }

    @Test
    fun `nothing is selected until a range is extended`() = runComposeUiTest {
        val state = table()

        onNodeWithText("Carol").performClick()

        assertNull(state.selectedRange)
        assertFalse(state.isCellInRange(1, "name"))
    }

    @Test
    fun `Shift+Right extends across columns`() = runComposeUiTest {
        val state = table()

        onNodeWithText("Carol").performClick()
        pressWithShift(Key.DirectionRight)

        assertEquals(CellPosition(1, "name"), state.selectedRange?.anchor)
        assertEquals(CellPosition(1, "role"), state.selectedRange?.focus)
        assertEquals(listOf("1/name", "1/role"), state.highlighted())
    }

    @Test
    fun `Shift+Down extends across rows, and again to a block`() = runComposeUiTest {
        val state = table()

        onNodeWithText("Carol").performClick()
        pressWithShift(Key.DirectionDown)
        assertEquals(listOf("1/name", "2/name"), state.highlighted())

        pressWithShift(Key.DirectionRight)
        assertEquals(
            listOf("1/name", "1/role", "2/name", "2/role"),
            state.highlighted(),
        )
    }

    @Test
    fun `a range extended backwards covers the same block`() = runComposeUiTest {
        val state = table()

        onNodeWithText("Kisumu").performClick()   // Bob's city, the bottom-right cell
        pressWithShift(Key.DirectionUp)
        pressWithShift(Key.DirectionLeft)

        // The anchor is now the bottom-right corner; the rectangle is normalised regardless.
        assertEquals(CellPosition(3, "city"), state.selectedRange?.anchor)
        assertEquals(CellPosition(2, "role"), state.selectedRange?.focus)
        assertEquals(
            listOf("2/role", "2/city", "3/role", "3/city"),
            state.highlighted(),
        )
    }

    @Test
    fun `moving without Shift collapses the range`() = runComposeUiTest {
        val state = table()

        onNodeWithText("Carol").performClick()
        pressWithShift(Key.DirectionRight)
        assertTrue(state.isCellInRange(1, "name"))

        press(Key.DirectionRight)

        assertNull(state.selectedRange)
        assertFalse(state.isCellInRange(1, "name"))
        assertEquals(CellPosition(1, "city"), state.focusedCell)
    }

    @Test
    fun `Shift+End selects to the end of the row, Ctrl+Shift+End to the last cell`() =
        runComposeUiTest {
            val state = table()

            onNodeWithText("Carol").performClick()
            pressWithShift(Key.MoveEnd)
            assertEquals(listOf("1/name", "1/role", "1/city"), state.highlighted())

            onNodeWithText("Carol").performClick()
            onRoot().performKeyInput {
                withKeyDown(Key.CtrlLeft) { withKeyDown(Key.ShiftLeft) { pressKey(Key.MoveEnd) } }
            }
            assertEquals(9, state.highlighted().size)
        }

    @Test
    fun `Ctrl+A selects every cell`() = runComposeUiTest {
        val state = table()

        onNodeWithText("Carol").performClick()
        pressWithCtrl(Key.A)

        assertEquals(CellPosition(1, "name"), state.selectedRange?.anchor)
        assertEquals(CellPosition(3, "city"), state.selectedRange?.focus)
        assertEquals(9, state.highlighted().size)
    }

    @Test
    fun `Shift+click extends from the focused cell`() = runComposeUiTest {
        val state = table()

        onNodeWithText("Carol").performClick()
        onNodeWithText("Clerk").performMouseInput {
            // The press handler reads the modifier off the event, so hold it across the click.
            enter(); press(); release(); exit()
        }
        // Without a modifier held the second click just re-anchors.
        assertNull(state.selectedRange)

        onNodeWithText("Carol").performClick()
        onRoot().performKeyInput {
            keyDown(Key.ShiftLeft)
        }
        onNodeWithText("Clerk").performClick()
        onRoot().performKeyInput {
            keyUp(Key.ShiftLeft)
        }

        assertEquals(CellPosition(1, "name"), state.selectedRange?.anchor)
        assertEquals(CellPosition(2, "role"), state.selectedRange?.focus)
        assertEquals(listOf("1/name", "1/role", "2/name", "2/role"), state.highlighted())
    }

    @Test
    fun `a range holds its cells across a re-sort`() = runComposeUiTest {
        val state = table()

        onNodeWithText("Carol").performClick()
        pressWithShift(Key.DirectionDown)
        assertEquals(listOf("1/name", "2/name"), state.highlighted())

        onNodeWithText("Name").performClick()   // sort ascending: Alice, Bob, Carol

        // Carol and Alice are no longer adjacent, but they are still the cells that were chosen.
        assertEquals(listOf("1/name", "2/name"), state.highlighted())
    }

    @Test
    fun `selectRange and clearRange work without any key press`() = runComposeUiTest {
        val state = table()

        state.selectRange(CellPosition(1, "role"), CellPosition(3, "city"))

        assertEquals(CellPosition(3, "city"), state.focusedCell)
        assertEquals(
            listOf("1/role", "1/city", "2/role", "2/city", "3/role", "3/city"),
            state.highlighted(),
        )

        state.clearRange()
        assertNull(state.selectedRange)
        // Clearing the block leaves the cursor where it was.
        assertEquals(CellPosition(3, "city"), state.focusedCell)
    }

    @Test
    fun `a range whose corner is no longer displayed is dropped rather than guessed at`() =
        runComposeUiTest {
            val state = table()

            state.selectRange(CellPosition(1, "name"), CellPosition(2, "role"))
            assertEquals(4, state.highlighted().size)

            // Extending from a corner that is not on display leaves nothing to normalise.
            state.selectRange(CellPosition(99, "name"), CellPosition(2, "role"))
            assertTrue(state.highlighted().isEmpty())
        }
}
