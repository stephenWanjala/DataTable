package io.github.stephenwanjala.datatable

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tab order.
 *
 * The table is a single tab stop: Tab moves past it and arrow keys navigate within, which is the
 * ARIA grid pattern and what JavaFX's TableView does. Per-row controls are deliberately out of
 * the Tab order — otherwise Tab walks every checkbox and expand button of every visible row, so
 * the cost of leaving the table grows with the data.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableTabOrderTest {

    data class Person(val id: Int, val name: String)

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 200.dp),
    )

    private fun ComposeUiTest.tab() = onRoot().performKeyInput { pressKey(Key.Tab) }

    /** How many Tab presses it takes to leave the table, or -1 if it never does. */
    private fun ComposeUiTest.tabsToEscape(limit: Int = 40): Int {
        repeat(limit) { i ->
            tab()
            if (onAllNodes(isFocused()).fetchSemanticsNodes().any { node ->
                    node.config.firstOrNull {
                        it.key == androidx.compose.ui.semantics.SemanticsProperties.TestTag
                    }?.value == "after"
                }
            ) return i + 1
        }
        return -1
    }

    private fun composeTable(
        rowCount: Int,
        showSelect: Boolean,
        showExpand: Boolean,
    ): @androidx.compose.runtime.Composable () -> Unit = {
        val people = (1..rowCount).map { Person(it, "Person $it") }
        Column(Modifier.size(600.dp, 500.dp)) {
            BasicText("BEFORE", Modifier.testTag("before").focusable())
            Box(Modifier.weight(1f)) {
                DataTable(
                    items = people, headers = headers, itemKey = { it.id },
                    showSelect = showSelect, showExpand = showExpand,
                )
            }
            BasicText("AFTER", Modifier.testTag("after").focusable())
        }
    }

    @Test
    fun `leaving the table costs the same however many rows it has`() = runComposeUiTest {
        setContent { composeTable(rowCount = 3, showSelect = true, showExpand = true)() }

        onNodeWithText("Person 1").performClick()
        val small = tabsToEscape()

        assertTrue(small in 1..3, "expected the table to be a near-single tab stop, took $small")
    }

    @Test
    fun `a large table is no worse than a small one`() = runComposeUiTest {
        setContent { composeTable(rowCount = 40, showSelect = true, showExpand = true)() }

        onNodeWithText("Person 1").performClick()
        val large = tabsToEscape()

        // Before per-row controls were taken out of the Tab order this was 2 per visible row.
        assertTrue(large in 1..3, "tab cost must not scale with row count, took $large")
    }

    @Test
    fun `a plain table is a single tab stop`() = runComposeUiTest {
        setContent { composeTable(rowCount = 10, showSelect = false, showExpand = false)() }

        onNodeWithText("Person 1").performClick()

        tab()
        onNodeWithTag("after").assertIsFocused()
    }

    @Test
    fun `arrow keys still work after the table is focused`() = runComposeUiTest {
        // Removing the row controls from the Tab order must not disturb navigation.
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            val people = (1..5).map { Person(it, "Person $it") }
            Box(Modifier.size(600.dp, 400.dp)) {
                DataTable(
                    items = people, headers = headers, itemKey = { it.id }, state = state,
                    showSelect = true, showExpand = true,
                )
            }
        }

        onNodeWithText("Person 1").performClick()
        onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        onRoot().performKeyInput { pressKey(Key.DirectionDown) }

        kotlin.test.assertEquals(2, state.focusedKey)
    }

    @Test
    fun `row checkboxes remain clickable`() = runComposeUiTest {
        // Out of the Tab order, but still a working control.
        var emitted: Set<Any>? = null
        val people = (1..3).map { Person(it, "Person $it") }

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

        onNodeWithText("Person 2").performClick()

        kotlin.test.assertEquals(setOf<Any>(2), emitted)
    }
}
