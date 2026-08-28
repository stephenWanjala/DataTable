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
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Clipboard copy.
 *
 * The table builds a [ClipboardSelection] and, unless the caller intercepts it, writes it to the
 * system clipboard as tab-separated text. These tests take the `onCopy` route throughout: it is
 * the only one a headless JVM can exercise, and it is also what an application wanting CSV or
 * XLSX will use.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableClipboardTest {

    data class Person(val id: Int, val name: String, val role: String, val city: String)

    private val people = listOf(
        Person(1, "Carol", "Buyer", "Nairobi"),
        Person(2, "Alice", "Clerk", "Mombasa"),
        Person(3, "Bob", "Auditor", "Kisumu"),
    )

    private val headers = listOf(
        DataTableHeader<Person>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
        DataTableHeader<Person>(key = "role", title = "Role", value = { it.role }, width = 150.dp),
        DataTableHeader<Person>(key = "city", title = "City", value = { it.city }, width = 150.dp),
    )

    private fun ComposeUiTest.pressWithShift(key: Key) =
        onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(key) } }

    private fun ComposeUiTest.copy() =
        onRoot().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.C) } }

    private class Harness {
        lateinit var state: DataTableState
        var copied: ClipboardSelection<Person>? = null
    }

    private fun ComposeUiTest.table(
        cellNavigation: Boolean = true,
        selectedKeys: Set<Any> = emptySet(),
    ): Harness {
        val harness = Harness()
        setContent {
            harness.state = rememberDataTableState()
            Box(Modifier.size(700.dp, 400.dp)) {
                DataTable(
                    items = people,
                    headers = headers,
                    itemKey = { it.id },
                    state = harness.state,
                    cellNavigation = cellNavigation,
                    showSelect = selectedKeys.isNotEmpty(),
                    selectedKeys = selectedKeys,
                    onCopy = { harness.copied = it },
                )
            }
        }
        return harness
    }

    @Test
    fun `Ctrl+C copies the selected block as tab-separated rows`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Carol").performClick()
        pressWithShift(Key.DirectionRight)
        pressWithShift(Key.DirectionDown)
        copy()

        val selection = harness.copied
        assertEquals(listOf("name", "role"), selection?.columns?.map { it.key })
        assertEquals(listOf(1, 2), selection?.rows?.map { it.id })
        assertEquals("Carol\tBuyer\nAlice\tClerk", selection?.toTabSeparated())
    }

    @Test
    fun `the header row is available but off by default`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Carol").performClick()
        pressWithShift(Key.DirectionRight)
        copy()

        assertEquals("Carol\tBuyer", harness.copied?.toTabSeparated())
        assertEquals(
            "Name\tRole\nCarol\tBuyer",
            harness.copied?.toTabSeparated(includeHeader = true),
        )
    }

    @Test
    fun `with no block, Ctrl+C copies the checked rows across every column`() = runComposeUiTest {
        val harness = table(selectedKeys = setOf(3, 1))

        onNodeWithText("Carol").performClick()
        copy()

        // Display order, not the order the keys happened to be in the set.
        assertEquals(listOf(1, 3), harness.copied?.rows?.map { it.id })
        assertEquals(
            "Carol\tBuyer\tNairobi\nBob\tAuditor\tKisumu",
            harness.copied?.toTabSeparated(),
        )
    }

    @Test
    fun `with neither, Ctrl+C copies the focused cell alone`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Clerk").performClick()
        copy()

        assertEquals("Clerk", harness.copied?.toTabSeparated())
        assertEquals(listOf("role"), harness.copied?.columns?.map { it.key })
    }

    @Test
    fun `without cell navigation, Ctrl+C copies the focused row`() = runComposeUiTest {
        val harness = table(cellNavigation = false)

        onNodeWithText("Carol").performClick()
        onRoot().performKeyInput { pressKey(Key.DirectionDown) }   // focus the first row
        copy()

        assertEquals("Carol\tBuyer\tNairobi", harness.copied?.toTabSeparated())
    }

    @Test
    fun `Ctrl+C is left alone when there is nothing to copy`() = runComposeUiTest {
        // Consuming it unconditionally would take Ctrl+C away from the surrounding application
        // whenever the table merely happened to hold focus.
        val harness = table(cellNavigation = false)

        onNodeWithText("Carol").performClick()
        copy()

        assertNull(harness.copied)
    }

    @Test
    fun `a copied block follows the current sort order`() = runComposeUiTest {
        val harness = table()

        onNodeWithText("Name").performClick()   // ascending: Alice, Bob, Carol
        onNodeWithText("Alice").performClick()
        pressWithShift(Key.DirectionDown)
        copy()

        assertEquals(listOf(2, 3), harness.copied?.rows?.map { it.id })
        assertEquals("Alice\nBob", harness.copied?.toTabSeparated())
    }

    @Test
    fun `onCopy replaces the clipboard write rather than running alongside it`() =
        runComposeUiTest {
            // The contract that lets a handler redirect or suppress a copy — and the one that
            // caught out the gallery sample, which displayed the payload and copied nothing.
            val harness = table()

            onNodeWithText("Carol").performClick()
            copy()

            assertNotNull(harness.copied)
            // A handler that wants both calls `copyToSystemClipboard()` itself. Here that is the
            // only route to the clipboard, and this test deliberately does not take it.
        }

    @Test
    fun `a clipboard that cannot be reached fails soft`() {
        // These tests run headless, so there is no system clipboard at all. Writing to it must
        // report failure rather than throw: a copy that cannot complete should leave the
        // application running.
        assertFalse(copyToSystemClipboard("anything"))

        val selection = ClipboardSelection(
            rows = listOf("only"),
            columns = listOf(DataTableHeader<String>(key = "c", title = "C")),
            cells = listOf(listOf("only")),
        )
        assertFalse(selection.copyToSystemClipboard())
    }

    @Test
    fun `Ctrl+C on the default path does not throw when the clipboard is unreachable`() =
        runComposeUiTest {
            // No onCopy, so the table takes its own clipboard route — which fails headless.
            lateinit var state: DataTableState
            setContent {
                state = rememberDataTableState()
                Box(Modifier.size(700.dp, 400.dp)) {
                    DataTable(
                        items = people,
                        headers = headers,
                        itemKey = { it.id },
                        state = state,
                        cellNavigation = true,
                    )
                }
            }

            onNodeWithText("Carol").performClick()
            copy()

            // Reaching here at all is the assertion; the cursor is untouched by a failed copy.
            assertEquals(CellPosition(1, "name"), state.focusedCell)
        }

    @Test
    fun `values that would break the row structure are quoted`() {
        val selection = ClipboardSelection(
            rows = listOf("a", "b"),
            columns = listOf(
                DataTableHeader<String>(key = "notes", title = "Notes"),
                DataTableHeader<String>(key = "amount", title = "Amount"),
            ),
            cells = listOf(
                listOf("line one\nline two", "10"),
                listOf("has\ttab", "he said \"no\""),
            ),
        )

        assertEquals(
            "\"line one\nline two\"\t10\n\"has\ttab\"\t\"he said \"\"no\"\"\"",
            selection.toTabSeparated(),
        )
    }

    @Test
    fun `a column with no value extractor copies as empty`() = runComposeUiTest {
        // Cells rendered only through `cellContent` have no text the table can read back.
        val harness = Harness()
        setContent {
            harness.state = rememberDataTableState()
            Box(Modifier.size(700.dp, 400.dp)) {
                DataTable(
                    items = people,
                    headers = listOf(
                        headers[0],
                        DataTableHeader(
                            key = "badge", title = "Badge", width = 120.dp,
                            cellContent = { },
                        ),
                    ),
                    itemKey = { it.id },
                    state = harness.state,
                    cellNavigation = true,
                    onCopy = { harness.copied = it },
                )
            }
        }

        onNodeWithText("Carol").performClick()
        pressWithShift(Key.DirectionRight)
        copy()

        assertEquals("Carol\t", harness.copied?.toTabSeparated())
    }

    @Test
    fun `the payload carries the row items, not just their text`() = runComposeUiTest {
        // What makes this the seam an exporter hangs off: the caller gets their own type back.
        val harness = table()

        onNodeWithText("Carol").performClick()
        pressWithShift(Key.DirectionDown)
        copy()

        // Reading `.name` off the payload is the assertion: the rows come back as Person, not
        // as the flattened strings an exporter would then have to re-parse.
        assertEquals(listOf("Carol", "Alice"), harness.copied?.rows?.map { it.name })
        assertEquals(listOf(1, 2), harness.copied?.rows?.map { it.id })
    }
}
