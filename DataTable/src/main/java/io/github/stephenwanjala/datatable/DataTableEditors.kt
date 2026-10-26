package io.github.stephenwanjala.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

internal val LocalEditorColors = compositionLocalOf { DataTableColors() }
internal val LocalEditorTextStyle = compositionLocalOf { TextStyle.Default }

/**
 * Ready-made editors for [DataTableHeader.editorContent] and the matching cell renderers, for the
 * columns that do not want a free-text field.
 *
 * ```kotlin
 * DataTableHeader<Employee>(
 *     key = "department",
 *     value = { it.department },
 *     editable = true,
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
     * The list opens under the cell with the current value highlighted. Up and Down move the
     * highlight, typing a letter jumps to the next option starting with it, Enter or a click
     * commits, and Escape or a click outside cancels.
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
private fun DropdownEditor(
    options: List<String>,
    label: (String) -> String,
    controller: CellEditController,
) {
    val colors = LocalEditorColors.current
    val textStyle = LocalEditorTextStyle.current
    val focusRequester = remember { FocusRequester() }
    var highlighted by remember {
        mutableIntStateOf(options.indexOf(controller.currentText).coerceAtLeast(0))
    }

    fun jumpTo(char: Char) {
        val start = highlighted + 1
        val ordered = options.indices.map { (start + it) % options.size }
        ordered.firstOrNull { options[it].startsWith(char, ignoreCase = true) }
            ?.let { highlighted = it }
    }

    Box(Modifier.fillMaxWidth().background(colors.editingCell)) {
        BasicText(label(controller.currentText), style = textStyle)
        Popup(
            alignment = Alignment.BottomStart,
            onDismissRequest = controller::cancel,
            properties = PopupProperties(focusable = true),
        ) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Column(
                Modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(min = 120.dp)
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
                    .background(colors.container)
                    .border(1.dp, colors.divider)
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
                            Key.Enter, Key.NumPadEnter -> controller.commit(options[highlighted])
                            Key.Escape -> controller.cancel()
                            else -> {
                                val char = event.utf16CodePoint.toChar()
                                if (!char.isLetterOrDigit()) return@onPreviewKeyEvent false
                                jumpTo(char)
                            }
                        }
                        true
                    },
            ) {
                options.forEachIndexed { index, option ->
                    BasicText(
                        text = label(option),
                        style = textStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (index == highlighted) colors.hoveredRow
                                else colors.container,
                            )
                            .clickable { controller.commit(option) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
