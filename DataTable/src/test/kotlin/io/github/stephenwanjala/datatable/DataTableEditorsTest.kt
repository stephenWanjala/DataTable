package io.github.stephenwanjala.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DataTableEditorsTest {

    data class Person(val id: Int, val department: String, val active: Boolean)

    private val people = listOf(
        Person(1, "Finance", true),
        Person(2, "Sales", false),
    )

    private val headers = listOf(
        DataTableHeader<Person>(
            key = "department",
            title = "Department",
            value = { it.department },
            width = 200.dp,
            editable = true,
            editorContent = DataTableEditors.dropdown(listOf("Finance", "IT", "Sales")),
        ),
        DataTableHeader<Person>(
            key = "active",
            title = "Active",
            value = { it.active },
            width = 200.dp,
            editable = true,
            editOnClick = true,
            cellContent = DataTableEditors.checkboxCell { it.active },
            editorContent = DataTableEditors.checkbox(),
        ),
    )

    private class Harness {
        lateinit var state: DataTableState
        val edits = mutableListOf<CellEdit<Person>>()
    }

    private fun ComposeUiTest.table(): Harness {
        val harness = Harness()
        setContent {
            harness.state = rememberDataTableState()
            Box(Modifier.size(700.dp, 400.dp)) {
                DataTable(
                    items = people,
                    headers = headers,
                    itemKey = { it.id },
                    state = harness.state,
                    onCellEdit = { harness.edits += it },
                )
            }
        }
        return harness
    }

    private fun ComposeUiTest.press(key: Key) =
        onAllNodes(isRoot()).onLast().performKeyInput { pressKey(key) }

    @Test
    fun `dropdown lists its options and commits a clicked one`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Finance").performClick()
        press(Key.Enter)
        onNodeWithText("IT").performClick()

        assertFalse(harness.state.isEditing)
        val edit = harness.edits.single()
        assertEquals("Finance", edit.oldText)
        assertEquals("IT", edit.newText)
    }

    @Test
    fun `dropdown commits the highlighted option on Enter`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Finance").performClick()
        press(Key.Enter)
        waitForIdle()
        press(Key.DirectionDown)
        press(Key.Enter)

        assertEquals("IT", harness.edits.single().newText)
    }

    @Test
    fun `dropdown jumps to an option by its first letter`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Finance").performClick()
        press(Key.Enter)
        waitForIdle()
        press(Key.S)
        press(Key.Enter)

        assertEquals("Sales", harness.edits.single().newText)
    }

    @Test
    fun `dropdown jumps to the last option on End`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Finance").performClick()
        press(Key.Enter)
        waitForIdle()
        press(Key.MoveEnd)
        press(Key.Enter)

        assertEquals("Sales", harness.edits.single().newText)
    }

    @Test
    fun `dropdown jumps to the first option on Home`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Sales").performClick()
        press(Key.Enter)
        waitForIdle()
        press(Key.MoveHome)
        press(Key.Enter)

        assertEquals("Finance", harness.edits.single().newText)
    }

    @Test
    fun `dropdown builds a longer prefix from quick typing`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Finance").performClick()
        press(Key.Enter)
        waitForIdle()
        press(Key.I)
        press(Key.T)
        press(Key.Enter)

        assertEquals("IT", harness.edits.single().newText)
    }

    @Test
    fun `dropdown cancels on Escape`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Finance").performClick()
        press(Key.Enter)
        waitForIdle()
        press(Key.Escape)

        assertFalse(harness.state.isEditing)
        assertTrue(harness.edits.isEmpty())
    }

    @Test
    fun `checkbox toggles in one step when the editor opens`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Finance").performClick()
        harness.state.focusCell(1, "active")
        press(Key.Enter)
        waitForIdle()

        assertFalse(harness.state.isEditing)
        val edit = harness.edits.single()
        assertEquals("true", edit.oldText)
        assertEquals("false", edit.newText)
    }

    @Test
    fun `a single click on a checkbox cell toggles it`() = runComposeUiTest {
        val harness = table()

        onAllNodesWithContentDescription("Unchecked").onFirst().performClick()
        waitForIdle()

        assertFalse(harness.state.isEditing)
        assertEquals("true", harness.edits.single().newText)
    }
}
