package io.github.stephenwanjala.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Per-column display formatting.
 *
 * The point of `format` is that it is the *only* thing it touches: the cell reads formatted,
 * while sorting, editing, and validation all keep seeing the raw value. These tests hold that
 * line — each one changes the formatted text and asserts the raw behaviour is unmoved.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableFormatTest {

    data class Product(val id: Int, val name: String, val price: Double, val note: String?)

    // Prices whose numeric order (5, 40, 100) is not their lexicographic one ("100", "40", "5").
    private val products = listOf(
        Product(1, "Bolt", 40.0, "spare"),
        Product(2, "Nut", 100.0, null),
        Product(3, "Washer", 5.0, "spare"),
    )

    private fun priceColumn(editable: Boolean = false) = DataTableHeader<Product>(
        key = "price",
        title = "Price",
        value = { it.price },
        width = 150.dp,
        editable = editable,
        format = { "KES ${"%,.2f".format(it)}" },
    )

    private fun headers(editable: Boolean = false) = listOf(
        DataTableHeader<Product>(key = "name", title = "Name", value = { it.name }, width = 150.dp),
        priceColumn(editable),
    )

    @Test
    fun `a cell shows the formatted value, not the raw one`() = runComposeUiTest {
        setContent {
            DataTable(items = products, headers = headers(), itemKey = { it.id })
        }

        onNodeWithText("KES 40.00").assertIsDisplayed()
        onNodeWithText("40.0").assertDoesNotExist()
    }

    @Test
    fun `a column with no format still renders the raw value`() = runComposeUiTest {
        setContent {
            DataTable(
                items = products,
                headers = listOf(
                    DataTableHeader<Product>(
                        key = "price", title = "Price", value = { it.price }, width = 150.dp,
                    ),
                ),
                itemKey = { it.id },
            )
        }

        onNodeWithText("40.0").assertIsDisplayed()
    }

    @Test
    fun `the formatter sees a null value`() = runComposeUiTest {
        setContent {
            DataTable(
                items = products,
                headers = listOf(
                    DataTableHeader<Product>(
                        key = "note", title = "Note", value = { it.note }, width = 150.dp,
                        format = { it?.toString() ?: "—" },
                    ),
                ),
                itemKey = { it.id },
            )
        }

        onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun `sorting orders by the raw value, not by the formatted text`() = runComposeUiTest {
        setContent {
            // One row per page, so whichever row sorts first is the only one rendered.
            DataTable(
                items = products, headers = headers(), itemKey = { it.id },
                showPagination = true, itemsPerPage = 1,
            )
        }

        onNodeWithText("Price").performClick()

        // Numerically 5 leads; lexicographically "KES 100.00" would.
        onNodeWithText("KES 5.00").assertIsDisplayed()
        onNodeWithText("KES 100.00").assertDoesNotExist()
    }

    @Test
    fun `an editor opens on the raw value, not on the formatted text`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(700.dp, 400.dp)) {
                DataTable(items = products, headers = headers(editable = true), itemKey = { it.id })
            }
        }

        onNodeWithText("KES 40.00").performClick()
        onRoot().performKeyInput { pressKey(Key.F2) }

        onNodeWithText("40.0").assertIsDisplayed()
    }

    @Test
    fun `a reported edit is against the raw value`() = runComposeUiTest {
        val edits = mutableListOf<CellEdit<Product>>()
        setContent {
            Box(Modifier.size(700.dp, 400.dp)) {
                DataTable(
                    items = products,
                    headers = headers(editable = true),
                    itemKey = { it.id },
                    onCellEdit = { edits += it },
                )
            }
        }

        onNodeWithText("KES 40.00").performClick()
        onRoot().performKeyInput { pressKey(Key.F2) }
        onNode(hasSetTextAction()).performTextReplacement("55")
        onRoot().performKeyInput { pressKey(Key.Enter) }

        assertEquals(1, edits.size)
        assertEquals("40.0", edits.single().oldText)
        assertEquals("55", edits.single().newText)
    }

    @Test
    fun `a copy carries the formatted text`() = runComposeUiTest {
        var copied: ClipboardSelection<Product>? = null
        setContent {
            Box(Modifier.size(700.dp, 400.dp)) {
                DataTable(
                    items = products,
                    headers = headers(),
                    itemKey = { it.id },
                    cellNavigation = true,
                    onCopy = { copied = it },
                )
            }
        }

        onNodeWithText("KES 40.00").performClick()
        onRoot().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.C) } }

        val selection = assertNotNull(copied)
        assertEquals(listOf(listOf("KES 40.00")), selection.cells)
    }
}
