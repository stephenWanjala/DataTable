package io.github.stephenwanjala.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Saving and restoring how a user arranged a table.
 *
 * The tests that matter most are the ones where the layout and the table have drifted apart: a
 * column since removed, one that did not exist when the layout was saved, and a stored string
 * that was never a layout at all.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableLayoutTest {

    data class Person(val id: Int, val name: String, val age: Int, val city: String)

    private val people = listOf(
        Person(1, "Carol", 35, "Nairobi"),
        Person(2, "Alice", 30, "Mombasa"),
    )

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
        DataTableHeader<Person>(key = "age", title = "Age", value = { it.age }, width = 100.dp),
        DataTableHeader<Person>(key = "city", title = "City", value = { it.city }, width = 150.dp),
    )

    /**
     * Mutates the state and lets the table recompose: `columnKeys` and the captured sort are
     * published from the table's composition, so reading them straight after would be a frame
     * behind.
     */
    private fun ComposeUiTest.update(block: () -> Unit) {
        runOnUiThread(block)
        waitForIdle()
    }

    private fun ComposeUiTest.table(
        columns: List<DataTableHeader<Person>> = headers,
        configure: @androidx.compose.runtime.Composable (DataTableState) -> Unit = {},
    ): DataTableState {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            configure(state)
            Box(Modifier.size(800.dp, 400.dp)) {
                DataTable(items = people, headers = columns, itemKey = { it.id }, state = state)
            }
        }
        return state
    }

    // ---- The encoded form ----

    @Test
    fun `a layout round-trips through its encoded form`() {
        val layout = DataTableLayout(
            columnWidths = mapOf("name" to 150.dp, "age" to 80.5.dp),
            hiddenColumns = setOf("city"),
            columnOrder = listOf("age", "name", "city"),
            sortBy = SortState("age", SortOrder.DESCENDING),
            multiSortBy = listOf(SortState("name", SortOrder.ASCENDING), SortState("age", SortOrder.DESCENDING)),
            filters = mapOf("name" to "car"),
        )

        assertEquals(layout, DataTableLayout.decodeFromString(layout.encodeToString()))
    }

    @Test
    fun `an empty layout round-trips`() {
        val layout = DataTableLayout()

        assertEquals(layout, DataTableLayout.decodeFromString(layout.encodeToString()))
    }

    @Test
    fun `keys and queries holding the format's own punctuation survive`() {
        // A filter query is whatever a user typed, delimiters included.
        val layout = DataTableLayout(
            columnWidths = mapOf("order total = net" to 120.dp),
            hiddenColumns = setOf("a b\tc"),
            filters = mapOf("notes" to "line one\nline two = 100% sure"),
            sortBy = SortState("order total = net", SortOrder.ASCENDING),
        )

        assertEquals(layout, DataTableLayout.decodeFromString(layout.encodeToString()))
    }

    @Test
    fun `encoding the same layout twice gives the same string`() {
        val one = DataTableLayout(
            columnWidths = linkedMapOf("name" to 100.dp, "age" to 80.dp),
            filters = linkedMapOf("name" to "a", "city" to "b"),
        )
        val other = DataTableLayout(
            columnWidths = linkedMapOf("age" to 80.dp, "name" to 100.dp),
            filters = linkedMapOf("city" to "b", "name" to "a"),
        )

        assertEquals(one.encodeToString(), other.encodeToString())
    }

    @Test
    fun `anything that is not a layout decodes to null`() {
        assertNull(DataTableLayout.decodeFromString(""))
        assertNull(DataTableLayout.decodeFromString("   "))
        assertNull(DataTableLayout.decodeFromString("""{"columnWidths":{}}"""))
        assertNull(DataTableLayout.decodeFromString("dtlayout/99\nw name=100.0"))
    }

    @Test
    fun `an unreadable entry is skipped, not fatal`() {
        val encoded = DataTableLayout(
            columnWidths = mapOf("name" to 150.dp),
            hiddenColumns = setOf("city"),
        ).encodeToString()

        val damaged = encoded
            .replace("w name=150.0", "w name=not-a-number")
            .plus("x something-a-later-version-wrote\n")

        val decoded = DataTableLayout.decodeFromString(damaged)

        // The width is gone; everything else came back.
        assertEquals(emptyMap(), decoded?.columnWidths)
        assertEquals(setOf("city"), decoded?.hiddenColumns)
    }

    // ---- Hiding ----

    @Test
    fun `hiding a column takes it out of the table`() {
        runComposeUiTest {
            val state = table()

            onNodeWithText("City").assertIsDisplayed()

            update { state.setColumnHidden("city", true) }

            onNodeWithText("City").assertDoesNotExist()
            onNodeWithText("Nairobi").assertDoesNotExist()
            onNodeWithText("Name").assertIsDisplayed()
        }
    }

    @Test
    fun `showing a column again brings it back`() {
        runComposeUiTest {
            val state = table()

            update { state.setColumnHidden("city", true) }
            onNodeWithText("City").assertDoesNotExist()

            update { state.setColumnHidden("city", false) }
            onNodeWithText("City").assertIsDisplayed()
        }
    }

    @Test
    fun `a column its header declares invisible stays hidden`() {
        runComposeUiTest {
            val state = table(headers.map { if (it.key == "city") it.copy(visible = false) else it })

            // The state can hide a column; it cannot overrule a caller who says the column is
            // not available at all.
            update { state.setColumnHidden("city", false) }

            onNodeWithText("City").assertDoesNotExist()
            assertTrue(!state.isColumnHidden("city"))
        }
    }

    // ---- Order ----

    @Test
    fun `columnOrder puts the columns in the order it names`() {
        runComposeUiTest {
            val state = table()

            assertEquals(listOf("name", "age", "city"), state.columnKeys)

            update { state.columnOrder = listOf("city", "age") }

            // Named columns first, in the order given; the rest keep their declared order after.
            assertEquals(listOf("city", "age", "name"), state.columnKeys)
        }
    }

    @Test
    fun `moveColumn moves one column among the others`() {
        runComposeUiTest {
            val state = table()

            update { state.moveColumn("city", 0) }
            assertEquals(listOf("city", "name", "age"), state.columnKeys)

            update { state.moveColumn("city", 99) }
            assertEquals(listOf("name", "age", "city"), state.columnKeys)
        }
    }

    @Test
    fun `a group moves as a block, and keeps its own columns`() {
        runComposeUiTest {
            lateinit var state: DataTableState
            val grouped = listOf(
                DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
                DataTableHeader<Person>(
                    key = "detail", title = "Detail",
                    children = listOf(
                        DataTableHeader(key = "age", title = "Age", value = { it.age }, width = 100.dp),
                        DataTableHeader(key = "city", title = "City", value = { it.city }, width = 150.dp),
                    ),
                ),
            )
            setContent {
                state = rememberDataTableState()
                Box(Modifier.size(800.dp, 400.dp)) {
                    DataTable(items = people, headers = grouped, itemKey = { it.id }, state = state)
                }
            }

            update { state.columnOrder = listOf("city") }

            // Naming a column inside a group moves the group ahead of `name` and moves that
            // column to the front within it. What it cannot do is take the column out of the
            // group: `city` is still under `Detail`, beside `age`.
            assertEquals(listOf("city", "age", "name"), state.columnKeys)
            onNodeWithText("Detail").assertIsDisplayed()
        }
    }

    // ---- Capture and apply ----

    @Test
    fun `a captured layout holds what the user changed`() {
        runComposeUiTest {
            val state = table(headers.map { if (it.key == "name") it.copy(filterable = true) else it })

            update {
                state.setColumnHidden("city", true)
                state.moveColumn("age", 0)
                state.columnWidths["name"] = 220.dp
            }
            onNodeWithText("Name").performClick()          // sort ascending
            onNode(hasSetTextAction()).performTextInput("car")
            waitForIdle()

            val layout = state.captureLayout()

            assertEquals(mapOf("name" to 220.dp), layout.columnWidths)
            assertEquals(setOf("city"), layout.hiddenColumns)
            assertEquals(listOf("age", "name", "city"), layout.columnOrder)
            assertEquals(SortState("name", SortOrder.ASCENDING), layout.sortBy)
            assertEquals(mapOf("name" to "car"), layout.filters)
        }
    }

    @Test
    fun `a layout captured under a controlled sort still holds it`() {
        runComposeUiTest {
            lateinit var state: DataTableState
            setContent {
                state = rememberDataTableState()
                Box(Modifier.size(800.dp, 400.dp)) {
                    DataTable(
                        items = people, headers = headers, itemKey = { it.id }, state = state,
                        // The caller owns the sort here; a snapshot still has to describe what
                        // the user is looking at.
                        sortBy = SortState("age", SortOrder.DESCENDING),
                        onSortChange = {},
                    )
                }
            }

            waitForIdle()
            assertEquals(SortState("age", SortOrder.DESCENDING), state.captureLayout().sortBy)
        }
    }

    @Test
    fun `applying a layout puts the table back`() {
        runComposeUiTest {
            val state = table(headers.map { if (it.key == "name") it.copy(filterable = true) else it })

            update {
                state.applyLayout(
                    DataTableLayout(
                        columnWidths = mapOf("name" to 200.dp),
                        hiddenColumns = setOf("city"),
                        columnOrder = listOf("age", "name"),
                        sortBy = SortState("age", SortOrder.DESCENDING),
                        filters = mapOf("name" to "alice"),
                    )
                )
            }

            assertEquals(listOf("age", "name"), state.columnKeys)
            onNodeWithText("City").assertDoesNotExist()
            // Filtered to Alice, and the filter field shows what is filtering it.
            onNodeWithText("Alice").assertIsDisplayed()
            onNodeWithText("Carol").assertDoesNotExist()
            onNodeWithText("alice").assertIsDisplayed()
        }
    }

    @Test
    fun `a layout applied before the table composes keeps its sort and filters`() {
        runComposeUiTest {
            lateinit var state: DataTableState
            setContent {
                state = rememberDataTableState()
                // The startup restore: read a preference, apply it, then compose the table. The
                // table seeds its own sort and filters from its parameters, and must not do that
                // over the top of a layout that has already been put back.
                remember {
                    state.applyLayout(
                        DataTableLayout(
                            sortBy = SortState("age", SortOrder.DESCENDING),
                            filters = mapOf("name" to "carol"),
                        )
                    )
                }
                Box(Modifier.size(800.dp, 400.dp)) {
                    DataTable(
                        items = people,
                        headers = headers.map {
                            if (it.key == "name") it.copy(filterable = true) else it
                        },
                        itemKey = { it.id },
                        state = state,
                    )
                }
            }
            waitForIdle()

            assertEquals(SortState("age", SortOrder.DESCENDING), state.captureLayout().sortBy)
            onNodeWithText("Carol").assertIsDisplayed()
            onNodeWithText("Alice").assertDoesNotExist()
        }
    }

    @Test
    fun `a layout survives the columns it names being gone`() {
        runComposeUiTest {
            val state = table()

            update {
                state.applyLayout(
                    DataTableLayout(
                        columnWidths = mapOf("salary" to 200.dp),   // a column since removed
                        hiddenColumns = setOf("salary"),
                        columnOrder = listOf("salary", "city"),     // and one it never knew about
                    )
                )
            }

            // "city" is named and leads; "name" and "age" follow in declared order.
            assertEquals(listOf("city", "name", "age"), state.columnKeys)
            onNodeWithText("Name").assertIsDisplayed()
        }
    }

    @Test
    fun `resetLayout drops the arrangement but not what is on show`() {
        runComposeUiTest {
            val state = table()

            update {
                state.setColumnHidden("city", true)
                state.moveColumn("age", 0)
                state.columnWidths["name"] = 220.dp
            }
            onNodeWithText("Name").performClick()
            waitForIdle()

            update { state.resetLayout() }

            assertEquals(listOf("name", "age", "city"), state.columnKeys)
            assertEquals(emptySet(), state.hiddenColumns)
            assertTrue(state.columnWidths.isEmpty())
            // The sort is what the table is showing, not how it is arranged: still there.
            assertEquals(SortState("name", SortOrder.ASCENDING), state.captureLayout().sortBy)
        }
    }

    @Test
    fun `a layout can be saved and restored across a new table`() {
        val saved: String = run {
            var encoded = ""
            runComposeUiTest {
                val state = table()
                update {
                    state.setColumnHidden("age", true)
                    state.columnWidths["name"] = 190.dp
                }
                waitForIdle()
                encoded = state.captureLayout().encodeToString()
            }
            encoded
        }

        runComposeUiTest {
            lateinit var state: DataTableState
            setContent {
                state = rememberDataTableState()
                // Restored before the first composition, the way an application reading a
                // preference on startup would.
                remember {
                    DataTableLayout.decodeFromString(saved)?.let { state.applyLayout(it) }
                }
                Box(Modifier.size(800.dp, 400.dp)) {
                    DataTable(items = people, headers = headers, itemKey = { it.id }, state = state)
                }
            }

            onNodeWithText("Age").assertDoesNotExist()
            assertEquals(190.dp, state.columnWidths["name"])
        }
    }
}
