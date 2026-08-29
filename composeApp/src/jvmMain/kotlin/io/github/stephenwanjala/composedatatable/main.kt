package io.github.stephenwanjala.composedatatable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.nucleusframework.application.DecoratedDialog
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import dev.nucleusframework.window.TitleBar
import dev.nucleusframework.window.macOSLargeCornerRadius
import dev.nucleusframework.window.styling.TitleBarColors
import dev.nucleusframework.window.styling.TitleBarMetrics
import dev.nucleusframework.window.styling.TitleBarStyle

/**
 * One entry in the gallery.
 *
 * @param exercises What this sample is here to prove out, shown under the title.
 */
private data class Sample(
    val title: String,
    val exercises: String,
    val icon: ImageVector,
    val content: @Composable () -> Unit,
)

private val samples = listOf(
    Sample(
        title = "Large Dataset",
        exercises = "500 rows, 25 columns · frozen columns · nested headers · multi-sort (Ctrl+click) · resizable columns · pagination · right-click",
        icon = Icons.Default.GridOn,
        content = { LargeDataSetSample() },
    ),
    Sample(
        title = "Nested Headers",
        exercises = "Three levels of grouped headers, with top-level leaves spanning the full header height",
        icon = Icons.Default.AccountTree,
        content = { NestedHeadersSample() },
    ),
    Sample(
        title = "Selection",
        exercises = "NONE / SINGLE / MULTI, select-all, key-based selection surviving a re-sort",
        icon = Icons.Default.CheckBox,
        content = { SelectionSample() },
    ),
    Sample(
        title = "Expansion",
        exercises = "Expandable rows with custom detail content, expand-all and collapse-all",
        icon = Icons.Default.UnfoldMore,
        content = { ExpansionSample() },
    ),
    Sample(
        title = "Grouping",
        exercises = "Rows grouped by department with custom group headers and per-group summary rows",
        icon = Icons.Default.Folder,
        content = { GroupingSample() },
    ),
    Sample(
        title = "Filtering",
        exercises = "A filter row per column · text filters, a dropdown filterContent, a numeric predicate, and hoisted filter state",
        icon = Icons.Default.FilterAlt,
        content = { FilteringSample() },
    ),
    Sample(
        title = "Saved Layout",
        exercises = "Hide, reorder and resize columns · captureLayout / applyLayout, and the encoded string an app would store per user",
        icon = Icons.Default.ViewColumn,
        content = { LayoutSample() },
    ),
    Sample(
        title = "Server-Side",
        exercises = "manualSorting + manualPagination + manualFiltering against a 5,000-row fake repository, holding one page in memory",
        icon = Icons.Default.CloudQueue,
        content = { ServerSideSample() },
    ),
    Sample(
        title = "Keyboard",
        exercises = "Arrow keys, Home/End, Enter, Space · focusRow() and focusedKey on DataTableState",
        icon = Icons.Default.Keyboard,
        content = { KeyboardSample() },
    ),
    Sample(
        title = "Cell Editing",
        exercises = "Cell focus and grid navigation · editable columns, validation, editValue, and a dropdown editorContent",
        icon = Icons.Default.EditNote,
        content = { CellEditingSample() },
    ),
    Sample(
        title = "Range & Copy",
        exercises = "Shift+arrows and Shift+click extend a cell block · Ctrl+A, Ctrl+C, and the ClipboardSelection payload",
        icon = Icons.Default.ContentCopy,
        content = { RangeSelectionSample() },
    ),
    Sample(
        title = "Density & Theme",
        exercises = "All three densities and a fully custom dark palette, with no Material theming in the library",
        icon = Icons.Default.Palette,
        content = { ThemingSample() },
    ),
    Sample(
        title = "Loading",
        exercises = "Built-in loading indicator and a custom loadingContent slot",
        icon = Icons.Default.HourglassEmpty,
        content = { LoadingSample() },
    ),
    Sample(
        title = "Empty",
        exercises = "Built-in empty state and a custom noDataContent slot",
        icon = Icons.AutoMirrored.Filled.List,
        content = { EmptyStateSample() },
    ),
)

@Composable
fun DataTableGallery() {
    var selectedIndex by remember { mutableStateOf(0) }
    val selected = samples[selectedIndex]

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    header = {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    },
                ) {
                    samples.forEachIndexed { index, sample ->
                        NavigationRailItem(
                            selected = index == selectedIndex,
                            onClick = { selectedIndex = index },
                            icon = { Icon(sample.icon, contentDescription = null) },
                            label = { Text(sample.title, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Text(
                                selected.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(selected.exercises, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Box(Modifier.weight(1f)) {
                        // Keyed so each sample gets its own state rather than inheriting the
                        // previous one's scroll position, selection, and page.
                        key(selectedIndex) { selected.content() }
                    }
                }
            }
        }
    }
}

fun main() =  nucleusApplication(backend = NucleusBackend.Tao) {
    val titleBarStyle = TitleBarStyle(
        colors = TitleBarColors(
            background = Color(0xFF5A5555),
            inactiveBackground = Color(0xFF808388),
            content = Color(0xFFF0EBEB),
            border = Color.Transparent,
        ),
        metrics = TitleBarMetrics(height = 36.dp),
    )

    NucleusDecoratedWindowTheme(isDark = true, titleBarStyle = titleBarStyle) {
        DecoratedWindow(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(size = DpSize(1024.dp, 720.dp)),
            minimumSize = DpSize(640.dp, 480.dp),
        ) {
            TitleBar(modifier = Modifier.macOSLargeCornerRadius()) { state ->
                Text("Compose DataTable — Gallery", Modifier.align(Alignment.CenterHorizontally))
            }
            DataTableGallery()
        }
    }
}
