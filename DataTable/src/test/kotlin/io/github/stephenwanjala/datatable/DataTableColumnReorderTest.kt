package io.github.stephenwanjala.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Dragging a header to a new position.
 *
 * Every column here declares an explicit width, so the drags can be aimed at exact positions —
 * `name` occupies 0–150dp, `age` 150–250dp and `city` 250–400dp — and the assertions read
 * `state.columnKeys`, which the table republishes every composition, so they check the order the
 * table is actually *rendering* rather than the order list behind it.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableColumnReorderTest {

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

    private val grouped = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
        DataTableHeader<Person>(
            key = "detail", title = "Detail",
            children = listOf(
                DataTableHeader(key = "age", title = "Age", value = { it.age }, width = 100.dp),
                DataTableHeader(key = "city", title = "City", value = { it.city }, width = 150.dp),
            ),
        ),
    )

    /** Pixels per dp, so a drag can be aimed in the same units the columns are declared in. */
    private var pixelsPerDp = 1f

    private fun ComposeUiTest.table(
        columns: List<DataTableHeader<Person>> = headers,
        reorderableColumns: Boolean = true,
        width: Dp = 800.dp,
    ): DataTableState {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            pixelsPerDp = LocalDensity.current.density
            Box(Modifier.size(width, 400.dp)) {
                DataTable(
                    items = people,
                    headers = columns,
                    itemKey = { it.id },
                    state = state,
                    reorderableColumns = reorderableColumns,
                )
            }
        }
        return state
    }

    /**
     * Grabs the header titled [title] and drops it at [landAtDp] across the table.
     *
     * Injected at the root so the whole drag is expressed in table coordinates: the header being
     * dragged is not a fixed frame to measure against, since the table reorders under it.
     */
    private fun ComposeUiTest.dragHeader(title: String, landAtDp: Float) {
        val grip = onNodeWithText(title).fetchSemanticsNode().boundsInRoot.center

        onRoot().performMouseInput {
            moveTo(grip)
            press()
            moveTo(Offset(landAtDp * pixelsPerDp, grip.y))
            release()
        }
        waitForIdle()
    }

    // ---- The basic move ----

    @Test
    fun `a header dropped past the middle of its neighbour lands after it`() = runComposeUiTest {
        val state = table()

        dragHeader("Name", landAtDp = 230f)

        assertEquals(listOf("age", "name", "city"), state.columnKeys)
    }

    @Test
    fun `a header dropped before the middle of a column lands in front of it`() = runComposeUiTest {
        val state = table()

        dragHeader("City", landAtDp = 50f)

        assertEquals(listOf("city", "name", "age"), state.columnKeys)
    }

    @Test
    fun `a header dropped back where it started does not move`() = runComposeUiTest {
        val state = table()

        // The near half of its own neighbour is the boundary `name` already sits on.
        dragHeader("Name", landAtDp = 170f)

        assertEquals(listOf("name", "age", "city"), state.columnKeys)
    }

    @Test
    fun `a header dragged off the end of the table lands last`() = runComposeUiTest {
        val state = table()

        dragHeader("Name", landAtDp = 780f)

        assertEquals(listOf("age", "city", "name"), state.columnKeys)
    }

    @Test
    fun `columns stay put unless the table is told they can move`() = runComposeUiTest {
        val state = table(reorderableColumns = false)

        dragHeader("Name", landAtDp = 230f)

        assertEquals(listOf("name", "age", "city"), state.columnKeys)
    }

    @Test
    fun `a drag in a header narrower than its columns is not stolen by the scroll`() =
        runComposeUiTest {
            // 400dp of columns in a 300dp table, so the header is inside a horizontal scroll —
            // which claims sideways drags for panning unless the header gets there first.
            val state = table(width = 300.dp)

            dragHeader("Name", landAtDp = 230f)

            assertEquals(listOf("age", "name", "city"), state.columnKeys)
        }

    // ---- Sharing the press with sorting ----

    @Test
    fun `a header click still sorts once dragging is on`() = runComposeUiTest {
        val state = table()

        onNodeWithText("Name").performMouseInput { moveTo(center); press(); release() }
        waitForIdle()

        assertEquals(SortState("name", SortOrder.ASCENDING), state.captureLayout().sortBy)
        assertEquals(listOf("name", "age", "city"), state.columnKeys)
    }

    @Test
    fun `a drag does not also sort the column it grabbed`() = runComposeUiTest {
        val state = table()

        dragHeader("Name", landAtDp = 230f)

        assertEquals(SortState(), state.captureLayout().sortBy)
    }

    // ---- Grouped headers ----

    @Test
    fun `a group dragged by its band moves every column under it`() = runComposeUiTest {
        val state = table(grouped)

        dragHeader("Detail", landAtDp = 50f)

        assertEquals(listOf("age", "city", "name"), state.columnKeys)
    }

    @Test
    fun `a column dragged inside its group stays in it`() = runComposeUiTest {
        val state = table(grouped)

        dragHeader("Age", landAtDp = 380f)

        assertEquals(listOf("name", "city", "age"), state.columnKeys)
    }

    @Test
    fun `a column dragged past its group stops at the edge of it`() = runComposeUiTest {
        val state = table(grouped)

        // Aimed well outside the group, at `name`. A column is hit-tested against its own
        // siblings only, so the furthest it can go is the front of the group it belongs to.
        dragHeader("City", landAtDp = 50f)

        assertEquals(listOf("name", "city", "age"), state.columnKeys)
    }

    // ---- Frozen columns ----

    @Test
    fun `a column dragged at the frozen section stops at the boundary`() = runComposeUiTest {
        val state = table(
            listOf(headers[0].copy(fixed = true), headers[1], headers[2])
        )

        dragHeader("City", landAtDp = 50f)

        // `city` came to the front of the scrolling section; `name` is still frozen ahead of it.
        assertEquals(listOf("name", "city", "age"), state.columnKeys)
    }

    @Test
    fun `frozen columns reorder among themselves`() = runComposeUiTest {
        val state = table(
            listOf(headers[0].copy(fixed = true), headers[1].copy(fixed = true), headers[2])
        )

        dragHeader("Name", landAtDp = 230f)

        assertEquals(listOf("age", "name", "city"), state.columnKeys)
    }

    // ---- What the move leaves behind ----

    @Test
    fun `a dragged order is part of the captured layout`() = runComposeUiTest {
        val state = table()

        dragHeader("Name", landAtDp = 230f)

        assertEquals(listOf("age", "name", "city"), state.captureLayout().columnOrder)
    }

    @Test
    fun `a hidden column keeps its place while the others are rearranged`() = runComposeUiTest {
        val state = table()

        runOnUiThread { state.setColumnHidden("age", true) }
        waitForIdle()
        // `name` and `city` now span 0-150 and 150-300; drop `name` past the middle of `city`.
        dragHeader("Name", landAtDp = 280f)
        runOnUiThread { state.setColumnHidden("age", false) }
        waitForIdle()

        // `age` was not on display for the drag, so it holds the place it was declared in —
        // still ahead of `city` — rather than being swept to the end of the table.
        assertEquals(listOf("age", "city", "name"), state.columnKeys)
    }
}
