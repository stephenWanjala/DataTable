package io.github.stephenwanjala.datatable

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Selection is keyed on `itemKey`, never on the item, so these assert on the emitted key set.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableSelectionTest {

    /** Deliberately has no `equals`: selection must not depend on it. */
    class Person(val id: Int, val name: String)

    private val people = listOf(
        Person(101, "Alice"),
        Person(102, "Bob"),
        Person(103, "Carol"),
    )

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 200.dp),
    )

    @Test
    fun `selecting a row emits its key`() = runComposeUiTest {
        var emitted: Set<Any>? = null

        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showSelect = true, selectionMode = SelectionMode.MULTI,
                selectedKeys = emptySet(),
                onSelectionChange = { emitted = it },
            )
        }

        onNodeWithText("Bob").performClick()

        assertEquals(setOf<Any>(102), emitted)
    }

    @Test
    fun `multi select adds to the existing selection`() = runComposeUiTest {
        var emitted: Set<Any>? = null

        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showSelect = true, selectionMode = SelectionMode.MULTI,
                selectedKeys = setOf(101),
                onSelectionChange = { emitted = it },
            )
        }

        onNodeWithText("Carol").performClick()

        assertEquals(setOf<Any>(101, 103), emitted)
    }

    @Test
    fun `clicking a selected row deselects it`() = runComposeUiTest {
        var emitted: Set<Any>? = null

        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showSelect = true, selectionMode = SelectionMode.MULTI,
                selectedKeys = setOf(101, 102),
                onSelectionChange = { emitted = it },
            )
        }

        onNodeWithText("Alice").performClick()

        assertEquals(setOf<Any>(102), emitted)
    }

    @Test
    fun `single select replaces rather than accumulates`() = runComposeUiTest {
        var emitted: Set<Any>? = null

        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                showSelect = true, selectionMode = SelectionMode.SINGLE,
                selectedKeys = setOf(101),
                onSelectionChange = { emitted = it },
            )
        }

        onNodeWithText("Carol").performClick()

        assertEquals(setOf<Any>(103), emitted)
    }

    @Test
    fun `NONE mode never changes the selection`() = runComposeUiTest {
        var emitted: Set<Any>? = null

        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                selectionMode = SelectionMode.NONE,
                onSelectionChange = { emitted = it },
            )
        }

        onNodeWithText("Bob").performClick()

        assertNull(emitted)
    }

    @Test
    fun `NONE mode still reports row clicks`() = runComposeUiTest {
        var clicked: String? = null

        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { it.id },
                selectionMode = SelectionMode.NONE,
                onRowClick = { clicked = it.name },
            )
        }

        onNodeWithText("Bob").performClick()

        assertEquals("Bob", clicked)
    }

    @Test
    fun `keys are derived from itemKey, not from the item`() = runComposeUiTest {
        // A non-identity key must be what lands in the set.
        var emitted: Set<Any>? = null

        setContent {
            DataTable(
                items = people, headers = headers, itemKey = { "row-${it.id}" },
                showSelect = true,
                selectedKeys = emptySet(),
                onSelectionChange = { emitted = it },
            )
        }

        onNodeWithText("Carol").performClick()

        assertEquals(setOf<Any>("row-103"), emitted)
    }

    @Test
    fun `selection survives the item instances being replaced`() = runComposeUiTest {
        // Person has no equals, so a rebuilt list is entirely new instances. Keyed selection
        // must still mark the same row.
        val rebuilt = people.map { Person(it.id, it.name) }
        var emitted: Set<Any>? = null

        setContent {
            DataTable(
                items = rebuilt, headers = headers, itemKey = { it.id },
                showSelect = true,
                selectedKeys = setOf(102),
                onSelectionChange = { emitted = it },
            )
        }

        // 102 is already selected, so clicking it must deselect — proving the table matched it.
        onNodeWithText("Bob").performClick()

        assertEquals(emptySet<Any>(), emitted)
    }
}
