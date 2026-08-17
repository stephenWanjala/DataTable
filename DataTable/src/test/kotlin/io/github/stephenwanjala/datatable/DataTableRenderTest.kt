package io.github.stephenwanjala.datatable

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DataTableRenderTest {

    data class Person(val id: Int, val name: String, val age: Int)

    private val people = listOf(
        Person(1, "Alice", 30),
        Person(2, "Bob", 25),
        Person(3, "Carol", 35),
    )

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
        DataTableHeader(key = "age", title = "Age", value = { it.age }, width = 80.dp),
    )

    @Test
    fun `renders header titles`() = runComposeUiTest {
        setContent {
            DataTable(items = people, headers = headers, itemKey = { it.id })
        }

        onNodeWithText("Name").assertIsDisplayed()
        onNodeWithText("Age").assertIsDisplayed()
    }

    @Test
    fun `renders cell values`() = runComposeUiTest {
        setContent {
            DataTable(items = people, headers = headers, itemKey = { it.id })
        }

        onNodeWithText("Alice").assertIsDisplayed()
        onNodeWithText("Bob").assertIsDisplayed()
        onNodeWithText("30").assertIsDisplayed()
    }

    @Test
    fun `shows the empty state when there are no rows`() = runComposeUiTest {
        setContent {
            DataTable(items = emptyList<Person>(), headers = headers, itemKey = { it.id })
        }

        onNodeWithText("No data available").assertIsDisplayed()
    }

    @Test
    fun `shows custom empty content when supplied`() = runComposeUiTest {
        setContent {
            DataTable(
                items = emptyList<Person>(),
                headers = headers,
                itemKey = { it.id },
                noDataContent = { androidx.compose.foundation.text.BasicText("Nothing here") },
            )
        }

        onNodeWithText("Nothing here").assertIsDisplayed()
    }

    @Test
    fun `hides rows while loading`() = runComposeUiTest {
        setContent {
            DataTable(items = people, headers = headers, itemKey = { it.id }, loading = true)
        }

        onNodeWithText("Alice").assertDoesNotExist()
    }

    @Test
    fun `invisible columns are not rendered`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people,
                headers = listOf(
                    headers[0],
                    headers[1].copy(visible = false),
                ),
                itemKey = { it.id },
            )
        }

        onNodeWithText("Name").assertIsDisplayed()
        onNodeWithText("Age").assertDoesNotExist()
    }
}
