package io.github.stephenwanjala.datatable

import androidx.compose.runtime.Stable

/**
 * Handle given to a column's [DataTableHeader.filterContent], for filters that are not a plain
 * text field.
 *
 * A custom filter renders whatever control it likes — a dropdown of the values that occur, a
 * range of dates, a tri-state flag — and calls [setQuery] with the text that stands for the
 * user's choice. That text is what the table stores in its `filters` map, hands back here on the
 * next composition, and passes to the column's [DataTableHeader.filterPredicate], so a filter
 * whose state does not naturally read as one string encodes it into one — `">1000"`, or
 * `"IT|Finance"` — and its own predicate decodes it again.
 *
 * Keeping the value a `String` is what lets the whole filter set be lifted out of the table,
 * saved with a user's layout, or turned into a `WHERE` clause.
 *
 * @property columnKey [DataTableHeader.key] of the column being filtered.
 * @property query The query text currently set for that column, empty when it is not filtered.
 */
@Stable
class ColumnFilterController internal constructor(
    val columnKey: String,
    val query: String,
    private val onQueryChange: (String) -> Unit,
) {
    /**
     * Sets the column's query, filtering the table.
     *
     * The change is reported straight away, on every keystroke of a text filter. Against a
     * server — `manualFiltering` — debounce it in your own handler.
     */
    fun setQuery(query: String) {
        onQueryChange(query)
    }

    /**
     * Clears the column's filter, which is [setQuery] with an empty string.
     */
    fun clear() {
        onQueryChange("")
    }
}

/**
 * Whether this column draws a filter cell in the filter row.
 *
 * Supplying [DataTableHeader.filterContent] is enough on its own: a column with a custom filter
 * control has plainly opted in, and needing `filterable = true` beside it would only be a trap.
 */
internal fun <T> DataTableHeader<T>.hasFilterField(): Boolean =
    filterable || filterContent != null

/**
 * Whether one item passes one column's filter query.
 *
 * Without a [DataTableHeader.filterPredicate] the match is a case-insensitive "contains" against
 * the text the cell shows *and* against the raw value, so a formatted column can be found either
 * way: a salary reading `$92,000` matches both `92,0` and `92000`.
 */
internal fun <T> DataTableHeader<T>.matchesFilter(item: T, query: String): Boolean {
    val predicate = filterPredicate
    if (predicate != null) return predicate(item, query)

    val needle = query.trim()
    if (needle.isEmpty()) return true
    if (displayText(item).contains(needle, ignoreCase = true)) return true
    val raw = value?.invoke(item) ?: return false
    return raw.toString().contains(needle, ignoreCase = true)
}

/**
 * The columns being filtered, paired with their queries.
 *
 * Resolved against the *visible* leaf columns, so hiding a column stops its filter applying
 * rather than leaving rows missing for a reason the user can no longer see. Blank queries are
 * dropped: a field the user has emptied filters nothing.
 */
internal fun <T> resolveFilters(
    headers: List<DataTableHeader<T>>,
    filters: Map<String, String>,
): List<Pair<DataTableHeader<T>, String>> {
    if (filters.isEmpty()) return emptyList()
    return filters.mapNotNull { (key, query) ->
        if (query.isBlank()) return@mapNotNull null
        headers.firstOrNull { it.key == key }?.let { header -> header to query }
    }
}

/**
 * Keeps the items matching every active filter. Columns are ANDed: a row has to satisfy all of
 * them, which is what a row of filter fields reads as.
 */
internal fun <T> applyFilters(
    items: List<T>,
    active: List<Pair<DataTableHeader<T>, String>>,
): List<T> {
    if (active.isEmpty()) return items
    return items.filter { item -> active.all { (header, query) -> header.matchesFilter(item, query) } }
}
