package io.github.stephenwanjala.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * In-place cell editing.
 *
 * The table never mutates the items it was given: it validates what the user typed and reports
 * the edit, and the caller applies it. Every test here therefore watches the reported
 * [CellEdit]s rather than the rendered text, which is also what a caller's own code will do.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableCellEditingTest {

    data class Person(val id: Int, val name: String, val quantity: Int)

    private val people = listOf(
        Person(1, "Carol", 5),
        Person(2, "Alice", 8),
    )

    /** Name is read-only; quantity is editable and rejects anything that is not a number. */
    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 200.dp),
        DataTableHeader<Person>(
            key = "quantity",
            title = "Quantity",
            value = { it.quantity },
            width = 200.dp,
            editable = true,
            validateEdit = { _, text ->
                if (text.toIntOrNull() == null) "Quantity must be a whole number" else null
            },
        ),
    )

    private fun ComposeUiTest.press(key: Key) = onRoot().performKeyInput { pressKey(key) }

    private fun ComposeUiTest.pressWithShift(key: Key) =
        onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(key) } }

    /** Replaces the open editor's contents, the way selecting-all and typing would. */
    private fun ComposeUiTest.type(text: String) =
        onNode(hasSetTextAction()).performTextReplacement(text)

    private class Harness {
        lateinit var state: DataTableState
        val edits = mutableListOf<CellEdit<Person>>()
        val rowClicks = mutableListOf<String>()
        val doubleClicks = mutableListOf<String>()
    }

    private fun ComposeUiTest.table(
        columns: List<DataTableHeader<Person>> = headers,
        onRowDoubleClick: Boolean = false,
    ): Harness {
        val harness = Harness()
        setContent {
            harness.state = rememberDataTableState()
            Box(Modifier.size(700.dp, 400.dp)) {
                DataTable(
                    items = people,
                    headers = columns,
                    itemKey = { it.id },
                    state = harness.state,
                    onCellEdit = { harness.edits += it },
                    onRowClick = { harness.rowClicks += it.name },
                    onRowDoubleClick =
                        if (onRowDoubleClick) ({ harness.doubleClicks += it.name }) else null,
                )
            }
        }
        return harness
    }

    @Test
    fun `Enter opens the editor on an editable column`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Enter)

        assertEquals(CellPosition(1, "quantity"), harness.state.editingCell)
    }

    @Test
    fun `Enter still fires the row click on a read-only column`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Carol").performClick()
        val before = harness.rowClicks.size
        press(Key.Enter)

        assertFalse(harness.state.isEditing, "a read-only column has no editor to open")
        assertEquals(before + 1, harness.rowClicks.size)
        assertEquals("Carol", harness.rowClicks.last())
    }

    @Test
    fun `F2 opens the editor`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("8").performClick()
        press(Key.F2)

        assertEquals(CellPosition(2, "quantity"), harness.state.editingCell)
    }

    @Test
    fun `Escape closes the editor without reporting anything`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Enter)
        type("99")
        press(Key.Escape)

        assertFalse(harness.state.isEditing)
        assertTrue(harness.edits.isEmpty(), "a cancelled edit must not be reported")
        // Focus stays where it was, so the next arrow key carries on from the same cell.
        assertEquals(CellPosition(1, "quantity"), harness.state.focusedCell)
    }

    @Test
    fun `Enter commits and moves down the column`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Enter)
        type("12")
        press(Key.Enter)

        assertEquals(1, harness.edits.size)
        val edit = harness.edits.single()
        assertEquals(1, edit.rowKey)
        assertEquals("quantity", edit.columnKey)
        assertEquals("5", edit.oldText)
        assertEquals("12", edit.newText)
        assertEquals(Person(1, "Carol", 5), edit.item)

        assertFalse(harness.state.isEditing)
        // Down a column is the data-entry move: the same column, the next row.
        assertEquals(CellPosition(2, "quantity"), harness.state.focusedCell)
    }

    @Test
    fun `Tab commits and moves to the next cell`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Enter)
        type("7")
        press(Key.Tab)

        assertEquals("7", harness.edits.single().newText)
        // Last column of the row, so Tab wraps onto the next one.
        assertEquals(CellPosition(2, "name"), harness.state.focusedCell)
    }

    @Test
    fun `Shift+Tab commits and moves back`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("8").performClick()
        press(Key.Enter)
        type("3")
        pressWithShift(Key.Tab)

        assertEquals("3", harness.edits.single().newText)
        assertEquals(CellPosition(2, "name"), harness.state.focusedCell)
    }

    @Test
    fun `committing an unchanged value reports nothing`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Enter)
        press(Key.Enter)

        assertTrue(
            harness.edits.isEmpty(),
            "opening and closing a cell is not an edit, and must not mark a row dirty",
        )
        assertFalse(harness.state.isEditing)
    }

    @Test
    fun `validation rejects a value and holds the editor open`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Enter)
        type("not a number")
        press(Key.Enter)

        assertTrue(harness.state.isEditing, "a rejected value must not close the editor")
        assertEquals("Quantity must be a whole number", harness.state.editError)
        assertTrue(harness.edits.isEmpty(), "an invalid value must never reach onCellEdit")

        // Correcting it commits normally and clears the error.
        type("6")
        press(Key.Enter)
        assertFalse(harness.state.isEditing)
        assertNull(harness.state.editError)
        assertEquals("6", harness.edits.single().newText)
    }

    @Test
    fun `typing over a cell opens the editor seeded with what was typed`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Nine)

        assertEquals(CellPosition(1, "quantity"), harness.state.editingCell)

        press(Key.Enter)
        assertEquals("9", harness.edits.single().newText)
    }

    @Test
    fun `typing over a read-only cell does nothing`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Carol").performClick()
        press(Key.Nine)

        assertFalse(harness.state.isEditing)
    }

    @Test
    fun `Space stays bound to selection rather than starting an edit`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Spacebar)

        assertFalse(harness.state.isEditing)
    }

    @Test
    fun `a function key does not start an edit`() = runComposeUiTest {
        // F5 reports AWT's CHAR_UNDEFINED, which would otherwise read as a printable character
        // and open an editor holding U+FFFF.
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.F5)

        assertFalse(harness.state.isEditing)
    }

    @Test
    fun `double-clicking an editable cell opens its editor`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performMouseInput { doubleClick() }

        assertEquals(CellPosition(1, "quantity"), harness.state.editingCell)
    }

    @Test
    fun `double-clicking a read-only cell falls through to onRowDoubleClick`() = runComposeUiTest {
        val harness = table(onRowDoubleClick = true)

        onNodeWithText("Carol").performMouseInput { doubleClick() }

        assertFalse(harness.state.isEditing)
        assertEquals(listOf("Carol"), harness.doubleClicks)
    }

    @Test
    fun `a caller's own double-click handler keeps the gesture`() = runComposeUiTest {
        val harness = table(onRowDoubleClick = true)

        onNodeWithText("5").performMouseInput { doubleClick() }

        assertFalse(
            harness.state.isEditing,
            "the caller claimed double-click, so it must not also open an editor",
        )
        assertEquals(listOf("Carol"), harness.doubleClicks)

        // Editing is still reachable by keyboard, which is the point of not fighting over it.
        press(Key.F2)
        assertEquals(CellPosition(1, "quantity"), harness.state.editingCell)
    }

    @Test
    fun `startEditing opens an editor without any key press`() = runComposeUiTest {
        val harness = table()

        harness.state.startEditing(2, "quantity")
        waitForIdle()

        assertEquals(CellPosition(2, "quantity"), harness.state.editingCell)

        type("4")
        press(Key.Enter)
        assertEquals("4", harness.edits.single().newText)
        assertEquals(2, harness.edits.single().rowKey)
    }

    @Test
    fun `editValue seeds the editor instead of the displayed text`() = runComposeUiTest {
        // A column that formats for display has to hand the editor the raw value, or every edit
        // starts by making the user delete a currency symbol.
        val harness = table(
            columns = listOf(
                headers[0],
                headers[1].copy(
                    value = { "KES ${it.quantity}.00" },
                    editValue = { it.quantity.toString() },
                ),
            ),
        )

        onNodeWithText("KES 5.00").performClick()
        press(Key.Enter)
        press(Key.Enter)

        // Committing untouched compares against the raw value, so this is not a change.
        assertTrue(harness.edits.isEmpty())

        press(Key.MoveHome)
        onNodeWithText("KES 8.00").performClick()
        press(Key.Enter)
        type("9")
        press(Key.Enter)

        val edit = harness.edits.single()
        assertEquals("8", edit.oldText)
        assertEquals("9", edit.newText)
    }

    @Test
    fun `clicking another cell closes an open editor`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Enter)
        assertNotNull(harness.state.editingCell)

        onNodeWithText("Alice").performClick()

        assertFalse(harness.state.isEditing)
        assertTrue(harness.edits.isEmpty(), "clicking away abandons the edit, it does not commit")
        assertEquals(CellPosition(2, "name"), harness.state.focusedCell)
    }

    @Test
    fun `keyboard navigation resumes once an editor closes`() = runComposeUiTest {
        // The editor holds focus while it is open, and it is removed from the composition the
        // moment it commits. Unless focus is handed back to the table, the next key press goes
        // nowhere and the grid is dead until the user clicks it again.
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Enter)
        type("11")
        press(Key.Enter)
        assertEquals(CellPosition(2, "quantity"), harness.state.focusedCell)

        press(Key.DirectionLeft)
        assertEquals(CellPosition(2, "name"), harness.state.focusedCell)

        press(Key.DirectionUp)
        assertEquals(CellPosition(1, "name"), harness.state.focusedCell)
    }

    @Test
    fun `keyboard navigation resumes after a cancelled edit`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("5").performClick()
        press(Key.Enter)
        press(Key.Escape)

        press(Key.DirectionLeft)
        assertEquals(CellPosition(1, "name"), harness.state.focusedCell)
    }

    @Test
    fun `arrow keys move the caret inside an open editor, not the cell cursor`() =
        runComposeUiTest {
            val harness = table()

            onNodeWithText("5").performClick()
            press(Key.Enter)
            press(Key.DirectionDown)
            press(Key.DirectionRight)

            // The editor owns the keyboard while it is open.
            assertEquals(CellPosition(1, "quantity"), harness.state.editingCell)
        }
}
