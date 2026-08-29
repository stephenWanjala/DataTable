package io.github.stephenwanjala.datatable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The filter row.
 *
 * Filtering runs ahead of sorting and paging, so most of these tests are about what the rest of
 * the table sees once a filter has narrowed it: the page count, the footer's total, select-all,
 * and which empty state is shown. The filter fields themselves are found by their set-text
 * action, in column order — the filter row is the only place in the table with text fields,
 * unless a cell editor is open.
 */
@OptIn(ExperimentalTestApi::class)
class DataTableFilteringTest {

    data class Product(val id: Int, val name: String, val category: String, val price: Double)

    private val products = listOf(
        Product(1, "Bolt", "Hardware", 40.0),
        Product(2, "Nut", "Hardware", 100.0),
        Product(3, "Washer", "Spares", 5.0),
        Product(4, "Bolt cutter", "Tools", 250.0),
    )

    private val headers = listOf(
        DataTableHeader<Product>(
            key = "name", title = "Name", value = { it.name }, width = 200.dp,
            filterable = true, filterPlaceholder = "Filter name",
        ),
        DataTableHeader<Product>(
            key = "category", title = "Category", value = { it.category }, width = 200.dp,
            filterable = true,
        ),
        DataTableHeader<Product>(key = "price", title = "Price", value = { it.price }, width = 150.dp),
    )

    /** The filter fields, in column order. */
    private fun ComposeUiTest.filterField(index: Int): SemanticsNodeInteraction =
        onAllNodes(hasSetTextAction())[index]

