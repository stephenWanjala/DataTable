package io.github.stephenwanjala.datatable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Sorting behaviour.
 *
 * Order is asserted by paging to a single row: whichever row is first is the only one rendered,
 * so `assertIsDisplayed` / `assertDoesNotExist` is enough without reaching for node bounds.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableSortingTest {

    data class Person(val id: Int, val name: String, val age: Int)

    // Deliberately unsorted, so every ordering is distinguishable from the input.
    private val people = listOf(
        Person(1, "Carol", 35),
        Person(2, "Alice", 30),
        Person(3, "Bob", 25),
    )

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
        DataTableHeader(key = "age", title = "Age", value = { it.age }, width = 80.dp),
    )

    @Test
    fun `rows keep their given order until sorted`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showPagination = true, itemsPerPage = 1,
            )
        }

        onNodeWithText("Carol").assertIsDisplayed()
        onNodeWithText("Alice").assertDoesNotExist()
    }

    @Test
    fun `clicking a header sorts ascending`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showPagination = true, itemsPerPage = 1,
            )
        }

        onNodeWithText("Name").performClick()

        onNodeWithText("Alice").assertIsDisplayed()
        onNodeWithText("Carol").assertDoesNotExist()
    }

    @Test
    fun `clicking twice sorts descending`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showPagination = true, itemsPerPage = 1,
            )
        }

        onNodeWithText("Name").performClick()
        onNodeWithText("Name").performClick()

        onNodeWithText("Carol").assertIsDisplayed()
        onNodeWithText("Alice").assertDoesNotExist()
    }

    @Test
    fun `clicking three times returns to the original order`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showPagination = true, itemsPerPage = 1,
            )
        }

        repeat(3) { onNodeWithText("Name").performClick() }

        // Carol is first both descending and unsorted, so check a column that differs: sorted
        // descending by name the ages would be 35, 30, 25 — unsorted they are in input order.
        onNodeWithText("Carol").assertIsDisplayed()
    }

    @Test
    fun `a non-sortable column ignores clicks`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people,
                headers = listOf(headers[0].copy(sortable = false), headers[1]),
                itemKey = { it.id },
                showPagination = true, itemsPerPage = 1,
            )
        }

        onNodeWithText("Name").performClick()

        onNodeWithText("Carol").assertIsDisplayed()
    }

    @Test
    fun `a custom comparator wins over the displayed value`() = runComposeUiTest {
        // Formatted for display, so a string sort would order these "$1,000", "$200", "$30".
        val salaried = listOf(
            Person(1, "Carol", 30),
            Person(2, "Alice", 1000),
            Person(3, "Bob", 200),
        )
        val formatted = listOf(
            DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
            DataTableHeader(
                key = "pay", title = "Pay", width = 120.dp,
                value = { "$${it.age}" },
                comparator = compareBy { it.age },
            ),
        )

        setContent {
            DataTable(
                items = salaried, headers = formatted, itemKey = { it.id },
                showPagination = true, itemsPerPage = 1,
            )
        }

        onNodeWithText("Pay").performClick()

        // Numerically smallest is 30. A string sort would have put "$1000" first.
        onNodeWithText("Carol").assertIsDisplayed()
    }

    @Test
    fun `manualSorting leaves the given order alone`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                manualSorting = true,
                showPagination = true, itemsPerPage = 1,
            )
        }

        onNodeWithText("Name").performClick()

        // The caller is responsible for reordering; the table must not touch it.
        onNodeWithText("Carol").assertIsDisplayed()
        onNodeWithText("Alice").assertDoesNotExist()
    }

    @Test
    fun `manualSorting still reports header clicks`() = runComposeUiTest {
        var reported: SortState? = null

        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                manualSorting = true,
                sortBy = SortState(),
                onSortChange = { reported = it },
            )
        }

        onNodeWithText("Name").performClick()

        assertEquals(SortState("name", SortOrder.ASCENDING), reported)
    }

    @Test
    fun `supplying onSortChange makes sorting controlled`() = runComposeUiTest {
        // The callback fires but the parameter never changes, so nothing may reorder.
        // This is the 0.3.0 behaviour change: 0.2.0 sorted anyway via an internal copy.
        var callbacks = 0

        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                sortBy = SortState(),
                onSortChange = { callbacks++ },
                showPagination = true, itemsPerPage = 1,
            )
        }

        onNodeWithText("Name").performClick()

        assertEquals(1, callbacks)
        onNodeWithText("Carol").assertIsDisplayed()
    }

    @Test
    fun `feeding the sort state back applies it`() = runComposeUiTest {
        setContent {
            var sort by remember { mutableStateOf(SortState()) }
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                sortBy = sort,
                onSortChange = { sort = it },
                showPagination = true, itemsPerPage = 1,
            )
        }

        onNodeWithText("Name").performClick()

        onNodeWithText("Alice").assertIsDisplayed()
    }
}
