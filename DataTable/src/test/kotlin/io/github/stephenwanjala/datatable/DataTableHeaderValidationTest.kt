package io.github.stephenwanjala.datatable

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Nested header rendering, and the misconfigurations that must fail loudly rather than
 * silently render something wrong.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableHeaderValidationTest {

    data class Person(val id: Int, val name: String, val email: String, val phone: String)

    private val people = listOf(
        Person(1, "Alice", "alice@example.com", "555-0001"),
        Person(2, "Bob", "bob@example.com", "555-0002"),
    )

    private fun grouped(
        leafWidth: Int? = 150,
        secondLeafWidth: Int? = 150,
        leafFixed: Boolean = false,
        secondLeafFixed: Boolean = false,
    ) = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 120.dp),
        DataTableHeader(
            key = "contact",
            title = "Contact",
            children = listOf(
                DataTableHeader(
                    key = "email", title = "Email", value = { it.email },
                    width = leafWidth?.dp, fixed = leafFixed,
                ),
                DataTableHeader(
                    key = "phone", title = "Phone", value = { it.phone },
                    width = secondLeafWidth?.dp, fixed = secondLeafFixed,
                ),
            ),
        ),
    )

    @Test
    fun `a group title renders above its children`() = runComposeUiTest {
        setContent {
            DataTable(items = people, headers = grouped(), itemKey = { it.id })
        }

        onNodeWithText("Contact").assertIsDisplayed()
        onNodeWithText("Email").assertIsDisplayed()
        onNodeWithText("Phone").assertIsDisplayed()
    }

    @Test
    fun `a leaf beside a group keeps its own title`() = runComposeUiTest {
        setContent {
            DataTable(items = people, headers = grouped(), itemKey = { it.id })
        }

        onNodeWithText("Name").assertIsDisplayed()
    }

    @Test
    fun `only leaves become data columns`() = runComposeUiTest {
        setContent {
            DataTable(items = people, headers = grouped(), itemKey = { it.id })
        }

        onNodeWithText("alice@example.com").assertIsDisplayed()
        onNodeWithText("555-0001").assertIsDisplayed()
    }

    @Test
    fun `groups nest to three levels`() = runComposeUiTest {
        val headers = listOf(
            DataTableHeader<Person>(
                key = "outer",
                title = "Outer",
                children = listOf(
                    DataTableHeader(
                        key = "inner",
                        title = "Inner",
                        children = listOf(
                            DataTableHeader(key = "name", title = "Name", value = { it.name }, width = 120.dp),
                            DataTableHeader(key = "email", title = "Email", value = { it.email }, width = 200.dp),
                        ),
                    ),
                ),
            ),
        )

        setContent {
            DataTable(items = people, headers = headers, itemKey = { it.id })
        }

        onNodeWithText("Outer").assertIsDisplayed()
        onNodeWithText("Inner").assertIsDisplayed()
        onNodeWithText("Name").assertIsDisplayed()
        onNodeWithText("Alice").assertIsDisplayed()
    }

    @Test
    fun `a group mixing fixed and weighted leaves is rejected`() = runComposeUiTest {
        val error = assertFailsWith<IllegalArgumentException> {
            setContent {
                DataTable(
                    items = people,
                    headers = grouped(leafWidth = 150, secondLeafWidth = null),
                    itemKey = { it.id },
                )
            }
        }

        assertTrue(
            error.message!!.contains("contact"),
            "the message should name the offending group, was: ${error.message}",
        )
    }

    @Test
    fun `a group with all-weighted leaves is accepted`() = runComposeUiTest {
        // The rule is "all fixed OR all weighted", so this must not be rejected.
        setContent {
            DataTable(
                items = people,
                headers = grouped(leafWidth = null, secondLeafWidth = null),
                itemKey = { it.id },
            )
        }

        onNodeWithText("Contact").assertIsDisplayed()
    }

    @Test
    fun `a group straddling the freeze boundary is rejected`() = runComposeUiTest {
        val error = assertFailsWith<IllegalArgumentException> {
            setContent {
                DataTable(
                    items = people,
                    headers = grouped(leafFixed = true, secondLeafFixed = false),
                    itemKey = { it.id },
                )
            }
        }

        assertTrue(
            error.message!!.contains("contact"),
            "the message should name the offending group, was: ${error.message}",
        )
    }

    @Test
    fun `a frozen column without a width is rejected`() = runComposeUiTest {
        val error = assertFailsWith<IllegalArgumentException> {
            setContent {
                DataTable(
                    items = people,
                    headers = listOf(
                        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, fixed = true),
                    ),
                    itemKey = { it.id },
                )
            }
        }

        assertTrue(
            error.message!!.contains("name"),
            "the message should name the offending column, was: ${error.message}",
        )
    }

    @Test
    fun `a frozen column with a width renders`() = runComposeUiTest {
        setContent {
            DataTable(
                items = people,
                headers = listOf(
                    DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 120.dp, fixed = true),
                    DataTableHeader(key = "email", title = "Email", value = { it.email }, width = 200.dp),
                ),
                itemKey = { it.id },
            )
        }

        onNodeWithText("Name").assertIsDisplayed()
        onNodeWithText("Alice").assertIsDisplayed()
    }
}
