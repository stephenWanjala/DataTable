package io.github.stephenwanjala.datatable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

internal val LocalEditorColors = compositionLocalOf { DataTableColors() }
internal val LocalEditorTextStyle = compositionLocalOf { TextStyle.Default }

private const val TYPE_AHEAD_RESET_MILLIS = 800L

/**
 * Ready-made editors for [DataTableHeader.editorContent] and the matching cell renderers, for the
 * columns that do not want a free-text field.
 *
 * ```kotlin
 * DataTableHeader<Employee>(
 *     key = "department",
 *     value = { it.department },
 *     editable = true,
 *     cellContent = DataTableEditors.dropdownCell { it.department },
 *     editorContent = DataTableEditors.dropdown(listOf("Finance", "IT", "Sales")),
 * )
 *
 * DataTableHeader<Employee>(
 *     key = "active",
 *     value = { it.active },
 *     editable = true,
 *     editOnClick = true,
 *     cellContent = DataTableEditors.checkboxCell { it.active },
 *     editorContent = DataTableEditors.checkbox(),
 * )
 * ```
 *
 * Both editors commit through the column's `validateEdit` and report through `onCellEdit` like
 * the built-in text field, so the reported text is the chosen option, or `"true"` / `"false"`.
 */
object DataTableEditors {

    /**
     * A dropdown editor offering a fixed set of [options].
     *
     * The list opens under the cell, at least as wide as the cell, with the current value ticked
     * and highlighted. Up, Down, Home, and End move the highlight, as does hovering. Typing jumps
     * to the first option starting with what was typed, and typing quickly builds a longer
     * prefix. Enter or a click commits, and Escape or a click outside cancels.
     *
     * @param options The values a cell may take, which are also the text committed.
     * @param label Turns an option into the text shown for it in the list and in the cell.
     */
    fun <T> dropdown(
        options: List<String>,
        label: (String) -> String = { it },
    ): @Composable (T, CellEditController) -> Unit = { _, controller ->
        DropdownEditor(options, label, controller)
    }

    /**
     * Cell content for a dropdown column: the value followed by a chevron, so the column reads
     * as a picker before it is opened.
     *
     * @param text The text shown for a row, typically the same label given to [dropdown].
     */
    fun <T> dropdownCell(text: (T) -> String): @Composable (T) -> Unit = { item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = text(item),
                style = LocalEditorTextStyle.current,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            Chevron(LocalEditorColors.current.iconTint)
        }
    }

    /**
     * A boolean editor that flips the cell as soon as it opens, so Enter, F2, or a double-click
     * toggles the value in one step. Pair it with [checkboxCell] so the cell shows the state, and
     * with [DataTableHeader.editOnClick] to toggle on a single click.
     */
    fun <T> checkbox(): @Composable (T, CellEditController) -> Unit = { _, controller ->
        LaunchedEffect(Unit) {
            controller.commit((!controller.currentText.toBoolean()).toString())
        }
    }

    /**
     * Cell content drawing a read-only checkbox for a boolean column.
     *
     * @param isChecked Whether the checkbox is ticked for a row.
     */
    fun <T> checkboxCell(isChecked: (T) -> Boolean): @Composable (T) -> Unit = { item ->
        val checked = isChecked(item)
        SimpleCheckbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.semantics {
                contentDescription = if (checked) "Checked" else "Unchecked"
            },
            colors = LocalEditorColors.current,
            focusable = false,
        )
    }
}

@Composable
private fun Chevron(color: Color) {
    Canvas(Modifier.size(10.dp, 6.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width / 2, size.height)
            lineTo(size.width, 0f)
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun Tick(color: Color) {
    Canvas(Modifier.size(12.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.55f)
            lineTo(size.width * 0.4f, size.height * 0.8f)
            lineTo(size.width * 0.85f, size.height * 0.25f)
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

private data class TypedPrefix(val text: String = "", val at: Long = 0L)

@Composable
private fun DropdownEditor(
    options: List<String>,
    label: (String) -> String,
    controller: CellEditController,
) {
    val colors = LocalEditorColors.current
    val textStyle = LocalEditorTextStyle.current
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val requesters = remember(options) { options.map { BringIntoViewRequester() } }
    val current = options.indexOf(controller.currentText)
    var highlighted by remember { mutableIntStateOf(current.coerceAtLeast(0)) }
    var cellWidth by remember { mutableIntStateOf(0) }
    var typed by remember { mutableStateOf(TypedPrefix()) }

    LaunchedEffect(highlighted) { requesters.getOrNull(highlighted)?.bringIntoView() }

    fun typeAhead(char: Char) {
        val now = System.currentTimeMillis()
        val extending = now - typed.at <= TYPE_AHEAD_RESET_MILLIS
        val prefix = if (extending) typed.text + char else char.toString()
        typed = TypedPrefix(prefix, now)
        val start = if (extending) highlighted else highlighted + 1
        options.indices.map { (start + it) % options.size }
            .firstOrNull { options[it].startsWith(prefix, ignoreCase = true) }
            ?.let { highlighted = it }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.editingCell)
            .onSizeChanged { cellWidth = it.width },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label(controller.currentText),
            style = textStyle,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp))
        Chevron(colors.iconTint)
        Popup(
            alignment = Alignment.BottomStart,
            onDismissRequest = controller::cancel,
            properties = PopupProperties(focusable = true),
        ) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Column(
                Modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(min = with(density) { cellWidth.toDp() }.coerceAtLeast(120.dp))
                    .heightIn(max = 240.dp)
                    .background(colors.container)
                    .border(1.dp, colors.divider)
                    .verticalScroll(rememberScrollState())
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || options.isEmpty()) {
                            return@onPreviewKeyEvent false
                        }
                        when (event.key) {
                            Key.DirectionDown -> highlighted = (highlighted + 1) % options.size
                            Key.DirectionUp ->
                                highlighted = (highlighted - 1 + options.size) % options.size
                            Key.MoveHome -> highlighted = 0
                            Key.MoveEnd -> highlighted = options.lastIndex
                            Key.Enter, Key.NumPadEnter -> controller.commit(options[highlighted])
                            Key.Escape -> controller.cancel()
                            else -> {
                                val char = event.utf16CodePoint.toChar()
                                if (!char.isLetterOrDigit()) return@onPreviewKeyEvent false
                                typeAhead(char)
                            }
                        }
                        true
                    },
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(requesters[index])
                            .background(
                                if (index == highlighted) colors.hoveredRow else colors.container,
                            )
                            .pointerInput(index) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Move) highlighted = index
                                    }
                                }
                            }
                            .clickable { controller.commit(option) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(12.dp)) {
                            if (index == current) Tick(colors.checkboxChecked)
                        }
                        Spacer(Modifier.width(8.dp))
                        BasicText(text = label(option), style = textStyle)
                    }
                }
            }
        }
    }
}
