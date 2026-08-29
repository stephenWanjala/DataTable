package io.github.stephenwanjala.composedatatable

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.stephenwanjala.datatable.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

/** Strip of controls above a sample's table. */
@Composable
private fun SampleControls(content: @Composable RowScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Built once, not per recomposition: a formatter holds its locale and number format. */
private val money = DataTableFormatters.currency(locale = Locale.US, decimals = 0)

private val employeeHeaders = listOf(
    DataTableHeader<Employee>(key = "id", title = "ID", value = { it.id }, width = 60.dp, align = TextAlign.End),
    DataTableHeader(key = "name", title = "Name", value = { it.name }, width = 180.dp),
    DataTableHeader(key = "email", title = "Email", value = { it.email }, width = 240.dp, maxLines = 1, overflow = TextOverflow.Ellipsis),
    DataTableHeader(key = "department", title = "Department", value = { it.department }, width = 140.dp),
    DataTableHeader(key = "role", title = "Role", value = { it.role }, width = 140.dp),
    DataTableHeader(
        key = "salary", title = "Salary", width = 120.dp, align = TextAlign.End,
        // A Double, so the column sorts numerically with no comparator; only `format` knows
        // about currency.
        value = { it.salary }, format = money,
    ),
)

// ---------------------------------------------------------------------------
// 1. Large dataset
// ---------------------------------------------------------------------------

@Composable
fun LargeDataSetSample() {
    val random = remember { Random(42) }
    val items = remember { (1..500).map { LargeDataSetItem.generateRandom(it, random) } }

    var selectedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }
    var currentPage by remember { mutableStateOf(0) }
    var itemsPerPage by remember { mutableStateOf(20) }
    var multiSort by remember { mutableStateOf<List<SortState>>(emptyList()) }

    val tableState = rememberDataTableState()

    val headers = remember {
        listOf(
            DataTableHeader<LargeDataSetItem>(
                key = "id", title = "ID", value = { it.id },
                width = 60.dp, align = TextAlign.Center, fixed = true,
            ),
            DataTableHeader(
                key = "fullName", title = "Full Name", value = { it.fullName },
                width = 150.dp, fixed = true, maxLines = 1, overflow = TextOverflow.Ellipsis,
            ),
            // Nested headers, placed early so the bands are visible without scrolling right.
            DataTableHeader(
                key = "location",
                title = "Location",
                children = listOf(
                    DataTableHeader(
                        key = "street",
                        title = "Street",
                        children = listOf(
                            DataTableHeader(key = "zipCode", title = "ZIP", value = { it.zipCode }, width = 80.dp),
                            DataTableHeader(
                                key = "streetAddress", title = "Address", value = { it.streetAddress },
                                width = 200.dp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            ),
                        ),
                    ),
                    DataTableHeader(
                        key = "room",
                        title = "Room",
                        children = listOf(
                            DataTableHeader(key = "buildingNumber", title = "Building", value = { it.buildingNumber }, width = 80.dp),
                            DataTableHeader(key = "floorNumber", title = "Floor", value = { it.floorNumber }, width = 60.dp, align = TextAlign.End),
                            DataTableHeader(key = "officeNumber", title = "Office", value = { it.officeNumber }, width = 80.dp, align = TextAlign.End),
                        ),
                    ),
                ),
            ),
            DataTableHeader(key = "age", title = "Age", value = { it.age }, width = 60.dp, align = TextAlign.End),
            DataTableHeader(key = "email", title = "Email", value = { it.email }, width = 250.dp, maxLines = 1, overflow = TextOverflow.Ellipsis),
            DataTableHeader(key = "city", title = "City", value = { it.city }, width = 120.dp),
            DataTableHeader(key = "country", title = "Country", value = { it.country }, width = 100.dp),
            DataTableHeader(key = "occupation", title = "Occupation", value = { it.occupation }, width = 120.dp),
            DataTableHeader(
                key = "salary", title = "Salary", value = { it.salary },
                width = 120.dp, align = TextAlign.End,
                format = DataTableFormatters.currency(locale = Locale.US),
            ),
            DataTableHeader(key = "department", title = "Department", value = { it.department }, width = 120.dp),
            DataTableHeader(key = "startDate", title = "Start Date", value = { it.startDate }, width = 120.dp),
            DataTableHeader(
                key = "projectStatus", title = "Project Status", value = { it.projectStatus }, width = 150.dp,
                cellContent = { item ->
                    val color = when (item.projectStatus) {
                        "Completed" -> MaterialTheme.colorScheme.primary
                        "In Progress" -> MaterialTheme.colorScheme.tertiary
                        "Pending" -> MaterialTheme.colorScheme.secondary
                        "On Hold" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text(item.projectStatus, style = MaterialTheme.typography.bodySmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = color.copy(alpha = 0.2f),
                            labelColor = color,
                        ),
                    )
                },
            ),
            DataTableHeader(key = "hoursWorked", title = "Hours", value = { it.hoursWorked }, width = 80.dp, align = TextAlign.End),
            DataTableHeader(
                key = "isActive", title = "Active", value = { it.isActive }, width = 80.dp, align = TextAlign.Center,
                // The cell is an icon, so this only shows up in a copy: "Active", not "true".
                format = DataTableFormatters.boolean(trueText = "Active", falseText = "Inactive"),
                cellContent = { item ->
                    Icon(
                        imageVector = if (item.isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = if (item.isActive) "Active" else "Inactive",
                        tint = if (item.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                },
            ),
            DataTableHeader(key = "rating", title = "Rating", value = { it.rating }, width = 80.dp, align = TextAlign.End),
            DataTableHeader(key = "phoneNumber", title = "Phone", value = { it.phoneNumber }, width = 150.dp),
            DataTableHeader(key = "managerName", title = "Manager", value = { it.managerName }, width = 150.dp),
            DataTableHeader(key = "teamLead", title = "Team Lead", value = { it.teamLead }, width = 150.dp),
            DataTableHeader(key = "reviewScore", title = "Review", value = { it.reviewScore }, width = 80.dp, align = TextAlign.End),
            DataTableHeader(
                key = "notes", title = "Notes", value = { it.notes }, width = 300.dp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            ),
        )
    }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text("${items.size} rows · ${selectedKeys.size} selected", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { tableState.resetColumnWidths() }) { Text("Reset Widths") }
            if (selectedKeys.isNotEmpty()) {
                Button(onClick = { selectedKeys = emptySet() }) { Text("Clear Selection") }
            }
        }

        DataTable(
            items = items,
            headers = headers,
            itemKey = { it.id },
            state = tableState,
            showSelect = true,
            selectionMode = SelectionMode.MULTI,
            selectedKeys = selectedKeys,
            onSelectionChange = { selectedKeys = it },
            multiSortBy = multiSort,
            onMultiSortChange = { multiSort = it },
            resizableColumns = true,
            reorderableColumns = true,
            minColumnWidth = 50.dp,
            showPagination = true,
            itemsPerPage = itemsPerPage,
            currentPage = currentPage,
            onPageChange = { currentPage = it },
            onItemsPerPageChange = { itemsPerPage = it; currentPage = 0 },
            colors = DataTableDefaults.colors(rowAlternate = Color(0xFFF5F5F5)),
            onRowContextMenu = { item, offset -> println("Right-clicked ${item.fullName} at $offset") },
            modifier = Modifier.weight(1f),
            showColumnMenuButton=true,
        )
    }
}

// ---------------------------------------------------------------------------
// 2. Nested headers, on their own
// ---------------------------------------------------------------------------

@Composable
fun NestedHeadersSample() {
    val headers = remember {
        listOf(
            DataTableHeader<Employee>(key = "id", title = "ID", value = { it.id }, width = 60.dp, align = TextAlign.End),
            DataTableHeader(key = "name", title = "Name", value = { it.name }, width = 180.dp),
            DataTableHeader(
                key = "contact",
                title = "Contact",
                children = listOf(
                    DataTableHeader(key = "email", title = "Email", value = { it.email }, width = 240.dp, maxLines = 1, overflow = TextOverflow.Ellipsis),
                    DataTableHeader(key = "phone", title = "Phone", value = { it.phone }, width = 150.dp),
                ),
            ),
            DataTableHeader(
                key = "employment",
                title = "Employment",
                children = listOf(
                    DataTableHeader(
                        key = "placement",
                        title = "Placement",
                        children = listOf(
                            DataTableHeader(key = "department", title = "Department", value = { it.department }, width = 140.dp),
                            DataTableHeader(key = "role", title = "Role", value = { it.role }, width = 140.dp),
                        ),
                    ),
                    DataTableHeader(key = "salary", title = "Salary", value = { it.salary }, format = money, width = 120.dp, align = TextAlign.End),
                ),
            ),
        )
    }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text(
                "Three levels deep. ID and Name are leaves at the top level, so they reserve " +
                    "width in the bands and keep their titles in the leaf row. Drag a band to " +
                    "move a whole group; drag a column inside one and it stays in it.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        DataTable(
            items = sampleEmployees,
            headers = headers,
            itemKey = { it.id },
            reorderableColumns = true,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 3. Selection modes
// ---------------------------------------------------------------------------

@Composable
fun SelectionSample() {
    var mode by remember { mutableStateOf(SelectionMode.MULTI) }
    var selectedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text("Mode:", style = MaterialTheme.typography.bodyMedium)
            SelectionMode.entries.forEach { candidate ->
                FilterChip(
                    selected = mode == candidate,
                    onClick = { mode = candidate; selectedKeys = emptySet() },
                    label = { Text(candidate.name) },
                )
            }
            Spacer(Modifier.weight(1f))
            Text("Selected: ${selectedKeys.size}", style = MaterialTheme.typography.bodyMedium)
        }
        DataTable(
            items = sampleEmployees,
            headers = employeeHeaders,
            itemKey = { it.id },
            showSelect = mode != SelectionMode.NONE,
            selectionMode = mode,
            selectedKeys = selectedKeys,
            onSelectionChange = { selectedKeys = it },
            onRowClick = { println("Clicked ${it.name}") },
            reorderableColumns = true,
            colors = DataTableDefaults.colors(rowAlternate = Color(0xFFF7F7F7)),
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 4. Row expansion
// ---------------------------------------------------------------------------

@Composable
fun ExpansionSample() {
    var expandedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text("Expanded: ${expandedKeys.size}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { expandedKeys = sampleEmployees.map { it.id }.toSet() }) { Text("Expand All") }
            OutlinedButton(onClick = { expandedKeys = emptySet() }) { Text("Collapse All") }
        }
        DataTable(
            items = sampleEmployees,
            headers = employeeHeaders,
            itemKey = { it.id },
            showExpand = true,
            expandedKeys = expandedKeys,
            onExpandChange = { expandedKeys = it },
            expandContent = { employee ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(employee.name, style = MaterialTheme.typography.titleSmall)
                    Text("Phone: ${employee.phone}", style = MaterialTheme.typography.bodySmall)
                    Text("Status: ${if (employee.active) "Active" else "Inactive"}", style = MaterialTheme.typography.bodySmall)
                }
            },
            reorderableColumns = true,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 5. Grouping
// ---------------------------------------------------------------------------

@Composable
fun GroupingSample() {
    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text("Grouped by department, with a per-group summary row.", style = MaterialTheme.typography.bodySmall)
        }
        DataTable(
            items = sampleEmployees,
            headers = employeeHeaders,
            itemKey = { it.id },
            groupBy = { it.department },
            groupHeaderContent = { department, rows ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("$department (${rows.size})", fontWeight = FontWeight.Bold)
                    }
                }
            },
            groupSummaryContent = { _, rows ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        "Total salary: $${"%,.0f".format(rows.sumOf { it.salary })}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            reorderableColumns = true,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 5b. Filtering
// ---------------------------------------------------------------------------

/** A picker for a column whose values come from a short, known list. */
@Composable
private fun ChoiceFilter(
    options: List<String>,
    controller: ColumnFilterController,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(30.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(
                controller.query.ifEmpty { "All" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = { expanded = false; controller.clear() },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { expanded = false; controller.setQuery(option) },
                )
            }
        }
    }
}

@Composable
fun FilteringSample() {
    // Hoisted, so the strip above the table can show what is filtered and clear it in one go.
    var filters by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val departments = remember { sampleEmployees.map { it.department }.distinct().sorted() }

    val headers = remember(departments) {
        listOf(
            DataTableHeader<Employee>(
                key = "name", title = "Name", value = { it.name }, width = 180.dp,
                filterable = true, filterPlaceholder = "Contains…",
            ),
            DataTableHeader(
                key = "email", title = "Email", value = { it.email }, width = 240.dp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                filterable = true, filterPlaceholder = "Contains…",
            ),
            DataTableHeader(
                key = "department", title = "Department", value = { it.department }, width = 170.dp,
                // Supplying the control is opting in — no `filterable = true` beside it.
                filterContent = { controller -> ChoiceFilter(departments, controller) },
                filterPredicate = { employee, query -> employee.department == query },
            ),
            DataTableHeader(
                key = "salary", title = "Salary", value = { it.salary }, format = money,
                width = 150.dp, align = TextAlign.End,
                filterable = true, filterPlaceholder = "Min",
                // The query is a floor, not a substring.
                filterPredicate = { employee, query ->
                    query.toDoubleOrNull()?.let { employee.salary >= it } ?: true
                },
            ),
            DataTableHeader(
                key = "active", title = "Active", value = { it.active }, width = 110.dp,
                align = TextAlign.Center,
                format = DataTableFormatters.boolean(),
                // Matching reads the formatted text too, so "yes" finds what the column shows.
                filterable = true, filterPlaceholder = "Yes/No",
            ),
        )
    }

    val active = filters.filterValues { it.isNotBlank() }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text(
                "Type in the filter row. Department is a dropdown, salary is a minimum, and " +
                    "Esc clears a field.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (active.isEmpty()) "No filters" else active.entries.joinToString(" · ") {
                    "${it.key}: ${it.value}"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = { filters = emptyMap() },
                enabled = active.isNotEmpty(),
            ) {
                Text("Clear all")
            }
        }

        DataTable(
            items = sampleEmployees,
            headers = headers,
            itemKey = { it.id },
            filters = filters,
            onFiltersChange = { filters = it },
            reorderableColumns = true,
            density = DataTableDensity.COMFORTABLE,
            showPagination = true,
            itemsPerPage = 10,
            colors = DataTableDefaults.colors(rowAlternate = Color(0xFFF7F7F7)),
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 5c. Saving a column layout
// ---------------------------------------------------------------------------

@Composable
fun LayoutSample() {
    val tableState = rememberDataTableState()

    // Stands in for a preferences store.
    var saved by remember { mutableStateOf<String?>(null) }

    val headers = remember {
        employeeHeaders.map { header ->
            if (header.key == "name") header.copy(filterable = true, filterPlaceholder = "Contains…")
            else header
        }
    }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text(
                "Drag a header to move the column. Hide columns from the table's own menu at " +
                    "the right of the header, resize, sort and filter too — then save, wreck " +
                    "it, and restore.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.weight(1f))

            OutlinedButton(onClick = { tableState.moveColumn("salary", 0) }) {
                Text("Salary first")
            }

            OutlinedButton(onClick = { saved = tableState.captureLayout().encodeToString() }) {
                Text("Save")
            }
            OutlinedButton(
                onClick = {
                    saved?.let { text ->
                        DataTableLayout.decodeFromString(text)?.let(tableState::applyLayout)
                    }
                },
                enabled = saved != null,
            ) {
                Text("Restore")
            }
            OutlinedButton(onClick = { tableState.resetLayout() }) { Text("Reset") }
        }

        DataTable(
            items = sampleEmployees,
            headers = headers,
            itemKey = { it.id },
            state = tableState,
            resizableColumns = true,
            reorderableColumns = true,
            showColumnMenuButton = true,
            density = DataTableDensity.COMFORTABLE,
            colors = DataTableDefaults.colors(rowAlternate = Color(0xFFF7F7F7)),
            modifier = Modifier.weight(1f),
        )

        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Saved layout", style = MaterialTheme.typography.labelMedium)
                Text(
                    saved?.replace("\n", " · ") ?: "Nothing saved yet.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 6. Server-side sorting and pagination
// ---------------------------------------------------------------------------

@Composable
fun ServerSideSample() {
    var page by remember { mutableStateOf(0) }
    var pageSize by remember { mutableStateOf(25) }
    var sort by remember { mutableStateOf(SortState("id", SortOrder.ASCENDING)) }
    var filters by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var rows by remember { mutableStateOf<List<Employee>?>(null) }
    var matching by remember { mutableStateOf(EmployeeRepository.total) }
    var queries by remember { mutableStateOf(0) }

    // Declared here rather than on the shared `employeeHeaders` every other sample uses.
    val headers = remember {
        val filtered = mapOf(
            "name" to "Contains…",
            "department" to "Exact",
            "role" to "Contains…",
        )
        employeeHeaders.map { header ->
            filtered[header.key]?.let { hint ->
                header.copy(filterable = true, filterPlaceholder = hint)
            } ?: header
        }
    }

    // Stands in for hitting a database. Restarting on each keystroke is also the debounce: the
    // delay begins again, so a burst of typing costs one query rather than one per character.
    LaunchedEffect(page, pageSize, sort, filters) {
        rows = null
        delay(350.milliseconds)
        matching = EmployeeRepository.count(filters)
        rows = EmployeeRepository.page(
            offset = page * pageSize,
            limit = pageSize,
            sortKey = sort.key,
            ascending = sort.order != SortOrder.DESCENDING,
            filters = filters,
        )
        queries++
    }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text(
                "$matching of ${EmployeeRepository.total} rows match, ${rows?.size ?: 0} in memory · $queries queries",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.weight(1f))
            val where = filters.entries
                .filter { it.value.isNotBlank() }
                .joinToString(" AND ") { "${it.key} ~ '${it.value}'" }
            Text(
                (if (where.isEmpty()) "" else "WHERE $where · ") +
                    "ORDER BY ${sort.key} ${if (sort.order == SortOrder.DESCENDING) "DESC" else "ASC"}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        DataTable(
            items = rows.orEmpty(),
            headers = headers,
            itemKey = { it.id },
            loading = rows == null,

            manualSorting = true,
            sortBy = sort,
            onSortChange = { sort = it; page = 0 },

            // The repository has already applied these; the table must not filter again.
            manualFiltering = true,
            filters = filters,
            onFiltersChange = { filters = it; page = 0 },

            showPagination = true,
            manualPagination = true,
            totalItems = matching,
            itemsPerPage = pageSize,
            currentPage = page,
            onPageChange = { page = it },
            onItemsPerPageChange = { pageSize = it; page = 0 },

            reorderableColumns = true,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 7. Density and theming
// ---------------------------------------------------------------------------

@Composable
fun ThemingSample() {
    var density by remember { mutableStateOf(DataTableDensity.DEFAULT) }
    var dark by remember { mutableStateOf(false) }

    val colors = if (dark) {
        DataTableDefaults.colors(
            container = Color(0xFF1E1E1E),
            header = Color(0xFF2A2A2A),
            divider = Color(0xFF3A3A3A),
            selectedRow = Color(0x664F8CC9),
            hoveredRow = Color(0x14FFFFFF),
            rowAlternate = Color(0xFF242424),
            iconTint = Color(0xFFBBBBBB),
            disabledContent = Color(0xFF5A5A5A),
            checkboxChecked = Color(0xFF4F8CC9),
            checkboxUnchecked = Color(0xFF888888),
            focusedRowBorder = Color(0xFF4F8CC9),
            draggedColumn = Color(0x334F8CC9),
            columnDropIndicator = Color(0xFF4F8CC9),
        )
    } else {
        DataTableDefaults.colors(rowAlternate = Color(0xFFF5F5F5))
    }

    val textStyles = if (dark) {
        DataTableDefaults.textStyles(
            headerCell = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFEDEDED)),
            bodyCell = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFDDDDDD)),
            footer = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBBBBBB)),
            pagination = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDDDDDD)),
            noData = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF999999)),
            loading = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFDDDDDD)),
        )
    } else {
        DataTableDefaults.textStyles()
    }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text("Density:", style = MaterialTheme.typography.bodyMedium)
            DataTableDensity.entries.forEach { candidate ->
                FilterChip(
                    selected = density == candidate,
                    onClick = { density = candidate },
                    label = { Text(candidate.name) },
                )
            }
            Spacer(Modifier.weight(1f))
            Text("Dark palette", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = dark, onCheckedChange = { dark = it })
        }
        DataTable(
            items = sampleEmployees,
            headers = employeeHeaders,
            itemKey = { it.id },
            density = density,
            colors = colors,
            textStyles = textStyles,
            showSelect = true,
            showPagination = true,
            itemsPerPage = 10,
            onItemsPerPageChange = {},
            reorderableColumns = true,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 8. Loading state
// ---------------------------------------------------------------------------

@Composable
fun LoadingSample() {
    var isLoading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var useCustomContent by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(2000)
            items = sampleEmployees
            isLoading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text("Custom loading content", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = useCustomContent, onCheckedChange = { useCustomContent = it })
            Spacer(Modifier.weight(1f))
            Button(onClick = { items = emptyList(); isLoading = true }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reload")
            }
        }
        DataTable(
            items = items,
            headers = employeeHeaders,
            itemKey = { it.id },
            loading = isLoading,
            loadingContent = if (useCustomContent) {
                {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator()
                            Text("Fetching employees…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else null,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 9. Empty state
// ---------------------------------------------------------------------------

@Composable
fun EmptyStateSample() {
    var useCustomContent by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text("Custom empty content", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = useCustomContent, onCheckedChange = { useCustomContent = it })
        }
        DataTable(
            items = emptyList<Employee>(),
            headers = employeeHeaders,
            itemKey = { it.id },
            noDataContent = if (useCustomContent) {
                {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                            Text("No data to display", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Add some items to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Button(onClick = {}) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Item")
                            }
                        }
                    }
                }
            } else null,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 10. Keyboard navigation
// ---------------------------------------------------------------------------

@Composable
fun KeyboardSample() {
    val tableState = rememberDataTableState()
    var selectedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text(
                "Click the table, then use ↑ ↓ Home End, Enter, Space. Focus is tracked by key, " +
                    "so it follows its row when you re-sort.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.weight(1f))
            Text("Focused key: ${tableState.focusedKey ?: "none"}", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { tableState.focusRow(sampleEmployees.first().id) }) { Text("Focus First") }
            OutlinedButton(onClick = { tableState.focusRow(null) }) { Text("Clear") }
        }
        DataTable(
            items = sampleEmployees,
            headers = employeeHeaders,
            itemKey = { it.id },
            state = tableState,
            showSelect = true,
            selectedKeys = selectedKeys,
            onSelectionChange = { selectedKeys = it },
            onRowClick = { println("Activated ${it.name}") },
            reorderableColumns = true,
            colors = DataTableDefaults.colors(
                rowAlternate = Color(0xFFF7F7F7),
                focusedRowBorder = Color(0xFFD32F2F),
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// 11. Cell editing
// ---------------------------------------------------------------------------

@Composable
fun CellEditingSample() {
    val tableState = rememberDataTableState()
    // The table reports edits; this list is the source of truth that applies them.
    var employees by remember { mutableStateOf(sampleEmployees.take(12)) }
    var log by remember { mutableStateOf<List<String>>(emptyList()) }

    val departments = listOf("Finance", "IT", "Operations", "Sales")

    val headers = remember {
        listOf(
            DataTableHeader<Employee>(
                key = "name", title = "Name", value = { it.name }, width = 180.dp, fixed = true,
            ),
            DataTableHeader(
                key = "role", title = "Role", value = { it.role }, width = 160.dp,
                // Nothing but `editable`: the built-in text field handles the rest.
                editable = true,
            ),
            DataTableHeader(
                key = "department", title = "Department", value = { it.department }, width = 170.dp,
                editable = true,
                // A column whose values come from a fixed set deserves a picker, not free text.
                editorContent = { employee, controller ->
                    var expanded by remember { mutableStateOf(true) }
                    Box {
                        Text(employee.department, style = MaterialTheme.typography.bodyMedium)
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false; controller.cancel() },
                        ) {
                            departments.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { expanded = false; controller.commit(option) },
                                )
                            }
                        }
                    }
                },
            ),
            DataTableHeader(
                key = "salary", title = "Salary", width = 140.dp, align = TextAlign.End,
                value = { it.salary }, format = money,
                editable = true,
                // The raw value would open the editor onto "92000.0"; a salary is whole units.
                editValue = { it.salary.toLong().toString() },
                validateEdit = { _, text ->
                    val amount = text.toLongOrNull()
                    when {
                        amount == null -> "Enter a whole number"
                        amount < 0 -> "Salary cannot be negative"
                        else -> null
                    }
                },
            ),
            DataTableHeader(
                key = "email", title = "Email", value = { it.email }, width = 240.dp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            ),
        )
    }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text(
                "Click a cell, then ← → ↑ ↓ to move. Enter or F2 edits, or just start typing. " +
                    "Enter commits and drops a row; Tab commits and moves right; Esc cancels.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Focused: ${tableState.focusedCell?.columnKey ?: "none"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = { employees = sampleEmployees.take(12); log = emptyList() }) {
                Text("Reset")
            }
        }

        tableState.editError?.let { message ->
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    message,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        DataTable(
            items = employees,
            headers = headers,
            itemKey = { it.id },
            state = tableState,
            density = DataTableDensity.COMPACT,
            resizableColumns = true,
            reorderableColumns = true,
            onCellEdit = { edit ->
                log = (listOf("${edit.columnKey}: ${edit.oldText} → ${edit.newText}") + log).take(6)
                employees = employees.map { employee ->
                    if (employee.id != edit.rowKey) employee
                    else when (edit.columnKey) {
                        "role" -> employee.copy(role = edit.newText)
                        "department" -> employee.copy(department = edit.newText)
                        "salary" -> employee.copy(salary = edit.newText.toDouble())
                        else -> employee
                    }
                }
            },
            colors = DataTableDefaults.colors(rowAlternate = Color(0xFFF7F7F7)),
            modifier = Modifier.weight(1f),
        )

        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Reported edits", style = MaterialTheme.typography.labelMedium)
                if (log.isEmpty()) {
                    Text("None yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 12. Range selection and clipboard copy
// ---------------------------------------------------------------------------

@Composable
fun RangeSelectionSample() {
    val tableState = rememberDataTableState()
    var selectedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }
    var lastCopied by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        SampleControls {
            Text(
                "Click a cell, then Shift + arrows (or Shift+click) to extend the block. " +
                    "Ctrl+A selects everything, Ctrl+C copies. With no block, Ctrl+C falls back " +
                    "to the checked rows.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { tableState.clearRange(); lastCopied = null }) {
                Text("Clear")
            }
        }

        DataTable(
            items = sampleEmployees,
            headers = employeeHeaders,
            itemKey = { it.id },
            state = tableState,
            density = DataTableDensity.COMFORTABLE,
            cellNavigation = true,
            reorderableColumns = true,
            showSelect = true,
            selectedKeys = selectedKeys,
            onSelectionChange = { selectedKeys = it },
            // `onCopy` replaces the table's own clipboard write, so a handler that only
            // displays the payload would leave the clipboard empty. Do both: show it here, and
            // put it on the real clipboard. Drop `onCopy` entirely and the table does the
            // second half by itself.
            onCopy = { selection ->
                val text = selection.toTabSeparated(includeHeader = true)
                lastCopied = if (selection.copyToSystemClipboard(includeHeader = true)) {
                    text
                } else {
                    text + "\n\n(could not reach the system clipboard)"
                }
            },
            colors = DataTableDefaults.colors(rowAlternate = Color(0xFFF7F7F7)),
            modifier = Modifier.weight(1f),
        )

        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Clipboard payload" +
                        (tableState.selectedRange?.let { " — block anchored at ${it.anchor.columnKey}" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    lastCopied ?: "Nothing copied yet. Select some cells and press Ctrl+C — " +
                        "this panel shows what went to your clipboard, so paste it anywhere to check.",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
