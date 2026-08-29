package io.github.stephenwanjala.composedatatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlin.test.Test

/**
 * Every sample in the gallery renders rows.
 *
 * The library's own tests drive the table directly, so they cannot see a sample that composes
 * cleanly and shows nothing — which is exactly the failure this catches.
 */
@OptIn(ExperimentalTestApi::class)
class SampleGalleryTest {

    /**
     * @param row Text from the first data row, asserted to be **displayed** rather than merely
     *            present: a table whose header has swallowed the body still composes its rows,
     *            it just gives them nowhere to be.
     */
    private fun assertShowsRows(row: String, sample: @Composable () -> Unit) = runComposeUiTest {
        setContent {
            Box(Modifier.size(1200.dp, 800.dp)) { sample() }
        }

        onNodeWithText("No data available").assertDoesNotExist()
        onNodeWithText("No rows match the filter").assertDoesNotExist()
        onNodeWithText(row).assertIsDisplayed()
    }

    @Test
    fun `large dataset sample shows rows`() =
        // Same seed the sample uses, so this is its first row.
        assertShowsRows(LargeDataSetItem.generateRandom(1, Random(42)).fullName) { LargeDataSetSample() }

    @Test
    fun `nested headers sample shows rows`() = assertShowsRows("Alice Smith") { NestedHeadersSample() }

    @Test
    fun `selection sample shows rows`() = assertShowsRows("Alice Smith") { SelectionSample() }

    @Test
    fun `expansion sample shows rows`() = assertShowsRows("Alice Smith") { ExpansionSample() }

    @Test
    fun `grouping sample shows rows`() = assertShowsRows("Alice Smith") { GroupingSample() }

    @Test
    fun `filtering sample shows rows`() = assertShowsRows("Alice Smith") { FilteringSample() }

    @Test
    fun `layout sample shows rows`() = assertShowsRows("Alice Smith") { LayoutSample() }

    @Test
    fun `keyboard sample shows rows`() = assertShowsRows("Alice Smith") { KeyboardSample() }

    @Test
    fun `cell editing sample shows rows`() = assertShowsRows("Alice Smith") { CellEditingSample() }

    @Test
    fun `range selection sample shows rows`() = assertShowsRows("Alice Smith") { RangeSelectionSample() }

    @Test
    fun `theming sample shows rows`() = assertShowsRows("Alice Smith") { ThemingSample() }
}
