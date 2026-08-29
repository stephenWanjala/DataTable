package io.github.stephenwanjala.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/** The built-in column menu: the table's own show/hide UI. */
@OptIn(ExperimentalTestApi::class)
class DataTableColumnMenuTest {

    data class Person(val id: Int, val name: String, val age: Int, val city: String)

    private val people = listOf(Person(1, "Carol", 35, "Nairobi"))

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
        DataTableHeader<Person>(key = "age", title = "Age", value = { it.age }, width = 100.dp),
        DataTableHeader<Person>(key = "city", title = "City", value = { it.city }, width = 150.dp),
    )

    private fun ComposeUiTest.table(
        columns: List<DataTableHeader<Person>> = headers,
        showColumnMenuButton: Boolean = true,
    ): DataTableState {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(800.dp, 400.dp)) {
                DataTable(
                    items = people,
                    headers = columns,
                    itemKey = { it.id },
                    state = state,
                    showColumnMenuButton = showColumnMenuButton,
                )
            }
        }
        return state
    }

    private fun ComposeUiTest.openMenu() =
        onNodeWithContentDescription("Show or hide columns").performClick()

    @Test
    fun `the button appears only when asked for`() = runComposeUiTest {
        table(showColumnMenuButton = false)

        onNodeWithContentDescription("Show or hide columns").assertDoesNotExist()
    }

    @Test
    fun `the menu lists every column`() = runComposeUiTest {
        table()
        openMenu()

        // Both the header and the menu carry the title, so the menu adds a second node.
        assertEquals(2, onAllNodesWithText("Name").fetchSemanticsNodes().size)
        assertEquals(2, onAllNodesWithText("City").fetchSemanticsNodes().size)
    }

    @Test
    fun `unchecking a column hides it, and checking it brings it back`() = runComposeUiTest {
        val state = table()

        openMenu()
        onNode(isToggleable() and hasText("City")).performClick()
        waitForIdle()

        assertEquals(listOf("name", "age"), state.columnKeys)
        onNodeWithText("Nairobi").assertDoesNotExist()

        onNode(isToggleable() and hasText("City")).performClick()
        waitForIdle()

        assertEquals(listOf("name", "age", "city"), state.columnKeys)
        onNodeWithText("Nairobi").assertIsDisplayed()
    }

    @Test
    fun `a column its header declares invisible is not offered`() = runComposeUiTest {
        table(headers.map { if (it.key == "city") it.copy(visible = false) else it })
        openMenu()

        // Neither in the header nor in the menu: hiding it was the caller's decision.
        assertEquals(0, onAllNodesWithText("City").fetchSemanticsNodes().size)
    }

    @Test
    fun `a group's leaf columns are listed, not the group`() = runComposeUiTest {
        table(
            listOf(
                DataTableHeader(key = "name", title = "Name", value = { it.name }, width = 150.dp),
                DataTableHeader(
                    key = "detail", title = "Detail",
                    children = listOf(
                        DataTableHeader(key = "age", title = "Age", value = { it.age }, width = 100.dp),
                        DataTableHeader(key = "city", title = "City", value = { it.city }, width = 150.dp),
                    ),
                ),
            )
        )
        openMenu()

        assertEquals(2, onAllNodesWithText("Age").fetchSemanticsNodes().size)
        // The group band is drawn once, in the header only.
        assertEquals(1, onAllNodesWithText("Detail").fetchSemanticsNodes().size)
    }
}
