package io.github.stephenwanjala.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Renders the filter row: one cell per visible leaf column, sized from the same widths the body
 * cells use. A column that is not filterable renders an empty cell rather than being skipped, so
 * the row stays lined up.
 *
 * @param headers The visible leaf columns of this section, in display order.
 * @param filters Query text per column key. A column missing from the map is unfiltered.
 * @param onFilterChange Reports a column's new query, on every keystroke.
 */
@Composable
internal fun <T> DataTableFilterRow(
    headers: List<DataTableHeader<T>>,
    modifier: Modifier = Modifier,
    showSelect: Boolean,
    showExpand: Boolean,
    selectionMode: SelectionMode,
    density: DataTableDensity,
    filters: Map<String, String>,
    onFilterChange: (columnKey: String, query: String) -> Unit,
    colors: DataTableColors,
    textStyles: DataTableTextStyles,
    state: DataTableState,
) {
    Row(
        modifier = modifier
            .background(colors.filterRow)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The leading widths the header row reserves, so column one starts in the same place.
        if (showSelect && selectionMode != SelectionMode.NONE) {
            Spacer(Modifier.width(20.dp + density.horizontalPadding * 2))
        }
        if (showExpand) {
            Spacer(Modifier.width(48.dp))
        }

        headers.forEach { header ->
            val resolvedWidth = state.resolvedColumnWidth(header.key, header.width)
            val sizeModifier =
                if (resolvedWidth != null) Modifier.width(resolvedWidth) else Modifier.weight(1f)
            val query = filters[header.key].orEmpty()

            Box(modifier = sizeModifier.padding(horizontal = 4.dp)) {
                val custom = header.filterContent
                when {
                    custom != null -> {
                        val controller = remember(header.key, query) {
                            ColumnFilterController(header.key, query) { newQuery ->
                                onFilterChange(header.key, newQuery)
                            }
                        }
                        custom(controller)
                    }

                    header.filterable -> DefaultColumnFilterField(
                        query = query,
                        placeholder = header.filterPlaceholder,
                        textStyle = textStyles.filterField,
                        colors = colors,
                        onQueryChange = { newQuery -> onFilterChange(header.key, newQuery) },
                    )
                }
            }
        }
    }
}

/**
 * The built-in column filter: a text field with a placeholder and a clear button. Filtering is
 * live, so there is no "apply" affordance to find; Escape clears the field.
 */
@Composable
internal fun DefaultColumnFilterField(
    query: String,
    placeholder: String,
    textStyle: TextStyle,
    colors: DataTableColors,
    onQueryChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.filterField, shape)
            .border(1.dp, colors.divider, shape)
            .padding(start = 6.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            // Under the field, not inside it: `BasicTextField` has no placeholder of its own.
            if (query.isEmpty() && placeholder.isNotEmpty()) {
                BasicText(
                    text = placeholder,
                    style = textStyle.copy(color = colors.onSurfaceSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        val clears = event.type == KeyEventType.KeyDown &&
                            event.key == Key.Escape &&
                            query.isNotEmpty()
                        if (clears) onQueryChange("")
                        clears
                    },
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(colors.onSurface),
            )
        }

        if (query.isNotEmpty()) {
            SimpleIconButton(
                onClick = { onQueryChange("") },
                size = 18.dp,
                // The field beside it is the tab stop; a second one per column would double the
                // presses needed to cross the row.
                focusable = false,
            ) {
                VectorIcon(
                    imageVector = Close,
                    contentDescription = "Clear filter",
                    modifier = Modifier.size(12.dp),
                    tint = colors.iconTint,
                )
            }
        }
    }
}
