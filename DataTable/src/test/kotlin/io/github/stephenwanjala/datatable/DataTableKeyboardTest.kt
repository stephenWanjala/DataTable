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
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Keyboard navigation.
 *
 * The table only receives key events once it holds focus, and its rows are driven by raw pointer
 * input rather than `clickable`, so nothing requests focus on its own. Each test therefore
 * clicks the table first, exactly as a user would — that click is itself part of what is under
 * test, since without it every shortcut is dead.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableKeyboardTest {

    data class Person(val id: Int, val name: String)

    // Unsorted, so a re-sort visibly reorders them.
    private val people = listOf(
        Person(1, "Carol"),
        Person(2, "Alice"),
        Person(3, "Bob"),
    )

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 200.dp),
    )

    private fun ComposeUiTest.press(key: Key) = onRoot().performKeyInput { pressKey(key) }

    /** Clicks a row to hand the table focus, the way a user starts keyboard navigation. */
    private fun ComposeUiTest.focusTable() = onNodeWithText("Carol").performClick()

    @Test
    fun `the table takes focus when clicked`() = runComposeUiTest {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(items = people, headers = headers, itemKey = { it.id }, state = state)
            }
        }

        assertNull(state.focusedKey)

        focusTable()
        press(Key.DirectionDown)

        assertEquals(1, state.focusedKey)
    }

    @Test
    fun `arrow down walks forward`() = runComposeUiTest {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(items = people, headers = headers, itemKey = { it.id }, state = state)
            }
        }

        focusTable()
        press(Key.DirectionDown)
        press(Key.DirectionDown)

        assertEquals(2, state.focusedKey)
    }

    @Test
    fun `arrow up stops at the first row`() = runComposeUiTest {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(items = people, headers = headers, itemKey = { it.id }, state = state)
            }
        }

        focusTable()
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        press(Key.DirectionUp)

        assertEquals(1, state.focusedKey)
    }

    @Test
    fun `End focuses the last row and Home the first`() = runComposeUiTest {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(items = people, headers = headers, itemKey = { it.id }, state = state)
            }
        }

        focusTable()
        press(Key.MoveEnd)
        assertEquals(3, state.focusedKey)

        press(Key.MoveHome)
        assertEquals(1, state.focusedKey)
    }

    @Test
    fun `Enter activates the focused row`() = runComposeUiTest {
        val activated = mutableListOf<String>()

        setContent {
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(
                    items = people, headers = headers, itemKey = { it.id },
                    selectionMode = SelectionMode.NONE,
                    onRowClick = { activated += it.name },
                )
            }
        }

        focusTable()          // this click alone activates Carol
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.Enter)      // focus is on Alice

        assertEquals(listOf("Carol", "Alice"), activated)
    }

    @Test
    fun `Space toggles selection on the focused row`() = runComposeUiTest {
        var emitted: Set<Any>? = null

        setContent {
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(
                    items = people, headers = headers, itemKey = { it.id },
                    showSelect = true,
                    selectedKeys = emptySet(),
                    onSelectionChange = { emitted = it },
                )
            }
        }

        focusTable()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.Spacebar)   // focus is on Alice, id 2

        assertEquals(setOf<Any>(2), emitted)
    }

    @Test
    fun `focus stays on its row across a re-sort`() = runComposeUiTest {
        // The whole point of keying focus rather than holding a position.
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(items = people, headers = headers, itemKey = { it.id }, state = state)
            }
        }

        focusTable()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        assertEquals(2, state.focusedKey)   // Alice, sitting second while unsorted

        onNodeWithText("Name").performClick()   // sort ascending: Alice, Bob, Carol

        // Alice is now first. A position-based focus would have drifted to Bob.
        assertEquals(2, state.focusedKey)
    }

    @Test
    fun `focusRow sets focus without any key press`() = runComposeUiTest {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(items = people, headers = headers, itemKey = { it.id }, state = state)
            }
        }

        state.focusRow(3)
        assertEquals(3, state.focusedKey)

        state.focusRow(null)
        assertNull(state.focusedKey)
    }

    @Test
    fun `arrow keys resume from a programmatically focused row`() = runComposeUiTest {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(items = people, headers = headers, itemKey = { it.id }, state = state)
            }
        }

        focusTable()
        state.focusRow(1)
        press(Key.DirectionDown)

        assertEquals(2, state.focusedKey)
    }
}
