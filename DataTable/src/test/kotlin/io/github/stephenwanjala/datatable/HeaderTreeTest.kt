package io.github.stephenwanjala.datatable

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the header-tree helpers that drive column layout: which headers become columns,
 * which become spanning groups, and which side of the freeze boundary each lands on.
 */
class HeaderTreeTest {

    private fun header(
        key: String,
        width: Int? = null,
        fixed: Boolean = false,
        visible: Boolean = true,
        children: List<DataTableHeader<Row>>? = null,
    ) = DataTableHeader<Row>(
        key = key,
        title = key,
        width = width?.dp,
        fixed = fixed,
        visible = visible,
        children = children,
    )

    data class Row(val id: Int)

    // ---- flattenHeaders -------------------------------------------------------------------

    @Test
    fun `flat headers pass through unchanged`() {
        val headers = listOf(header("a"), header("b"), header("c"))

        assertEquals(listOf("a", "b", "c"), flattenHeaders(headers).map { it.key })
    }

    @Test
    fun `nested headers flatten to their leaves in order`() {
        val headers = listOf(
            header("a"),
            header("group", children = listOf(header("b"), header("c"))),
            header("d"),
        )

        assertEquals(listOf("a", "b", "c", "d"), flattenHeaders(headers).map { it.key })
    }

    @Test
    fun `parents are dropped, only leaves become columns`() {
        val headers = listOf(header("group", children = listOf(header("b"))))

        assertEquals(listOf("b"), flattenHeaders(headers).map { it.key })
    }

    @Test
    fun `deep nesting flattens all the way down`() {
        val headers = listOf(
            header(
                "outer",
                children = listOf(
                    header("inner", children = listOf(header("a"), header("b"))),
                    header("c"),
                ),
            ),
        )

        assertEquals(listOf("a", "b", "c"), flattenHeaders(headers).map { it.key })
    }

    @Test
    fun `invisible headers are excluded`() {
        val headers = listOf(header("a"), header("b", visible = false), header("c"))

        assertEquals(listOf("a", "c"), flattenHeaders(headers).map { it.key })
    }

    @Test
    fun `hiding a group hides its leaves`() {
        val headers = listOf(
            header("a"),
            header("group", visible = false, children = listOf(header("b"), header("c"))),
        )

        assertEquals(listOf("a"), flattenHeaders(headers).map { it.key })
    }

    @Test
    fun `hiding one child leaves its siblings`() {
        val headers = listOf(
            header("group", children = listOf(header("b", visible = false), header("c"))),
        )

        assertEquals(listOf("c"), flattenHeaders(headers).map { it.key })
    }

    // ---- visibleChildren ------------------------------------------------------------------

    @Test
    fun `a leaf reports no children`() {
        assertNull(visibleChildren(header("a")))
    }

    @Test
    fun `a group whose children are all hidden counts as a leaf`() {
        // Otherwise it would render as a group spanning nothing.
        val group = header("group", children = listOf(header("b", visible = false)))

        assertNull(visibleChildren(group))
    }

    @Test
    fun `an empty children list counts as a leaf`() {
        assertNull(visibleChildren(header("group", children = emptyList())))
    }

    // ---- leavesOf -------------------------------------------------------------------------

    @Test
    fun `a leaf is its own only leaf`() {
        val leaf = header("a")

        assertEquals(listOf("a"), leavesOf(leaf).map { it.key })
    }

    @Test
    fun `a group reports every leaf beneath it`() {
        val group = header(
            "outer",
            children = listOf(
                header("inner", children = listOf(header("a"), header("b"))),
                header("c"),
            ),
        )

        assertEquals(listOf("a", "b", "c"), leavesOf(group).map { it.key })
    }

    // ---- partitionHeaderTree --------------------------------------------------------------

    @Test
    fun `columns split by their fixed flag`() {
        val headers = listOf(
            header("a", width = 60, fixed = true),
            header("b"),
            header("c", width = 60, fixed = true),
        )

        val (frozen, scrollable) = partitionHeaderTree(headers)

        assertEquals(listOf("a", "c"), frozen.map { it.key })
        assertEquals(listOf("b"), scrollable.map { it.key })
    }

    @Test
    fun `a group follows its leaves onto the frozen side`() {
        val headers = listOf(
            header(
                "group",
                children = listOf(
                    header("a", width = 60, fixed = true),
                    header("b", width = 60, fixed = true),
                ),
            ),
        )

        val (frozen, scrollable) = partitionHeaderTree(headers)

        assertEquals(listOf("group"), frozen.map { it.key })
        assertTrue(scrollable.isEmpty())
    }

    @Test
    fun `a group straddling the freeze boundary is rejected`() {
        // Half the group would scroll out from under the other half.
        val headers = listOf(
            header(
                "group",
                children = listOf(
                    header("a", width = 60, fixed = true),
                    header("b"),
                ),
            ),
        )

        val error = assertFailsWith<IllegalArgumentException> { partitionHeaderTree(headers) }

        assertTrue(
            error.message!!.contains("group"),
            "the message should name the offending group, was: ${error.message}",
        )
    }

    @Test
    fun `invisible top-level headers are dropped from both sides`() {
        val headers = listOf(
            header("a", width = 60, fixed = true),
            header("hidden", visible = false),
            header("b"),
        )

        val (frozen, scrollable) = partitionHeaderTree(headers)

        assertEquals(listOf("a"), frozen.map { it.key })
        assertEquals(listOf("b"), scrollable.map { it.key })
    }

    @Test
    fun `no frozen columns puts everything on the scrollable side`() {
        val headers = listOf(header("a"), header("b"))

        val (frozen, scrollable) = partitionHeaderTree(headers)

        assertTrue(frozen.isEmpty())
        assertEquals(listOf("a", "b"), scrollable.map { it.key })
    }
}
