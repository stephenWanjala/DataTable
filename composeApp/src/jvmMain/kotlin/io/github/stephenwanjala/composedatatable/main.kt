package io.github.stephenwanjala.composedatatable


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.stephenwanjala.datatable.*
import java.util.*
import kotlin.random.Random

data class LargeDataSetItem(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val email: String,
    val city: String,
    val country: String,
    val occupation: String,
    val salary: Double,
    val startDate: String,
    val projectStatus: String,
    val hoursWorked: Int,
    val department: String,
    val notes: String,
    val isActive: Boolean,
    val rating: Double,
    val version: String,
    val licenseKey: String,
    val lastLogin: String,
    val ipAddress: String,
    val macAddress: String,
    val phoneNumber: String,
    val zipCode: String,
    val streetAddress: String,
    val buildingNumber: String,
    val floorNumber: Int,
    val officeNumber: Int,
    val managerName: String,
    val teamLead: String,
    val reviewScore: Int,
) {
    val fullName: String
        get() = "$firstName $lastName"

    companion object {
        private val firstNames =
            listOf("Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Heidi", "Ivan", "Judy")
        private val lastNames = listOf(
            "Smith", "Johnson", "Williams", "Brown", "Jones",
            "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"
        )
        private val cities = listOf(
            "New York", "Los Angeles", "Chicago", "Houston",
            "Phoenix", "Philadelphia", "San Antonio", "San Diego"
        )
        private val countries =
            listOf("USA", "Canada", "Mexico", "Brazil", "UK", "Germany", "France", "Spain", "Italy", "Japan")
        private val occupations = listOf(
            "Engineer", "Doctor", "Teacher", "Artist",
            "Programmer", "Analyst", "Manager", "Consultant", "Designer"
        )
        private val projectStatuses = listOf("Completed", "In Progress", "Pending", "On Hold", "Cancelled")
        private val departments =
            listOf("IT", "HR", "Finance", "Marketing", "Sales", "Operations", "Research", "Development")

        fun generateRandom(id: Int, random: Random): LargeDataSetItem {
            val firstName = firstNames.random(random)
            val lastName = lastNames.random(random)
            val age = random.nextInt(22, 65)
            val email = "${firstName.lowercase(Locale.getDefault())}.${lastName.lowercase(Locale.getDefault())}@example.com"
            val city = cities.random(random)
            val country = countries.random(random)
            val occupation = occupations.random(random)
            val salary = random.nextDouble(40000.0, 120000.0).round(2)
            val startDate = "20${random.nextInt(10, 24)}-${random.nextInt(1, 12).toString().padStart(2, '0')}-${
                random.nextInt(1, 28).toString().padStart(2, '0')
            }"
            val projectStatus = projectStatuses.random(random)
            val hoursWorked = random.nextInt(100, 2000)
            val department = departments.random(random)
            val notes =
                "This is a random note for ${firstName} ${lastName} generated for testing purposes. It can be quite long."
            val isActive = random.nextBoolean()
            val rating = random.nextDouble(1.0, 5.0).round(1)
            val version = "${random.nextInt(1, 5)}.${random.nextInt(0, 10)}.${random.nextInt(0, 10)}"
            val licenseKey =
                (1..5).map { (0..9).random(random) }.joinToString("") + "-" + (1..5).map { (0..9).random(random) }
                    .joinToString("")
            val lastLogin = "2024-03-${random.nextInt(1, 28).toString().padStart(2, '0')} ${
                random.nextInt(0, 23).toString().padStart(2, '0')
            }:${random.nextInt(0, 59).toString().padStart(2, '0')}"
            val ipAddress = "${random.nextInt(0, 255)}.${random.nextInt(0, 255)}.${random.nextInt(0, 255)}.${
                random.nextInt(0, 255)
            }"
            val macAddress = (1..6).joinToString(":") { String.format("%02X", random.nextInt(256)) }
            val phoneNumber = "+1-${random.nextInt(200, 999)}-${random.nextInt(100, 999)}-${random.nextInt(1000, 9999)}"
            val zipCode = "${random.nextInt(10000, 99999)}"
            val streetAddress = "${
                random.nextInt(100, 999)
            } ${firstNames.random(random)} ${if (random.nextBoolean()) "Street" else "Avenue"}"
            val buildingNumber = "${random.nextInt(1, 20)}"
            val floorNumber = random.nextInt(1, 30)
            val officeNumber = random.nextInt(101, 500)
            val managerName = firstNames.random(random) + " " + lastNames.random(random)
            val teamLead = firstNames.random(random) + " " + lastNames.random(random)
            val reviewScore = random.nextInt(1, 10)

            return LargeDataSetItem(
                id, firstName, lastName, age, email, city, country, occupation, salary, startDate,
                projectStatus, hoursWorked, department, notes, isActive, rating, version, licenseKey,
                lastLogin, ipAddress, macAddress, phoneNumber, zipCode, streetAddress, buildingNumber,
                floorNumber, officeNumber, managerName, teamLead, reviewScore
            )
        }

        fun Double.round(decimals: Int): Double {
            var multiplier = 1.0
            repeat(decimals) { multiplier *= 10 }
            return (this * multiplier).toLong() / multiplier
        }
    }
}

