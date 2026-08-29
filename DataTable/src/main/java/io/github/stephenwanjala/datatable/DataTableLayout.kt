package io.github.stephenwanjala.datatable

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A snapshot of how a user has arranged a table: column widths, which columns they hid, the order
 * they put them in, the sort they left it under, and the filters they typed.
 *
 * Everything a user changes about a grid is here, and nothing about the data is — a layout is
 * keyed entirely by [DataTableHeader.key], so it survives the rows being replaced, and can be
 * saved against a user rather than against a query.
 *
 * Capture one with [DataTableState.captureLayout] and put it back with
 * [DataTableState.applyLayout]. To store it between sessions, either use [encodeToString], or map
 * these fields onto your own serializable type — they are all plain data, and `Dp` is a `Float`
 * in `dp` behind [Dp.value].
 *
 * A layout is applied by key, so one saved before a column was added, removed, or renamed still
 * applies: entries naming columns that no longer exist are ignored, and columns the layout has
 * never heard of keep their declared width and sit after the ones it names.
 *
 * @property columnWidths Widths the user dragged columns to. A column absent from the map keeps
 *                        the width its header declares.
 * @property hiddenColumns Columns the user hid. This *adds* to `DataTableHeader.visible = false`
 *                         rather than overriding it: a column a caller declared unavailable stays
 *                         unavailable however the layout was saved.
 * @property columnOrder Column keys in the order the user put them. Columns missing from the list
 *                       follow the ones in it, in declaration order — which is where a column
 *                       added since the layout was saved appears.
 * @property sortBy The single-column sort in force.
 * @property multiSortBy The multi-column sort in force, which takes precedence over [sortBy].
 * @property filters Filter queries per column. Worth dropping — `layout.copy(filters =
 *                   emptyMap())` — where a saved layout should restore the shape of a table but
 *                   not narrow what it shows.
 */
@Immutable
data class DataTableLayout(
    val columnWidths: Map<String, Dp> = emptyMap(),
    val hiddenColumns: Set<String> = emptySet(),
    val columnOrder: List<String> = emptyList(),
    val sortBy: SortState = SortState(),
    val multiSortBy: List<SortState> = emptyList(),
    val filters: Map<String, String> = emptyMap(),
) {
    /**
     * Encodes this layout as one line-based string, for a preferences store, a database column,
     * or a file.
     *
     * The format is versioned and its contents are an implementation detail: hand back exactly
     * what you were given, and read it with [decodeFromString]. Every value is escaped, so a
     * filter query holding spaces, newlines, or `=` round-trips unchanged. Encoding is
     * deterministic — the same layout always produces the same string — so it can be compared to
     * decide whether anything is worth saving.
     */
    fun encodeToString(): String = buildString {
        append(FORMAT_VERSION).append('\n')
        columnWidths.toSortedMap().forEach { (key, width) ->
            append("w ").append(escape(key)).append('=').append(width.value).append('\n')
        }
        hiddenColumns.sorted().forEach { key ->
            append("h ").append(escape(key)).append('\n')
        }
        columnOrder.forEach { key ->
            append("o ").append(escape(key)).append('\n')
        }
        if (sortBy.key.isNotEmpty() && sortBy.order != SortOrder.NONE) {
            append("s ").append(escape(sortBy.key)).append(' ').append(sortBy.order.name).append('\n')
        }
        multiSortBy.forEach { sort ->
            append("m ").append(escape(sort.key)).append(' ').append(sort.order.name).append('\n')
        }
        filters.toSortedMap().forEach { (key, query) ->
            append("f ").append(escape(key)).append('=').append(escape(query)).append('\n')
        }
    }

    /** Reads back what [encodeToString] wrote. */
    companion object {
        /** Leads every encoded layout, so a later format can be recognised and declined. */
        private const val FORMAT_VERSION = "dtlayout/1"

        /**
         * Decodes a layout written by [encodeToString], or `null` when [text] is not one.
         *
         * Returning `null` rather than throwing is what makes this safe to point at a stored
         * value of unknown provenance — an empty preference, something another version of the
         * app wrote, or a value that was never a layout at all. Within a layout it recognises,
         * entries it cannot parse are skipped rather than failing the whole restore: one
         * unreadable width is not a reason to lose the other twenty.
         */
        fun decodeFromString(text: String): DataTableLayout? {
            val lines = text.trim().lines()
            if (lines.firstOrNull()?.trim() != FORMAT_VERSION) return null

            val widths = LinkedHashMap<String, Dp>()
            val hidden = LinkedHashSet<String>()
            val order = mutableListOf<String>()
            var sort = SortState()
            val multiSort = mutableListOf<SortState>()
            val filters = LinkedHashMap<String, String>()

            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val tag = line.substringBefore(' ')
                val payload = line.substringAfter(' ', "")
                when (tag) {
                    "w" -> {
                        val width = payload.substringAfterLast('=').toFloatOrNull()
                        val key = unescape(payload.substringBeforeLast('='))
                        if (width != null && key.isNotEmpty()) widths[key] = width.dp
                    }

                    "h" -> unescape(payload).takeIf { it.isNotEmpty() }?.let { hidden += it }

                    "o" -> unescape(payload).takeIf { it.isNotEmpty() }?.let { order += it }

                    "s" -> parseSort(payload)?.let { sort = it }

                    "m" -> parseSort(payload)?.let { multiSort += it }

                    "f" -> {
                        val key = unescape(payload.substringBefore('='))
                        val query = unescape(payload.substringAfter('=', ""))
                        if (key.isNotEmpty()) filters[key] = query
                    }

                    // Anything else was written by a future version that kept this one's format.
                    else -> Unit
                }
            }

            return DataTableLayout(
                columnWidths = widths,
                hiddenColumns = hidden,
                columnOrder = order,
                sortBy = sort,
                multiSortBy = multiSort,
                filters = filters,
            )
        }

        private fun parseSort(payload: String): SortState? {
            val key = unescape(payload.substringBeforeLast(' '))
            val order = SortOrder.entries.firstOrNull { it.name == payload.substringAfterLast(' ') }
            return if (key.isEmpty() || order == null) null else SortState(key, order)
        }
    }
}

/**
 * Percent-escapes the characters the format uses as delimiters, so a column key or a filter query
 * can hold any of them.
 */
private fun escape(value: String): String = buildString(value.length) {
    value.forEach { char ->
        when (char) {
            '%', ' ', '=', '\n', '\r' -> append('%').append("%02X".format(char.code))
            else -> append(char)
        }
    }
}

/** Reverses [escape]. A stray `%` that is not a valid escape is left as it stands. */
private fun unescape(value: String): String {
    if ('%' !in value) return value
    return buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            val code = if (char == '%' && index + 2 < value.length) {
                value.substring(index + 1, index + 3).toIntOrNull(16)
            } else {
                null
            }
            if (code != null) {
                append(code.toChar())
                index += 3
            } else {
                append(char)
                index++
            }
        }
    }
}