    @Test
    fun `a filterable column draws a field, and its placeholder`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(items = products, headers = headers, itemKey = { it.id })
            }
        }

        onNodeWithText("Filter name").assertIsDisplayed()
        // Two filterable columns, so two fields — the unfilterable price column draws none.
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun `no filterable column means no filter row at all`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = headers.map { it.copy(filterable = false) },
                    itemKey = { it.id },
                )
            }
        }

        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun `typing in a filter narrows the rows`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(items = products, headers = headers, itemKey = { it.id })
            }
        }

        filterField(0).performTextInput("bolt")

        // Case-insensitive contains: "Bolt" and "Bolt cutter", not "Nut".
        onNodeWithText("Bolt").assertIsDisplayed()
        onNodeWithText("Bolt cutter").assertIsDisplayed()
        onNodeWithText("Nut").assertDoesNotExist()
        onNodeWithText("Washer").assertDoesNotExist()
    }

    @Test
    fun `two filters are ANDed`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(items = products, headers = headers, itemKey = { it.id })
            }
        }

        filterField(0).performTextInput("bolt")
        filterField(1).performTextInput("tools")

        onNodeWithText("Bolt cutter").assertIsDisplayed()
        onNodeWithText("Bolt").assertDoesNotExist()
    }

    @Test
    fun `the default match reads the formatted text and the raw value alike`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = listOf(
                        DataTableHeader<Product>(key = "name", title = "Name", value = { it.name }, width = 200.dp),
                        DataTableHeader<Product>(
                            key = "price", title = "Price", value = { it.price }, width = 200.dp,
                            format = { "KES ${"%,.2f".format(it)}" },
                            filterable = true,
                        ),
                    ),
                    itemKey = { it.id },
                )
            }
        }

        // "250" appears in the raw value (250.0) and in the formatted text ("KES 250.00").
        filterField(0).performTextInput("250")
        onNodeWithText("Bolt cutter").assertIsDisplayed()
        onNodeWithText("Nut").assertDoesNotExist()

        // The grouping separator only exists in the formatted text.
        filterField(0).performTextReplacement("KES 5")
        onNodeWithText("Washer").assertIsDisplayed()
        onNodeWithText("Bolt cutter").assertDoesNotExist()
    }

    @Test
    fun `a column predicate replaces the default match`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = listOf(
                        DataTableHeader<Product>(key = "name", title = "Name", value = { it.name }, width = 200.dp),
                        DataTableHeader<Product>(
                            key = "price", title = "Price", value = { it.price }, width = 200.dp,
                            filterable = true,
                            // "over 99" rather than "contains 99".
                            filterPredicate = { product, query ->
                                query.toDoubleOrNull()?.let { product.price > it } ?: true
                            },
                        ),
                    ),
                    itemKey = { it.id },
                )
            }
        }

        filterField(0).performTextInput("99")

        onNodeWithText("Nut").assertIsDisplayed()
        onNodeWithText("Bolt cutter").assertIsDisplayed()
        onNodeWithText("Washer").assertDoesNotExist()
    }

    @Test
    fun `escape clears a filter`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(items = products, headers = headers, itemKey = { it.id })
            }
        }

        filterField(0).performTextInput("washer")
        onNodeWithText("Bolt").assertDoesNotExist()

        filterField(0).performKeyInput { pressKey(Key.Escape) }

        onNodeWithText("Bolt").assertIsDisplayed()
        onNodeWithText("Washer").assertIsDisplayed()
    }

    @Test
    fun `the clear button empties a filter`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(items = products, headers = headers, itemKey = { it.id })
            }
        }

        filterField(0).performTextInput("washer")
        onNodeWithText("Bolt").assertDoesNotExist()

        // The × only exists once something is typed.
        onNodeWithContentDescription("Clear filter").performClick()

        onNodeWithText("Bolt").assertIsDisplayed()
    }

    @Test
    fun `supplying onFiltersChange makes filtering controlled`() = runComposeUiTest {
        val reported = mutableListOf<Map<String, String>>()
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = headers,
                    itemKey = { it.id },
                    filters = emptyMap(),
                    onFiltersChange = { reported += it },
                )
            }
        }

        filterField(0).performTextInput("b")

        // Reported, but the table renders the filters it was given — which are still empty.
        assertEquals(listOf(mapOf("name" to "b")), reported)
        onNodeWithText("Washer").assertIsDisplayed()
    }

    @Test
    fun `a controlled filter is rendered and applied`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = headers,
                    itemKey = { it.id },
                    filters = mapOf("category" to "Spares"),
                    onFiltersChange = {},
                )
            }
        }

        onNodeWithText("Washer").assertIsDisplayed()
        onNodeWithText("Bolt").assertDoesNotExist()
    }

    @Test
    fun `manualFiltering leaves the items alone but still reports`() = runComposeUiTest {
        val reported = mutableListOf<Map<String, String>>()
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = headers,
                    itemKey = { it.id },
                    manualFiltering = true,
                    onFiltersChange = { reported += it },
                    filters = mapOf("name" to "washer"),
                )
            }
        }

        // The caller's query would have done the filtering; the table must not do it again.
        onNodeWithText("Bolt").assertIsDisplayed()
        onNodeWithText("Washer").assertIsDisplayed()

        filterField(0).performTextReplacement("nut")
        assertTrue(reported.isNotEmpty())
    }

    @Test
    fun `filtering goes back to the first page`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products, headers = headers, itemKey = { it.id },
                    showPagination = true, itemsPerPage = 2,
                )
            }
        }

        // Page two of two: Washer and Bolt cutter.
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithText("Washer").assertIsDisplayed()

        // Filtering to a single match would leave page two empty if the page did not reset.
        filterField(0).performTextInput("nut")
        onNodeWithText("Nut").assertIsDisplayed()
    }

    @Test
    fun `the page count follows the filtered total`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products, headers = headers, itemKey = { it.id },
                    showPagination = true, itemsPerPage = 2,
                )
            }
        }

        onNodeWithText("1–2 of 4").assertIsDisplayed()

        filterField(1).performTextInput("hardware")

        onNodeWithText("1–2 of 2").assertIsDisplayed()
    }

    @Test
    fun `a filter matching nothing says so, rather than showing the empty state`() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(800.dp, 500.dp)) {
                    DataTable(items = products, headers = headers, itemKey = { it.id })
                }
            }

            filterField(0).performTextInput("zzz")

            onNodeWithText("No rows match the filter").assertIsDisplayed()
            onNodeWithText("No data available").assertDoesNotExist()
        }

    @Test
    fun `an empty table still says it has no data`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(items = emptyList<Product>(), headers = headers, itemKey = { it.id })
            }
        }

        onNodeWithText("No data available").assertIsDisplayed()
    }

    @Test
    fun `hiding a column stops its filter applying`() = runComposeUiTest {
        setContent {
            var hidden by remember { mutableStateOf(false) }
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = headers.map {
                        if (it.key == "category") it.copy(visible = !hidden) else it
                    },
                    itemKey = { it.id },
                    filters = mapOf("category" to "Spares"),
                    onFiltersChange = {},
                    // Hiding the filtered column mid-test, from a button the table draws for us.
                    footerContent = {
                        BasicText(
                            text = "Hide category",
                            modifier = Modifier.clickableForTest { hidden = true },
                        )
                    },
                )
            }
        }

        onNodeWithText("Bolt").assertDoesNotExist()

        onNodeWithText("Hide category").performClick()

        // The filter is still in the map, but its column is gone: leaving rows missing for a
        // reason the user can no longer see would be worse than dropping it.
        onNodeWithText("Bolt").assertIsDisplayed()
    }

    @Test
    fun `select-all covers the filtered rows only`() = runComposeUiTest {
        var selected: Set<Any> = emptySet()
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = headers,
                    itemKey = { it.id },
                    showSelect = true,
                    selectedKeys = selected,
                    onSelectionChange = { selected = it },
                )
            }
        }

        filterField(1).performTextInput("hardware")
        // The select-all checkbox: the header composes ahead of the filter row, so it is the
        // first click target in the table even once a filter has drawn its clear button.
        onAllNodes(hasClickAction())[0].performClick()

        assertEquals(setOf(1, 2), selected)
    }

    @Test
    fun `typing in a filter does not open a cell editor`() = runComposeUiTest {
        val edits = mutableListOf<CellEdit<Product>>()
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = headers.map {
                        if (it.key == "name") it.copy(editable = true) else it
                    },
                    itemKey = { it.id },
                    onCellEdit = { edits += it },
                )
            }
        }

        // With an editable column the table binds printable keys to "start editing". A focused
        // filter has to take precedence, or the filter row is unusable in an editable table.
        filterField(0).performTextInput("nut")

        onNodeWithText("Nut").assertIsDisplayed()
        onNodeWithText("Bolt").assertDoesNotExist()
        assertEquals(0, edits.size)
    }

    @Test
    fun `clicking a filter field takes the keyboard from the table`() = runComposeUiTest {
        lateinit var state: DataTableState
        setContent {
            state = rememberDataTableState()
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products, headers = headers, itemKey = { it.id },
                    state = state, cellNavigation = true,
                )
            }
        }

        // The container grabs focus on any press inside the table, on the initial pointer pass.
        // The field has to win that race, or it can be typed into only after a Tab.
        filterField(0).performClick()
        onRoot().performKeyInput { pressKey(Key.DirectionDown) }

        // The arrow key went to the field's caret, not to the table's row cursor.
        assertEquals(null, state.focusedKey)
    }

    @Test
    fun `a custom filter control drives the same filtering`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                DataTable(
                    items = products,
                    headers = listOf(
                        DataTableHeader<Product>(key = "name", title = "Name", value = { it.name }, width = 250.dp),
                        DataTableHeader<Product>(
                            key = "category", title = "Category", value = { it.category }, width = 250.dp,
                            // No `filterable` flag: supplying the control is opting in.
                            filterContent = { controller ->
                                BasicText(
                                    text = "Only tools",
                                    modifier = Modifier.clickableForTest {
                                        controller.setQuery("Tools")
                                    },
                                )
                            },
                        ),
                    ),
                    itemKey = { it.id },
                )
            }
        }

        onNodeWithText("Only tools").performClick()

        onNodeWithText("Bolt cutter").assertIsDisplayed()
        onNodeWithText("Washer").assertDoesNotExist()
    }
}

/** A plain click target, for the stand-in controls the tests above drive the table with. */
private fun Modifier.clickableForTest(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