/**
 * Showcases: frozen columns, multi-sort (Ctrl+click), resizable columns,
 * alternating row colors, items-per-page selector, text overflow/ellipsis,
 * custom sort comparator, row hover, right-click, keyboard navigation, column visibility.
 */
@Composable
fun LargeDataSetExample() {
    val random = remember { Random(42) }
    val items = remember {
        (1..500).map { id -> LargeDataSetItem.generateRandom(id, random) }
    }

    var selectedItems by remember { mutableStateOf<Set<LargeDataSetItem>>(emptySet()) }
    var currentPage by remember { mutableStateOf(0) }
    var itemsPerPage by remember { mutableStateOf(20) }
    var multiSort by remember { mutableStateOf<List<SortState>>(emptyList()) }
    var showNotesColumn by remember { mutableStateOf(true) }

    val tableState = rememberDataTableState()

    val headers = remember(showNotesColumn) {
        listOf(
            DataTableHeader<LargeDataSetItem>(
                key = "id",
                title = "ID",
                value = { it.id },
                width = 60.dp,
                align = TextAlign.Center,
                fixed = true, // Frozen column
            ),
            DataTableHeader(
                key = "fullName",
                title = "Full Name",
                value = { it.fullName },
                width = 150.dp,
                fixed = true, // Frozen column
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            ),
            DataTableHeader(
                key = "age",
                title = "Age",
                value = { it.age },
                width = 60.dp,
                align = TextAlign.End,
            ),
            DataTableHeader(
                key = "email",
                title = "Email",
                value = { it.email },
                width = 250.dp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            ),
            DataTableHeader(
                key = "city",
                title = "City",
                value = { it.city },
                width = 120.dp,
            ),
            DataTableHeader(
                key = "country",
                title = "Country",
                value = { it.country },
                width = 100.dp,
            ),
            DataTableHeader(
                key = "occupation",
                title = "Occupation",
                value = { it.occupation },
                width = 120.dp,
            ),
            DataTableHeader(
                key = "salary",
                title = "Salary",
                value = { "$${String.format("%.2f", it.salary)}" },
                width = 120.dp,
                align = TextAlign.End,
                // Custom comparator: sort by numeric salary, not string
                comparator = compareBy { it.salary },
            ),
            DataTableHeader(
                key = "department",
                title = "Department",
                value = { it.department },
                width = 120.dp,
            ),
            DataTableHeader(
                key = "startDate",
                title = "Start Date",
                value = { it.startDate },
                width = 120.dp,
            ),
            DataTableHeader(
                key = "projectStatus",
                title = "Project Status",
                value = { it.projectStatus },
                width = 150.dp,
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
                            labelColor = color
                        )
                    )
                }
            ),
            DataTableHeader(
                key = "hoursWorked",
                title = "Hours",
                value = { it.hoursWorked },
                width = 80.dp,
                align = TextAlign.End,
            ),
            DataTableHeader(
                key = "isActive",
                title = "Active",
                value = { it.isActive },
                width = 80.dp,
                align = TextAlign.Center,
                cellContent = { item ->
                    Icon(
                        imageVector = if (item.isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = if (item.isActive) "Active" else "Inactive",
                        tint = if (item.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            ),
            DataTableHeader(
                key = "rating",
                title = "Rating",
                value = { it.rating },
                width = 80.dp,
                align = TextAlign.End,
            ),
            DataTableHeader(
                key = "version",
                title = "Version",
                value = { it.version },
                width = 80.dp,
            ),
            DataTableHeader(
                key = "licenseKey",
                title = "License Key",
                value = { it.licenseKey },
                width = 150.dp,
            ),
            DataTableHeader(
                key = "lastLogin",
                title = "Last Login",
                value = { it.lastLogin },
                width = 150.dp,
            ),
            DataTableHeader(
                key = "ipAddress",
                title = "IP Address",
                value = { it.ipAddress },
                width = 130.dp,
            ),
            DataTableHeader(
                key = "macAddress",
                title = "MAC Address",
                value = { it.macAddress },
                width = 150.dp,
            ),
            DataTableHeader(
                key = "phoneNumber",
                title = "Phone",
                value = { it.phoneNumber },
                width = 150.dp,
            ),
            DataTableHeader(
                key = "zipCode",
                title = "ZIP",
                value = { it.zipCode },
                width = 80.dp,
            ),
            DataTableHeader(
                key = "streetAddress",
                title = "Address",
                value = { it.streetAddress },
                width = 200.dp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            ),
            DataTableHeader(
                key = "buildingNumber",
                title = "Building",
                value = { it.buildingNumber },
                width = 80.dp,
            ),
            DataTableHeader(
                key = "floorNumber",
                title = "Floor",
                value = { it.floorNumber },
                width = 60.dp,
                align = TextAlign.End,
            ),
            DataTableHeader(
                key = "officeNumber",
                title = "Office",
                value = { it.officeNumber },
                width = 80.dp,
                align = TextAlign.End,
            ),
            DataTableHeader(
                key = "managerName",
                title = "Manager",
                value = { it.managerName },
                width = 150.dp,
            ),
            DataTableHeader(
                key = "teamLead",
                title = "Team Lead",
                value = { it.teamLead },
                width = 150.dp,
            ),
            DataTableHeader(
                key = "reviewScore",
                title = "Review",
                value = { it.reviewScore },
                width = 80.dp,
                align = TextAlign.End,
            ),
            DataTableHeader(
                key = "notes",
                title = "Notes",
                value = { it.notes },
                width = 300.dp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                visible = showNotesColumn, // Togglable visibility
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Large Dataset Example",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${items.size} total | ${selectedItems.size} selected | Ctrl+click headers to multi-sort",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Toggle notes column visibility
                    Button(onClick = { showNotesColumn = !showNotesColumn }) {
                        Text(if (showNotesColumn) "Hide Notes" else "Show Notes")
                    }

                    if (selectedItems.isNotEmpty()) {
                        Button(
                            onClick = { selectedItems = emptySet() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Clear Selection")
                        }
                    }
                }
            }
        }

        DataTable(
            items = items,
            headers = headers,
            state = tableState,
            itemKey = { it.id },
            showSelect = true,
            selectionMode = SelectionMode.MULTI,
            selectedItems = selectedItems,
            onSelectionChange = { selectedItems = it },
            // Multi-sort
            multiSortBy = multiSort,
            onMultiSortChange = { multiSort = it },
            // Resizable columns
            resizableColumns = true,
            minColumnWidth = 50.dp,
            // Pagination with items-per-page selector
            showPagination = true,
            itemsPerPage = itemsPerPage,
            currentPage = currentPage,
            onPageChange = { currentPage = it },
            onItemsPerPageChange = {
                itemsPerPage = it
                currentPage = 0
            },
            // Alternating row colors
            colors = DataTableDefaults.colors(
                rowAlternate = Color(0xFFF5F5F5),
            ),
            density = DataTableDensity.DEFAULT,
            onRowClick = { item ->
                println("Clicked: ${item.fullName}")
            },
            onRowDoubleClick = { item ->
                println("Double-clicked: ${item.fullName}")
            },
            // Right-click context menu
            onRowContextMenu = { item, offset ->
                println("Right-clicked: ${item.fullName} at $offset")
            },
            modifier = Modifier.weight(1f),
            showScrollbars = true,
        )
    }
}

@Composable
fun LoadingStateExample() {
    var isLoading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<LargeDataSetItem>>(emptyList()) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            kotlinx.coroutines.delay(2000)
            val random = Random(42)
            items = (1..50).map { id ->
                LargeDataSetItem.generateRandom(id, random)
            }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Loading State Example",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        isLoading = true
                        items = emptyList()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reload")
                }
            }
        }

        val headers = remember {
            listOf(
                DataTableHeader<LargeDataSetItem>(
                    key = "id",
                    title = "ID",
                    value = { it.id },
                    width = 60.dp
                ),
                DataTableHeader(
                    key = "fullName",
                    title = "Full Name",
                    value = { it.fullName },
                    width = 150.dp
                ),
                DataTableHeader(
                    key = "email",
                    title = "Email",
                    value = { it.email },
                    width = 250.dp
                ),
                DataTableHeader(
                    key = "department",
                    title = "Department",
                    value = { it.department },
                    width = 120.dp
                )
            )
        }

        DataTable(
            items = items,
            headers = headers,
            itemKey = { it.id },
            loading = isLoading,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun EmptyStateExample() {
    val items = remember { emptyList<LargeDataSetItem>() }

    val headers = remember {
        listOf(
            DataTableHeader<LargeDataSetItem>(
                key = "id",
                title = "ID",
                value = { it.id },
                width = 60.dp
            ),
            DataTableHeader(
                key = "fullName",
                title = "Full Name",
                value = { it.fullName },
                width = 150.dp
            ),
            DataTableHeader(
                key = "email",
                title = "Email",
                value = { it.email },
                width = 250.dp
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Empty State Example",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DataTable(
            items = items,
            headers = headers,
            itemKey = { it.id },
            noDataContent = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            "No data to display",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "Add some items to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Button(onClick = { }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Item")
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "composedatatable",
    ) {
        LargeDataSetExample()
    }
}
