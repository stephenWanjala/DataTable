package io.github.stephenwanjala.datatable

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DataTablePaginationTest {

    data class Person(val id: Int, val name: String)

    private val people = (1..10).map { Person(it, "Person $it") }

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 200.dp),
    )

    @Test
    fun `only the current page is rendered`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showPagination = true, itemsPerPage = 3,
            )
        }

        onNodeWithText("Person 1").assertIsDisplayed()
        onNodeWithText("Person 3").assertIsDisplayed()
        onNodeWithText("Person 4").assertDoesNotExist()
    }

    @Test
    fun `currentPage selects the slice`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showPagination = true, itemsPerPage = 3, currentPage = 1,
            )
        }

        onNodeWithText("Person 4").assertIsDisplayed()
        onNodeWithText("Person 1").assertDoesNotExist()
    }

    @Test
    fun `the footer reports the range and total`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showPagination = true, itemsPerPage = 3,
            )
        }

        onNodeWithText("1–3 of 10").assertIsDisplayed()
        onNodeWithText("Page 1 of 4").assertIsDisplayed()
    }

    @Test
    fun `a page past the end renders the empty state`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showPagination = true, itemsPerPage = 3, currentPage = 99,
            )
        }

        onNodeWithText("No data available").assertIsDisplayed()
    }

    @Test
    fun `without pagination every row is rendered`() = runComposeUiTest {
        setContent {
            DataTable(items = people, headers = headers, itemKey = { it.id })
        }

        onNodeWithText("Person 1").assertIsDisplayed()
        onNodeWithText("Person 10").assertIsDisplayed()
        onNodeWithText("10 items").assertIsDisplayed()
    }

    // ---- manual pagination -----------------------------------------------------------------

    @Test
    fun `manualPagination renders every given row without slicing`() = runComposeUiTest {
        // items IS the page, so all three must show even though itemsPerPage is 3 of 100.
        val page = people.take(3)

        setContent {
            DataTable(
                items = page, headers = headers, itemKey = { it.id },
                showPagination = true, manualPagination = true, totalItems = 100,
                itemsPerPage = 3,
            )
        }

        onNodeWithText("Person 1").assertIsDisplayed()
        onNodeWithText("Person 3").assertIsDisplayed()
    }

    @Test
    fun `manualPagination counts pages from totalItems, not from items`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people.take(3), headers = headers, itemKey = { it.id },
                showPagination = true, manualPagination = true, totalItems = 100,
                itemsPerPage = 3,
            )
        }

        // 100 rows at 3 per page. Counting from items.size would have said "Page 1 of 1".
        onNodeWithText("Page 1 of 34").assertIsDisplayed()
        onNodeWithText("1–3 of 100").assertIsDisplayed()
    }

    @Test
    fun `manualPagination reports the range for a later page`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people.take(3), headers = headers, itemKey = { it.id },
                showPagination = true, manualPagination = true, totalItems = 100,
                itemsPerPage = 3, currentPage = 2,
            )
        }

        onNodeWithText("7–9 of 100").assertIsDisplayed()
    }

    @Test
    fun `manualPagination without totalItems is rejected`() = runComposeUiTest {
        val error = assertFailsWith<IllegalArgumentException> {
            setContent {
                DataTable(
                    items = people, headers = headers, itemKey = { it.id },
                    showPagination = true, manualPagination = true,
                )
            }
        }

        assertTrue(
            error.message!!.contains("totalItems"),
            "the message should name the missing parameter, was: ${error.message}",
        )
    }
}
